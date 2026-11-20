package io.github.theskyfell.outworld.industry.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.theskyfell.outworld.core.item.ItemAccess;
import io.github.theskyfell.outworld.core.item.ItemBuffer;
import io.github.theskyfell.outworld.core.item.ItemTransfer;
import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A conveyor belt's contents: two {@link BeltLane}s, left and right as seen looking the way items
 * move, and two entry slots where hoppers, pipes and other mods drop items for the belt to take on.
 *
 * <ul>
 *   <li>Items move {@link #SPEED} per tick (1.5 blocks a second; 7.5 items a second per lane). The
 *       belt works in the machine scheduler's batches and sleeps when nothing on it can move.
 *   <li>An item reaching the end goes onto the belt in front: the same lane if that belt runs the same
 *       way or is a curve fed by this one; the near lane, half way along, if this belt feeds into its
 *       side. If the block in front is anything else with an inventory, the item goes into it.
 *       Otherwise the lane stops and items queue behind each other.
 *   <li>When anything on a belt moves, the belts feeding it are woken in case there is room now. A
 *       belt stuck behind a full inventory checks again every second.
 *   <li>Clients are sent the lanes only when items come or go; between updates they move the items
 *       themselves with the same rules, so a busy belt costs one update per item, not one per tick.
 * </ul>
 */
public final class BeltBlockEntity extends MachineBlockEntity {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    /** How far items move per tick, in {@link BeltLane} units (1/256 of a block). */
    public static final int SPEED = 24;
    private static final int BLOCKED_RETRY_TICKS = 20;
    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("lane").forGetter(Entry::lane),
            Codec.INT.fieldOf("position").forGetter(Entry::position),
            ItemStack.CODEC.fieldOf("item").forGetter(Entry::item)
    ).apply(instance, Entry::new));

    private final BeltLane[] lanes = {new BeltLane(), new BeltLane()};
    private final ItemBuffer entry = new EntrySlots();
    /** Items came or went since the client was last told. */
    private boolean clientOutdated;
    /** Client side: the game time the lanes were last received, or MIN_VALUE until first drawn. */
    private long syncTime = Long.MIN_VALUE;

    public BeltBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.BELT.get(), pos, state);
        entry.onChange(() -> {
            setChanged();
            wake();
        });
    }

    public Direction facing() {
        return getBlockState().getValue(BeltBlock.FACING);
    }

    /** {@link #LEFT} or {@link #RIGHT}. */
    public BeltLane lane(int index) {
        return lanes[index];
    }

    /** Where other blocks put items for the belt: slot 0 for the left lane, 1 for the right. */
    public ItemBuffer entry() {
        return entry;
    }

    /**
     * Puts an item onto a lane at {@code position}, from another belt or an inserter.
     *
     * @return false if it doesn't fit there
     */
    public boolean receive(int lane, ItemStack item, int position) {
        if (!lanes[lane].insert(item, position)) {
            return false;
        }
        itemsChanged();
        return true;
    }

    /**
     * Takes the item furthest along a lane, for an inserter.
     *
     * @return the item, or empty if the lane is empty
     */
    public ItemStack takeFront(int lane) {
        if (lanes[lane].isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack item = lanes[lane].remove(0);
        itemsChanged();
        return item;
    }

    /** Takes every item off the belt, for the stress test. */
    public void clearItems() {
        for (BeltLane lane : lanes) {
            lane.clear();
        }
        itemsChanged();
    }

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        Direction facing = facing();
        boolean moved = false;
        for (int i = 0; i < lanes.length; i++) {
            ItemStack waiting = entry.getItem(i);
            if (!waiting.isEmpty() && lanes[i].insert(waiting, 0)) {
                waiting.shrink(1);
                entry.setItem(i, waiting.isEmpty() ? ItemStack.EMPTY : waiting);
                clientOutdated = true;
                moved = true;
            }
        }
        int distance = SPEED * elapsedTicks;
        for (int i = 0; i < lanes.length; i++) {
            int lane = i;
            moved |= lanes[i].advance(distance, (item, overflow) -> handOff(level, facing, lane, item, overflow));
        }

        if (moved) {
            level.blockEntityChanged(worldPosition);
            wakeFeeders(level, facing);
        }
        if (clientOutdated) {
            clientOutdated = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        if (moved) {
            return true;
        }
        if (blockedAtInventory(level, facing)) {
            retryIn(BLOCKED_RETRY_TICKS);
        }
        return false;
    }

    /** Hands an item that reached the end of a lane to whatever is in front. */
    private boolean handOff(ServerLevel level, Direction facing, int lane, ItemStack item, int overflow) {
        BlockPos front = worldPosition.relative(facing);
        if (!level.isLoaded(front)) {
            return false;
        }
        boolean taken;
        if (level.getBlockEntity(front) instanceof BeltBlockEntity next) {
            Direction nextFacing = next.facing();
            if (nextFacing == facing.getOpposite()) {
                return false; // head on
            }
            int nextLane = lane;
            int position = Math.min(overflow, BeltLane.LENGTH / 2);
            if (nextFacing != facing) {
                // This belt is beside the next one. Fed only from here, the next one is a curve and the
                // lanes carry on; otherwise items are loaded onto the side nearest this belt.
                Direction fromNext = facing.getOpposite();
                BeltShape shape = BeltBlock.shapeFor(level, front, nextFacing);
                boolean curve = shape == BeltShape.LEFT && fromNext == nextFacing.getCounterClockWise()
                        || shape == BeltShape.RIGHT && fromNext == nextFacing.getClockWise();
                if (!curve) {
                    nextLane = fromNext == nextFacing.getCounterClockWise() ? LEFT : RIGHT;
                    position = BeltLane.LENGTH / 2;
                }
            }
            if (nextLane == lane) {
                // Carrying on along the same lane: if the next belt hasn't moved its items yet this
                // batch, its last item may be in the way; join the line right behind it rather than
                // wait a whole batch at the end of this belt.
                position = Math.min(position, next.lane(nextLane).tailRoom());
            }
            taken = next.receive(nextLane, item, position);
        } else {
            ItemAccess target = ItemTransfer.find(level, front, facing.getOpposite());
            taken = target != null && target.insert(item.copyWithCount(1), false) == 1;
        }
        if (taken) {
            clientOutdated = true;
        }
        return taken;
    }

    /** Whether an item waits at the end of a lane for an inventory (not a belt) in front to have room. */
    private boolean blockedAtInventory(ServerLevel level, Direction facing) {
        boolean waiting = false;
        for (BeltLane lane : lanes) {
            waiting |= !lane.isEmpty() && lane.position(0) >= BeltLane.LENGTH;
        }
        if (!waiting) {
            return false;
        }
        BlockPos front = worldPosition.relative(facing);
        return level.isLoaded(front) && !(level.getBlockEntity(front) instanceof BeltBlockEntity)
                && ItemTransfer.find(level, front, facing.getOpposite()) != null;
    }

    /** Wakes the belts that feed this one: there may be room for their items now. */
    private void wakeFeeders(ServerLevel level, Direction facing) {
        for (Direction side : new Direction[] {facing.getOpposite(), facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos pos = worldPosition.relative(side);
            if (BeltBlock.feeds(level, pos, side.getOpposite()) && level.getBlockEntity(pos) instanceof BeltBlockEntity feeder) {
                feeder.wake();
            }
        }
    }

    private void itemsChanged() {
        clientOutdated = true;
        markUnsaved();
        wake();
    }

    /**
     * Marks the chunk as needing saving. Not {@link #setChanged()}, which also tells neighbouring
     * comparators, looking up the blocks around; belts give comparators nothing, and this runs every
     * time an item is handed on.
     */
    private void markUnsaved() {
        if (level != null) {
            level.blockEntityChanged(worldPosition);
        }
    }

    /** Puts one of {@code stack} on the lane a player clicked, where they clicked. */
    boolean placeByHand(ItemStack stack, Vec3 hit) {
        Direction facing = facing();
        Vec3 local = hit.subtract(Vec3.atCenterOf(worldPosition));
        double along = local.x * facing.getStepX() + local.z * facing.getStepZ() + 0.5;
        int position = (int) Math.round(Math.clamp(along, 0, 1) * BeltLane.LENGTH);
        int lane = laneAt(facing, local);
        for (int offset : new int[] {0, -BeltLane.SPACING / 2, BeltLane.SPACING / 2}) {
            if (lanes[lane].canInsert(position + offset)) {
                return receive(lane, stack, position + offset);
            }
        }
        return false;
    }

    /** Takes every item off the lane a player clicked. */
    List<ItemStack> takeByHand(Vec3 hit) {
        Direction facing = facing();
        int lane = laneAt(facing, hit.subtract(Vec3.atCenterOf(worldPosition)));
        List<ItemStack> taken = new ArrayList<>();
        while (!lanes[lane].isEmpty()) {
            taken.add(lanes[lane].remove(0));
        }
        if (!taken.isEmpty()) {
            itemsChanged();
        }
        return taken;
    }

    private static int laneAt(Direction facing, Vec3 local) {
        Direction left = facing.getCounterClockWise();
        return local.x * left.getStepX() + local.z * left.getStepZ() >= 0 ? LEFT : RIGHT;
    }

    // Client

    /**
     * Client side: where each item on a lane is drawn this frame, projected from the last update by the
     * belt's own rules. Writes into {@code out} (room for {@link BeltLane#CAPACITY}) and returns how many.
     */
    public int projectLane(int lane, float partialTick, float[] out) {
        long now = level == null ? 0 : level.getGameTime();
        if (syncTime == Long.MIN_VALUE) {
            syncTime = now;
        }
        float elapsed = Math.min(now - syncTime + partialTick, 20 * 60);
        lanes[lane].project(SPEED * elapsed, out);
        return lanes[lane].size();
    }

    // Saving and syncing

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        entry.save(output);
        List<Entry> items = new ArrayList<>();
        for (int lane = 0; lane < lanes.length; lane++) {
            for (int i = 0; i < lanes[lane].size(); i++) {
                items.add(new Entry(lane, lanes[lane].position(i), lanes[lane].item(i)));
            }
        }
        output.store("lanes", ENTRY_CODEC.listOf(), items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        entry.load(input);
        for (BeltLane lane : lanes) {
            lane.clear();
        }
        for (Entry item : input.read("lanes", ENTRY_CODEC.listOf()).orElse(List.of())) {
            if (item.lane() >= 0 && item.lane() < lanes.length) {
                lanes[item.lane()].insert(item.item(), Math.clamp(item.position(), 0, BeltLane.LENGTH));
            }
        }
        syncTime = level != null && level.isClientSide() ? level.getGameTime() : Long.MIN_VALUE;
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
        if (level != null) {
            Containers.dropContents(level, pos, entry);
            for (BeltLane lane : lanes) {
                while (!lane.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, lane.remove(0));
                }
            }
        }
    }

    private record Entry(int lane, int position, ItemStack item) {
    }

    /**
     * The entry slots. A block beside the belt fills the lane on its side; anything else (a hopper above,
     * a belt-end machine behind) fills the left lane first.
     */
    private final class EntrySlots extends ItemBuffer {
        private static final int[] LEFT_ONLY = {LEFT};
        private static final int[] RIGHT_ONLY = {RIGHT};
        private static final int[] BOTH = {LEFT, RIGHT};

        EntrySlots() {
            super(Role.INPUT, Role.INPUT);
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            Direction facing = facing();
            if (side == facing.getCounterClockWise()) {
                return LEFT_ONLY;
            }
            if (side == facing.getClockWise()) {
                return RIGHT_ONLY;
            }
            return BOTH;
        }
    }
}
