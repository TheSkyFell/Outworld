package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.multiblock.MultiblockControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Temporary test multiblock controller for the {@code test_frame} structure. Right-click shows or
 * hides the build preview and says what is missing.
 */
public final class TestMultiblockBlock extends MultiblockControllerBlock {
    public TestMultiblockBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestMultiblockBlockEntity(pos, state);
    }
}
