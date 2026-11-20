package io.github.theskyfell.outworld.industry.logistics;

import io.github.theskyfell.outworld.core.energy.Energy;
import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import io.github.theskyfell.outworld.core.item.ItemAccess;
import io.github.theskyfell.outworld.core.item.ItemTransfer;
import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * An inserter's arm: takes one item from the block behind and swings it to the block in front, one
 * item a second ({@link #SWING_TICKS} out, the same back).
 *
 * <ul>
 *   <li>It only picks up an item the block in front will take, so it never stands holding something
 *       nothing wants. From a belt it takes the item furthest along either lane.
 *   <li>Onto a belt it puts items on the far lane (seen from the inserter), half way along; into
 *       anything else with an inventory through the loader's item API.
 *   <li>Each swing costs {@link #ENERGY_PER_SWING} FE, drawn from cables or batteries next to it.
 *   <li>With nothing to move it looks again every half second (counted as waiting, not awake).
 * </ul>
 */
public final class InserterBlockEntity extends MachineBlockEntity {
    public static final int SWING_TICKS = 10;
    public static final long ENERGY_PER_SWING = 40;
    private static final int RETRY_TICKS = 10;

    private final EnergyBuffer energy = new EnergyBuffer(4_000, 4_000, 0);
    private ItemStack held = ItemStack.EMPTY;
    /** Game time the arm last started swinging out (with {@link #held}) or back (empty). */
    private long swingStart = Long.MIN_VALUE / 2;

    public InserterBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.INSERTER.get(), pos, state);
        energy.onChange(this::wake);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public Direction facing() {
        return getBlockState().getValue(InserterBlock.FACING);
    }

    /** What the arm holds; empty while it swings back or waits. */
    public ItemStack held() {
        return held;
    }

    /** Client side: how far the arm is through its swing, 0 to 1. */
    public float swingProgress(float partialTick) {
        long now = level == null ? 0 : level.getGameTime();
        return Math.clamp((now - swingStart + partialTick) / SWING_TICKS, 0F, 1F);
    }

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        long now = level.getGameTime();
        if (now - swingStart < SWING_TICKS) {
            return true; // still swinging out or back
        }
        Direction facing = facing();
        BlockPos target = worldPosition.relative(facing);
        if (!held.isEmpty()) {
            if (!drop(level, target, facing, held, false)) {
                retryIn(RETRY_TICKS); // the target is full: hold on to it
                return false;
            }
            held = ItemStack.EMPTY;
            swingStart = now;
            changed(level);
            return true;
        }
        if (energy.getAmount() < ENERGY_PER_SWING) {
            Energy.pullFromNeighbours(level, worldPosition, energy, energy.getCapacity() - energy.getAmount());
            if (energy.getAmount() < ENERGY_PER_SWING) {
                retryIn(RETRY_TICKS * 2);
                return false;
            }
        }
        ItemStack picked = pick(level, worldPosition.relative(facing.getOpposite()), facing,
                stack -> drop(level, target, facing, stack, true));
        if (picked.isEmpty()) {
            retryIn(RETRY_TICKS);
            return false;
        }
        energy.setAmount(energy.getAmount() - ENERGY_PER_SWING);
        held = picked;
        swingStart = now;
        changed(level);
        return true;
    }

    /** Takes one item the target wants from the block behind. */
    private static ItemStack pick(ServerLevel level, BlockPos source, Direction facing, Predicate<ItemStack> wanted) {
        if (!level.isLoaded(source)) {
            return ItemStack.EMPTY;
        }
        if (level.getBlockEntity(source) instanceof BeltBlockEntity belt) {
            int best = -1;
            for (int lane = 0; lane < 2; lane++) {
                BeltLane items = belt.lane(lane);
                if (!items.isEmpty() && wanted.test(items.item(0))
                        && (best < 0 || items.position(0) > belt.lane(best).position(0))) {
                    best = lane;
                }
            }
            return best < 0 ? ItemStack.EMPTY : belt.takeFront(best);
        }
        ItemAccess access = ItemTransfer.find(level, source, facing);
        return access == null ? ItemStack.EMPTY : access.extract(wanted, 1, false);
    }

    /** Puts {@code item} into the block in front, or only checks that it would fit. */
    private boolean drop(ServerLevel level, BlockPos target, Direction facing, ItemStack item, boolean simulate) {
        if (!level.isLoaded(target)) {
            return false;
        }
        if (level.getBlockEntity(target) instanceof BeltBlockEntity belt) {
            int lane = farLane(belt.facing(), facing.getOpposite());
            for (int position : new int[] {BeltLane.LENGTH / 2, BeltLane.LENGTH / 4, BeltLane.LENGTH * 3 / 4}) {
                if (belt.lane(lane).canInsert(position)) {
                    return simulate || belt.receive(lane, item, position);
                }
            }
            return false;
        }
        ItemAccess access = ItemTransfer.find(level, target, facing.getOpposite());
        return access != null && access.insert(item.copyWithCount(1), simulate) == 1;
    }

    /** The lane of a belt facing {@code beltFacing} furthest from a block on its {@code side}. */
    static int farLane(Direction beltFacing, Direction side) {
        if (side == beltFacing.getCounterClockWise()) {
            return BeltBlockEntity.RIGHT;
        }
        if (side == beltFacing.getClockWise()) {
            return BeltBlockEntity.LEFT;
        }
        return BeltBlockEntity.LEFT;
    }

    private void changed(ServerLevel level) {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.save(output, "energy");
        if (!held.isEmpty()) {
            output.store("held", ItemStack.CODEC, held);
        }
        output.putLong("swing_start", swingStart);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.load(input, "energy");
        held = input.read("held", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        swingStart = input.getLongOr("swing_start", Long.MIN_VALUE / 2);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !held.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, held);
            held = ItemStack.EMPTY;
        }
    }
}
