package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Temporary test gas tank block. See {@link TestGasTankBlockEntity}. */
public final class TestGasTankBlock extends MachineBlock {
    public TestGasTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestGasTankBlockEntity(pos, state);
    }
}
