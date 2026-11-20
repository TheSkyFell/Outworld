package io.github.theskyfell.outworld.industry.pipe;

import io.github.theskyfell.outworld.core.gas.GasConductor;
import io.github.theskyfell.outworld.core.gas.GasGrids;
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
 * A gas tank: a block of gas pipe that holds {@link #CAPACITY}. Tanks and pipes that touch share one
 * grid, so stacking tanks makes a bigger tank, and what it holds is saved with the world. Right-click
 * it with an empty hand to see what it holds.
 */
public final class GasTankBlock extends Block implements GasConductor {
    /** What each tank block adds to its grid, in millibuckets (32 buckets). */
    public static final long CAPACITY = 32_000;

    public GasTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public long gasGridCapacity() {
        return CAPACITY;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(GridInfo.gas(GasGrids.tankAt(serverLevel, pos)), true);
        }
        return InteractionResult.SUCCESS;
    }
}
