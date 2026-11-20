package io.github.theskyfell.outworld.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

/**
 * The base of every Outworld machine block. Its block entity must be a {@link MachineBlockEntity}.
 *
 * <p>It wakes the machine whenever a neighbour changes, since that can give it work (a new inventory
 * or cable next to it, a redstone signal). It never returns a vanilla block entity ticker: the
 * machine scheduler runs machines instead.
 */
public abstract class MachineBlock extends Block implements EntityBlock {
    protected MachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.wake();
        }
    }
}
