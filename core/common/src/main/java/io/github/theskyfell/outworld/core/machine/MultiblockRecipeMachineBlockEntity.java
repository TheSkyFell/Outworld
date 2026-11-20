package io.github.theskyfell.outworld.core.machine;

import io.github.theskyfell.outworld.core.multiblock.MultiblockController;
import io.github.theskyfell.outworld.core.multiblock.MultiblockLink;
import io.github.theskyfell.outworld.core.multiblock.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A recipe machine that only runs while its multiblock structure is complete, such as the arc
 * furnace. Its block is a {@link MultiblockRecipeMachineBlock}. Inputs, outputs and energy all go
 * through the controller block itself; the rest of the structure is just the shape.
 */
public abstract class MultiblockRecipeMachineBlockEntity extends RecipeMachineBlockEntity implements MultiblockController {
    private final MultiblockLink link = new MultiblockLink(this, this::structureKey);

    protected MultiblockRecipeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Layout layout) {
        super(type, pos, state, layout);
    }

    /** The structure this machine needs, from the {@link MultiblockStructure#REGISTRY} data registry. */
    public abstract ResourceKey<MultiblockStructure> structureKey();

    @Override
    public final MultiblockLink multiblock() {
        return link;
    }

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        if (link.checkIfNeeded(level)) {
            return true;
        }
        if (!link.isFormed()) {
            return false; // sleep until a block in the structure changes
        }
        return super.work(level, elapsedTicks) || link.checkPending();
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
