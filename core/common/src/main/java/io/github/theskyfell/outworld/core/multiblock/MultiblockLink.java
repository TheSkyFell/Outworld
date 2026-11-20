package io.github.theskyfell.outworld.core.multiblock;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps track of whether a controller's structure is complete. A controller block entity owns one and
 * calls it from its own work and lifecycle; see {@link MultiblockControllerBlockEntity} for the rules.
 *
 * <p>The structure is checked when the controller loads and again whenever a block in it changes
 * (including the controller turning); nothing is scanned on a timer. It turns with the controller's
 * {@code facing} block state property if it has one; otherwise it faces north.
 */
public final class MultiblockLink {
    private static final Set<ResourceKey<MultiblockStructure>> WARNED_UNKNOWN = ConcurrentHashMap.newKeySet();

    private final MachineBlockEntity owner;
    private final Supplier<ResourceKey<MultiblockStructure>> structureKey;
    private BiConsumer<Boolean, MultiblockStructure.Match> formedListener = (formed, match) -> { };
    private boolean formed;
    private boolean checkNeeded = true;
    private boolean unknownStructure;
    private MultiblockStructure.Match lastMatch;
    private MultiblockStructure claimedStructure;
    private Direction claimedFacing;

    /**
     * @param owner the controller's block entity, woken when a block in the structure changes
     * @param structureKey the structure it forms, from the {@link MultiblockStructure#REGISTRY} data registry
     */
    public MultiblockLink(MachineBlockEntity owner, Supplier<ResourceKey<MultiblockStructure>> structureKey) {
        this.owner = owner;
        this.structureKey = structureKey;
    }

    /** Called with true (and where each role's blocks are) when the structure becomes complete, false when it stops being so. */
    public void onFormedChanged(BiConsumer<Boolean, MultiblockStructure.Match> listener) {
        this.formedListener = listener;
    }

    public ResourceKey<MultiblockStructure> structureKey() {
        return structureKey.get();
    }

    public boolean isFormed() {
        return formed;
    }

    /** The most recent check's result, or null before the first check. */
    public MultiblockStructure.Match lastMatch() {
        return lastMatch;
    }

    /** Whether a block in the structure changed since the last check. */
    public boolean checkPending() {
        return checkNeeded;
    }

    /** Which way the controller, and so the structure, faces. */
    public Direction facing() {
        BlockState state = owner.getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    /** The structure, or null if no such structure is loaded. Works on both sides. */
    public MultiblockStructure structure(Level level) {
        return level.registryAccess().lookupOrThrow(MultiblockStructure.REGISTRY).getValue(structureKey());
    }

    /**
     * The empty spot nearest {@code from} where the structure needs {@code block}, or null if there is
     * none. Spots holding another block don't count: something would have to be broken first.
     */
    public BlockPos nextSpotFor(Level level, Block block, Vec3 from) {
        MultiblockStructure structure = structure(level);
        if (structure == null) {
            return null;
        }
        BlockState placed = block.defaultBlockState();
        Direction facing = facing();
        BlockPos origin = owner.getBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (MultiblockStructure.Entry entry : structure.entries()) {
            if (!placed.is(entry.part().blocks())) {
                continue;
            }
            BlockPos at = MultiblockStructure.place(entry, origin, facing);
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState current = level.getBlockState(at);
            if (current.is(entry.part().blocks()) || !current.canBeReplaced()) {
                continue;
            }
            double distance = Vec3.atCenterOf(at).distanceToSqr(from);
            if (distance < nearestDistance) {
                nearest = at;
                nearestDistance = distance;
            }
        }
        return nearest;
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

    /**
     * Checks the structure if a block in it changed. Call at the start of the controller's work.
     *
     * @return true if part of the structure isn't loaded: the controller should stay awake and call
     *     again next batch, since loading a chunk changes no block and would wake nothing
     */
    public boolean checkIfNeeded(ServerLevel level) {
        if (!checkNeeded) {
            return false;
        }
        checkNeeded = false;
        check(level);
        return checkNeeded;
    }

    /** Called by the tracker when a block in the structure changes. */
    void structureChanged() {
        checkNeeded = true;
        owner.wake();
    }

    /** Call from the controller's {@code clearRemoved}, before the machine wakes: claims are dropped on removal, so check again. */
    public void loaded() {
        checkNeeded = true;
    }

    /** Call from the controller's {@code setRemoved}. */
    public void removed(Level level) {
        claimedStructure = null;
        claimedFacing = null;
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.getServer().isSameThread()) {
                MultiblockTracker.release(serverLevel, this);
            } else {
                serverLevel.getServer().execute(() -> MultiblockTracker.release(serverLevel, this));
            }
        }
    }

    /** Saves whether the structure is complete, for {@code /data get} and debugging. It is checked again on load. */
    public void save(ValueOutput output) {
        output.putBoolean("formed", formed);
    }

    private void check(ServerLevel level) {
        MultiblockStructure structure = structure(level);
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
        BlockPos origin = owner.getBlockPos();
        if (structure != claimedStructure || facing != claimedFacing) {
            List<BlockPos> positions = new ArrayList<>(structure.positions(origin, facing));
            positions.add(origin); // the controller itself: turning it moves the structure
            MultiblockTracker.claim(level, this, positions);
            claimedStructure = structure;
            claimedFacing = facing;
        }

        MultiblockStructure.Match match = structure.match(level, origin, facing);
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
        owner.setChanged();
        formedListener.accept(formed, match);
    }
}
