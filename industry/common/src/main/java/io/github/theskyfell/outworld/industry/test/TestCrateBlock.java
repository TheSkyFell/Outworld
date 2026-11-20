package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Temporary test crate block. See {@link TestCrateBlockEntity}. */
public final class TestCrateBlock extends MachineBlock {
    public TestCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestCrateBlockEntity(pos, state);
    }
}
