package io.github.theskyfell.outworld.industry.pipe;

import io.github.theskyfell.outworld.core.gas.GasConductor;
import io.github.theskyfell.outworld.core.gas.GasGrids;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A gas pipe. Touching gas pipes and gas tank blocks form one gas grid with a shared tank (see
 * Core's {@link GasGrids}), holding one gas at a time. Machines push what they make into it and
 * draw what they need, and it hands what it's given to the machines and tanks along it.
 */
public final class GasPipeBlock extends ConduitBlock implements GasConductor {
    /** What each pipe adds to its grid's tank, in millibuckets (one bucket). */
    public static final long CAPACITY = 1_000;

    public GasPipeBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    public long gasGridCapacity() {
        return CAPACITY;
    }

    @Override
    protected boolean connectsTo(BlockState neighbour) {
        return neighbour.getBlock() instanceof GasConductor || neighbour.hasBlockEntity();
    }

    @Override
    protected Component describeGrid(ServerLevel level, BlockPos pos) {
        return GridInfo.gas(GasGrids.tankAt(level, pos));
    }
}
