package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The electric furnace. See {@link ElectricFurnaceBlockEntity}. */
public final class ElectricFurnaceBlock extends RecipeMachineBlock {
    public ElectricFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }
}
