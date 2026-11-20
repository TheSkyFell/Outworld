package io.github.theskyfell.outworld.core.mixin;

import io.github.theskyfell.outworld.core.grid.Grids;
import io.github.theskyfell.outworld.core.multiblock.MultiblockTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reports every block change in a loaded chunk on the server to {@link MultiblockTracker} and
 * {@link Grids} (cables, pipes and tanks). Neither loader has an event for all block changes (commands, explosions and
 * pistons included), so this is the one place Core hooks Minecraft directly.
 */
@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
    @Shadow
    @Final
    Level level;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void outworld$blockChanged(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> callback) {
        // The previous state comes back only when the block actually changed.
        BlockState previous = callback.getReturnValue();
        if (previous != null && level instanceof ServerLevel serverLevel) {
            MultiblockTracker.blockChanged(serverLevel, pos);
            Grids.blockChanged(serverLevel, pos, previous, state);
        }
    }
}
