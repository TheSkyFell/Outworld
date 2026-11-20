package io.github.theskyfell.outworld.industry.pipe;

import io.github.theskyfell.outworld.core.fluid.FluidConductor;
import io.github.theskyfell.outworld.core.fluid.FluidGrids;
import io.github.theskyfell.outworld.core.fluid.FluidUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fluid pipe. Touching fluid pipes and tank blocks form one fluid grid with a shared tank (see
 * Core's {@link FluidGrids}), holding one fluid at a time. Machines push what they make into it
 * and draw what they need, and it hands what it's given to the machines and tanks along it.
 */
public final class FluidPipeBlock extends ConduitBlock implements FluidConductor {
    /** What each pipe adds to its grid's tank, in droplets (one bucket). */
    public static final long CAPACITY = FluidUnits.BUCKET;

    public FluidPipeBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    public long fluidGridCapacity() {
        return CAPACITY;
    }

    @Override
    protected boolean connectsTo(BlockState neighbour) {
        return neighbour.getBlock() instanceof FluidConductor || neighbour.hasBlockEntity();
    }

    @Override
    protected Component describeGrid(ServerLevel level, BlockPos pos) {
        return GridInfo.fluid(FluidGrids.tankAt(level, pos));
    }
}
