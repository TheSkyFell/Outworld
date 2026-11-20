package io.github.theskyfell.outworld.core.client;

import io.github.theskyfell.outworld.core.multiblock.MultiblockControllerBlockEntity;
import io.github.theskyfell.outworld.core.multiblock.MultiblockStructure;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The build preview of multiblock controllers: floating block markers (the particles that show barrier
 * and light blocks) wherever a structure is missing a block. A barrier marker, just in front of the
 * block, flags a position that holds the wrong block. The preview hides itself once the structure is
 * complete, or when the player goes far away or changes dimension.
 *
 * <p>It is worked out on the client from the synced structure and the blocks the client sees, so it
 * sends nothing over the network. A preview is refreshed every {@link #REFRESH_TICKS} ticks, and a
 * marker is only respawned when the previous one runs out, so a preview costs a handful of particles.
 */
public final class MultiblockPreview {
    /** How often previews are compared with the world. */
    static final int REFRESH_TICKS = 10;
    /** How long vanilla's block marker particle lasts. */
    private static final int MARKER_TICKS = 80;
    /** The most markers one preview shows at once. */
    private static final int MAX_MARKERS = 512;
    /** Previews of controllers further away than this are dropped. */
    private static final double MAX_DISTANCE = 96;

    private static final Map<BlockPos, Map<BlockPos, Long>> PREVIEWS = new HashMap<>();
    private static Level previewLevel;

    private MultiblockPreview() {
    }

    /** Shows the preview of the controller at {@code pos}, or hides it if it is showing. */
    public static void toggle(Level level, BlockPos pos) {
        if (level != previewLevel) {
            PREVIEWS.clear();
            previewLevel = level;
        }
        BlockPos controller = pos.immutable();
        if (PREVIEWS.remove(controller) == null) {
            Map<BlockPos, Long> markers = new HashMap<>();
            PREVIEWS.put(controller, markers);
            Player player = Minecraft.getInstance().player;
            if (player != null && !refresh(level, player, controller, markers, level.getGameTime())) {
                PREVIEWS.remove(controller); // already complete: nothing to show
            }
        }
    }

    /** Called every client tick. */
    static void tick(Minecraft minecraft) {
        if (PREVIEWS.isEmpty()) {
            return;
        }
        Level level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || level != previewLevel) {
            PREVIEWS.clear();
            previewLevel = null;
            return;
        }
        long now = level.getGameTime();
        if (now % REFRESH_TICKS != 0) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Map<BlockPos, Long>>> previews = PREVIEWS.entrySet().iterator();
        while (previews.hasNext()) {
            Map.Entry<BlockPos, Map<BlockPos, Long>> preview = previews.next();
            if (!refresh(level, player, preview.getKey(), preview.getValue(), now)) {
                previews.remove();
            }
        }
    }

    /**
     * Spawns markers for the missing blocks of one preview.
     *
     * @return false if the preview should end: the structure is complete, the controller is gone, or
     *         the player is far away
     */
    private static boolean refresh(Level level, Player player, BlockPos pos, Map<BlockPos, Long> markers, long now) {
        if (player.position().distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE * MAX_DISTANCE) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MultiblockControllerBlockEntity controller)) {
            return false;
        }
        MultiblockStructure structure = level.registryAccess().lookupOrThrow(MultiblockStructure.REGISTRY)
                .getValue(controller.structureKey());
        if (structure == null) {
            return false;
        }
        markers.values().removeIf(until -> until <= now);

        Direction facing = controller.facing();
        boolean incomplete = false;
        int shown = markers.size();
        for (MultiblockStructure.Entry entry : structure.entries()) {
            BlockPos at = MultiblockStructure.place(entry, pos, facing);
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState current = level.getBlockState(at);
            if (current.is(entry.part().blocks())) {
                continue;
            }
            incomplete = true;
            if (markers.containsKey(at) || shown >= MAX_MARKERS) {
                continue;
            }
            BlockState expected = shownState(entry.part().blocks());
            if (current.canBeReplaced() && !expected.isAir()) {
                // Empty spot: float the block that belongs here.
                level.addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, expected),
                        at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0, 0, 0);
            } else {
                // Wrong block: a barrier marker just outside it, on the player's side.
                Vec3 center = Vec3.atCenterOf(at);
                Vec3 marker = center.add(player.getEyePosition().subtract(center).normalize().scale(0.75));
                level.addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()),
                        marker.x, marker.y, marker.z, 0, 0, 0);
            }
            // Respawn a little before it runs out, so the marker doesn't flicker.
            markers.put(at, now + MARKER_TICKS - REFRESH_TICKS);
            shown++;
        }
        return incomplete;
    }

    /** The block to show for a part: the first of its blocks. */
    private static BlockState shownState(HolderSet<Block> blocks) {
        return blocks.size() == 0 ? Blocks.AIR.defaultBlockState() : blocks.get(0).value().defaultBlockState();
    }
}
