package io.github.theskyfell.outworld.industry.test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Temporary test battery block. Storage only: it never ticks. */
public final class TestBatteryBlock extends Block implements EntityBlock {
    public TestBatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestBatteryBlockEntity(pos, state);
    }
}
