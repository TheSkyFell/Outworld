package io.github.theskyfell.outworld.core.machine;

import io.github.theskyfell.outworld.core.CoreContent;
import io.github.theskyfell.outworld.core.fluid.FluidAmount;
import io.github.theskyfell.outworld.core.fluid.FluidTank;
import io.github.theskyfell.outworld.core.fluid.FluidUnits;
import io.github.theskyfell.outworld.core.gas.Gas;
import io.github.theskyfell.outworld.core.gas.GasStack;
import io.github.theskyfell.outworld.core.gas.GasTank;
import io.github.theskyfell.outworld.core.menu.Menus;
import io.github.theskyfell.outworld.core.network.Network;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * The menu behind every recipe machine's screen: its input and output slots, the player's inventory,
 * and the energy, progress and tank readings ({@link MachineValues}), which the server sends only to a
 * player with the screen open and only when they change.
 */
public final class MachineMenu extends AbstractContainerMenu {
    /** Where the first input slot and the first output slot go; slots run to the right. */
    public static final int INPUT_X = 26;
    public static final int OUTPUT_X = 116;
    public static final int SLOT_Y = 35;

    /** What the client needs to build the menu: where the machine is and how many slots it has. */
    public record OpenData(BlockPos pos, int itemInputs, int itemOutputs) {
        public static final StreamCodec<ByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenData::pos,
                ByteBufCodecs.VAR_INT, OpenData::itemInputs,
                ByteBufCodecs.VAR_INT, OpenData::itemOutputs,
                OpenData::new);

        static OpenData of(RecipeMachineBlockEntity machine) {
            return new OpenData(machine.getBlockPos(), machine.layout().itemInputs(), machine.layout().itemOutputs());
        }
    }

    private final BlockPos pos;
    private final int machineSlots;
    private final RecipeMachineBlockEntity machine;
    private final Player player;
    private MachineValues values = MachineValues.EMPTY;

    /** On the client, from the data the server sent. */
    public MachineMenu(int containerId, Inventory inventory, OpenData data) {
        this(containerId, inventory, data, new SimpleContainer(data.itemInputs() + data.itemOutputs()), null);
    }

    private MachineMenu(int containerId, Inventory inventory, OpenData data, Container container, RecipeMachineBlockEntity machine) {
        super(CoreContent.MACHINE_MENU.get(), containerId);
        this.pos = data.pos();
        this.machineSlots = data.itemInputs() + data.itemOutputs();
        this.machine = machine;
        this.player = inventory.player;
        for (int i = 0; i < data.itemInputs(); i++) {
            addSlot(new InputSlot(container, i, INPUT_X + i * 18, SLOT_Y));
        }
        for (int i = 0; i < data.itemOutputs(); i++) {
            addSlot(new OutputSlot(container, data.itemInputs() + i, OUTPUT_X + i * 18, SLOT_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    /** Opens a machine's screen for a player. */
    public static void open(ServerPlayer player, RecipeMachineBlockEntity machine, Component title) {
        OpenData data = OpenData.of(machine);
        Menus.open(player, new SimpleMenuProvider(
                (containerId, inventory, opener) -> new MachineMenu(containerId, inventory, data, machine.items(), machine), title),
                OpenData.STREAM_CODEC, data);
    }

    public BlockPos pos() {
        return pos;
    }

    /** The latest readings from the server (on the client), or nothing yet. */
    public MachineValues values() {
        return values;
    }

    /** Called on the client when new readings arrive. */
    public void setValues(MachineValues values) {
        this.values = values;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (machine != null && player instanceof ServerPlayer serverPlayer) {
            MachineValues current = read(machine);
            if (!current.equals(values)) {
                values = current;
                Network.sendToPlayer(serverPlayer, current);
            }
        }
    }

    private MachineValues read(RecipeMachineBlockEntity machine) {
        List<MachineValues.Tank> tanks = new ArrayList<>();
        for (FluidTank tank : machine.fluidInputs()) {
            tanks.add(fluidTank(tank));
        }
        for (FluidTank tank : machine.fluidOutputs()) {
            tanks.add(fluidTank(tank));
        }
        for (GasTank tank : machine.gasInputs()) {
            tanks.add(gasTank(machine, tank));
        }
        for (GasTank tank : machine.gasOutputs()) {
            tanks.add(gasTank(machine, tank));
        }
        return new MachineValues(containerId, machine.energy().getAmount(), machine.energy().getCapacity(),
                machine.progress(), machine.recipeTime(), List.copyOf(tanks));
    }

    private static MachineValues.Tank fluidTank(FluidTank tank) {
        FluidAmount contents = tank.getContents();
        String id = contents.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(contents.fluid()).toString();
        int color = contents.isEmpty() ? 0 : contents.fluid().isSame(Fluids.LAVA) ? 0xff6a00 : 0x3f76e4;
        return new MachineValues.Tank(false, id, contents.amount() / FluidUnits.MILLIBUCKET,
                tank.getCapacity() / FluidUnits.MILLIBUCKET, color);
    }

    private static MachineValues.Tank gasTank(RecipeMachineBlockEntity machine, GasTank tank) {
        GasStack contents = tank.getContents();
        if (contents.isEmpty()) {
            return new MachineValues.Tank(true, "", 0, tank.getCapacity(), 0);
        }
        Gas gas = machine.getLevel() == null ? null
                : machine.getLevel().registryAccess().lookupOrThrow(Gas.REGISTRY).getValue(contents.gas());
        return new MachineValues.Tank(true, contents.gas().identifier().toString(), contents.amount(), tank.getCapacity(),
                gas == null ? 0xcccccc : gas.color());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots) {
            // From the machine into the player's inventory.
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
            // Into the machine's inputs (output slots refuse it).
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (machine == null) {
            return true; // the client trusts the server, which closes the screen
        }
        return !machine.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64;
    }

    /** An input slot: takes only what the machine accepts there. */
    private static final class InputSlot extends Slot {
        InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    /** An output slot: the player can take from it but not put things in. */
    private static final class OutputSlot extends Slot {
        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
