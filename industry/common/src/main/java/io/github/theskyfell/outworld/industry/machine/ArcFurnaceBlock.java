package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.MultiblockRecipeMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The arc furnace's controller. See {@link ArcFurnaceBlockEntity}. */
public final class ArcFurnaceBlock extends MultiblockRecipeMachineBlock {
    public ArcFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcFurnaceBlockEntity(pos, state);
    }
}
