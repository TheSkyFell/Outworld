package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The ore washer. See {@link OreWasherBlockEntity}. */
public final class OreWasherBlock extends RecipeMachineBlock {
    public OreWasherBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreWasherBlockEntity(pos, state);
    }
}
