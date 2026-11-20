package io.github.theskyfell.outworld.industry.power;

import io.github.theskyfell.outworld.core.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A solar panel. See {@link SolarPanelBlockEntity}. */
public final class SolarPanelBlock extends MachineBlock {
    public SolarPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }
}
