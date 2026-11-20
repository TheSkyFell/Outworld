package io.github.theskyfell.outworld.industry.power;

import io.github.theskyfell.outworld.core.energy.EnergyConductor;
import io.github.theskyfell.outworld.core.energy.EnergyGrids;
import io.github.theskyfell.outworld.industry.pipe.ConduitBlock;
import io.github.theskyfell.outworld.industry.pipe.GridInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An energy cable. Touching cables form one energy grid with a shared buffer (see Core's
 * {@link EnergyGrids}), which every cable exposes on every side: generators push into it, machines draw
 * from it, and it hands what it's given to the machines and batteries along it.
 */
public final class CableBlock extends ConduitBlock implements EnergyConductor {
    /** What each cable adds to its grid's buffer, in FE. */
    public static final long CAPACITY = 1_000;

    public CableBlock(Properties properties) {
        super(properties, 5);
    }

    @Override
    public long gridCapacity() {
        return CAPACITY;
    }

    @Override
    protected boolean connectsTo(BlockState neighbour) {
        return neighbour.getBlock() instanceof EnergyConductor || neighbour.hasBlockEntity();
    }

    @Override
    protected Component describeGrid(ServerLevel level, BlockPos pos) {
        return GridInfo.energy(EnergyGrids.bufferAt(level, pos));
    }
}
