package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Temporary test tank block. See {@link TestTankBlockEntity}. */
public final class TestTankBlock extends MachineBlock {
    public TestTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestTankBlockEntity(pos, state);
    }
}
