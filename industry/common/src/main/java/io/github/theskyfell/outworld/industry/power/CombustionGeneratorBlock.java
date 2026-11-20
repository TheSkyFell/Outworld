package io.github.theskyfell.outworld.industry.power;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The combustion generator. See {@link CombustionGeneratorBlockEntity}. */
public final class CombustionGeneratorBlock extends RecipeMachineBlock {
    public CombustionGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CombustionGeneratorBlockEntity(pos, state);
    }
}
