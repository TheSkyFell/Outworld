package io.github.theskyfell.outworld.core.multiblock;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The block entity of a multiblock's controller. It is a machine: it does its work in
 * {@link #workFormed}, which only runs while the structure is complete.
 *
 * <p>The structure is checked when the controller loads and again whenever a block in it changes
 * (including the controller turning); nothing is scanned on a timer. {@link #onFormed} and
 * {@link #onUnformed} run when it becomes complete or incomplete. After loading, the first complete
 * check counts as forming, so {@code onFormed} always runs before the controller works.
 *
 * <p>The structure turns with the controller's {@code facing} block state property if it has one;
 * otherwise it faces north.
 */
public abstract class MultiblockControllerBlockEntity extends MachineBlockEntity {
    private static final Set<ResourceKey<MultiblockStructure>> WARNED_UNKNOWN = ConcurrentHashMap.newKeySet();

    private boolean formed;
    private boolean checkNeeded = true;
    private boolean unknownStructure;
    private MultiblockStructure.Match lastMatch;
    private MultiblockStructure claimedStructure;
    private Direction claimedFacing;

    protected MultiblockControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The structure this controller forms, from the {@link MultiblockStructure#REGISTRY} data registry. */
    protected abstract ResourceKey<MultiblockStructure> structureKey();

    /**
     * Does {@code elapsedTicks} ticks of work while the structure is complete. Same contract as
     * {@link MachineBlockEntity#work}.
     *
     * @return true to stay awake, false to sleep until woken
     */
    protected abstract boolean workFormed(ServerLevel level, int elapsedTicks);

    /** The structure has become complete. {@code match} says where each role's blocks are. */
    protected void onFormed(MultiblockStructure.Match match) {
    }

    /** The structure was complete and isn't any more. */
    protected void onUnformed() {
    }

    public final boolean isFormed() {
        return formed;
    }

    /** The most recent check's result, or null before the first check. */
    public final MultiblockStructure.Match lastMatch() {
        return lastMatch;
    }

    /** Which way the controller, and so the structure, faces. */
    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    /** A one-line status for players: complete, or what is missing where. */
    public Component status() {
        if (unknownStructure) {
            return Component.literal("Unknown structure " + structureKey().identifier() + ": check the data pack");
        }
        if (formed) {
            return Component.literal("Structure complete");
        }
        if (lastMatch == null || lastMatch.problem() == null) {
            return Component.literal("Checking the structure...");
        }
        BlockPos pos = lastMatch.problem();
        String at = " at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        if (lastMatch.status() == MultiblockStructure.Status.NOT_LOADED) {
            return Component.literal("Waiting for the chunk" + at + " to load");
        }
        return Component.literal("Needs ").append(lastMatch.expected().describe()).append(at);
    }

    @Override
    protected final boolean work(ServerLevel level, int elapsedTicks) {
        if (checkNeeded) {
            checkNeeded = false;
            check(level);
            if (checkNeeded) {
                // Part of the structure isn't loaded. Loading a chunk changes no block, so nothing
                // would wake us: stay awake and try again next batch.
                return true;
            }
        }
        if (!formed) {
            return false; // sleep until a block in the structure changes
        }
        boolean keepWorking = workFormed(level, elapsedTicks);
        // A block in the structure may have changed during the work itself, when waking is ignored.
        return keepWorking || checkNeeded;
    }

    /** Called by the tracker when a block in the structure changes. */
    final void structureChanged() {
        checkNeeded = true;
        wake();
    }

    private void check(ServerLevel level) {
        MultiblockStructure structure = level.registryAccess().lookupOrThrow(MultiblockStructure.REGISTRY).getValue(structureKey());
        if (structure == null) {
            if (WARNED_UNKNOWN.add(structureKey())) {
                OutworldCore.LOG.warn("No multiblock structure {} is loaded; its controllers can't form", structureKey().identifier());
            }
            unknownStructure = true;
            lastMatch = null;
            MultiblockTracker.release(level, this);
            claimedStructure = null;
            setFormed(false, null);
            return;
        }
        unknownStructure = false;

        Direction facing = facing();
        if (structure != claimedStructure || facing != claimedFacing) {
            List<BlockPos> positions = new ArrayList<>(structure.positions(worldPosition, facing));
            positions.add(worldPosition); // the controller itself: turning it moves the structure
            MultiblockTracker.claim(level, this, positions);
            claimedStructure = structure;
            claimedFacing = facing;
        }

        MultiblockStructure.Match match = structure.match(level, worldPosition, facing);
        lastMatch = match;
        if (match.status() == MultiblockStructure.Status.NOT_LOADED) {
            checkNeeded = true;
            return;
        }
        setFormed(match.complete(), match);
    }

    private void setFormed(boolean nowFormed, MultiblockStructure.Match match) {
        if (nowFormed == formed) {
            return;
        }
        formed = nowFormed;
        setChanged();
        if (formed) {
            onFormed(match);
        } else {
            onUnformed();
        }
    }

    @Override
    public void clearRemoved() {
        // Claims are dropped whenever the controller is removed, so check (and claim) again when it returns.
        checkNeeded = true;
        super.clearRemoved();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        claimedStructure = null;
        claimedFacing = null;
        if (level instanceof ServerLevel serverLevel) {
            onServerThread(serverLevel, () -> MultiblockTracker.release(serverLevel, this));
        }
    }

    /** Saves whether the structure is complete, for {@code /data get} and debugging. It is checked again on load. */
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("formed", formed);
    }
}
