package io.github.theskyfell.outworld.core.multiblock;

import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The block entity of a machine that exists only as a multiblock's controller. It does its work in
 * {@link #workFormed}, which only runs while the structure is complete. (A recipe machine that is also
 * a multiblock extends {@code MultiblockRecipeMachineBlockEntity} instead.)
 *
 * <p>The structure is checked when the controller loads and again whenever a block in it changes
 * (including the controller turning); nothing is scanned on a timer. {@link #onFormed} and
 * {@link #onUnformed} run when it becomes complete or incomplete. After loading, the first complete
 * check counts as forming, so {@code onFormed} always runs before the controller works.
 *
 * <p>The structure turns with the controller's {@code facing} block state property if it has one;
 * otherwise it faces north.
 */
public abstract class MultiblockControllerBlockEntity extends MachineBlockEntity implements MultiblockController {
    private final MultiblockLink link = new MultiblockLink(this, this::structureKey);

    protected MultiblockControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        link.onFormedChanged((formed, match) -> {
            if (formed) {
                onFormed(match);
            } else {
                onUnformed();
            }
        });
    }

    /** The structure this controller forms, from the {@link MultiblockStructure#REGISTRY} data registry. */
    public abstract ResourceKey<MultiblockStructure> structureKey();

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

    @Override
    public final MultiblockLink multiblock() {
        return link;
    }

    public final boolean isFormed() {
        return link.isFormed();
    }

    /** The most recent check's result, or null before the first check. */
    public final MultiblockStructure.Match lastMatch() {
        return link.lastMatch();
    }

    /** Which way the controller, and so the structure, faces. */
    public Direction facing() {
        return link.facing();
    }

    /** The structure this controller forms, or null if no such structure is loaded. Works on both sides. */
    public final MultiblockStructure structure(Level level) {
        return link.structure(level);
    }

    /** See {@link MultiblockLink#nextSpotFor}. */
    public BlockPos nextSpotFor(Level level, Block block, Vec3 from) {
        return link.nextSpotFor(level, block, from);
    }

    /** A one-line status for players: complete, or what is missing where. */
    public Component status() {
        return link.status();
    }

    @Override
    protected final boolean work(ServerLevel level, int elapsedTicks) {
        if (link.checkIfNeeded(level)) {
            return true;
        }
        if (!link.isFormed()) {
            return false; // sleep until a block in the structure changes
        }
        boolean keepWorking = workFormed(level, elapsedTicks);
        // A block in the structure may have changed during the work itself, when waking is ignored.
        return keepWorking || link.checkPending();
    }

    @Override
    public void clearRemoved() {
        link.loaded();
        super.clearRemoved();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        link.removed(level);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        link.save(output);
    }
}
