package io.github.theskyfell.outworld.industry.power;

import io.github.theskyfell.outworld.core.energy.EnergyConductor;
import io.github.theskyfell.outworld.core.energy.EnergyGrids;
import io.github.theskyfell.outworld.industry.pipe.GridInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A battery: a block of cable that stores {@link #CAPACITY}. Batteries and cables that touch share one
 * energy grid, so a stack of batteries is one bigger battery bank, of any shape, and generators,
 * machines and cables can attach anywhere on it. What it holds is saved with the world. Right-click
 * it with an empty hand to see how full its grid is.
 */
public final class BatteryBlock extends Block implements EnergyConductor {
    /** What each battery adds to its grid, in FE. */
    public static final long CAPACITY = 100_000;

    public BatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public long gridCapacity() {
        return CAPACITY;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(GridInfo.energy(EnergyGrids.bufferAt(serverLevel, pos)), true);
        }
        return InteractionResult.SUCCESS;
    }
}
