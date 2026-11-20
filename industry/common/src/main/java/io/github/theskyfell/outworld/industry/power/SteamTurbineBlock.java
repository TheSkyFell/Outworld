package io.github.theskyfell.outworld.industry.power;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The steam turbine. See {@link SteamTurbineBlockEntity}. */
public final class SteamTurbineBlock extends RecipeMachineBlock {
    public SteamTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamTurbineBlockEntity(pos, state);
    }
}
