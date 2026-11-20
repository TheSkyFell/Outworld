package io.github.theskyfell.outworld.core.client;

import io.github.theskyfell.outworld.core.multiblock.MultiblockControllerBlockEntity;
import io.github.theskyfell.outworld.core.multiblock.MultiblockStructure;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The build preview of multiblock controllers. Every spot where the structure is missing a block gets
 * its edges traced in teal sparks, with the block that belongs there floating inside (the marker
 * particle barrier and light blocks use). A spot that holds the wrong block is traced in orange
 * instead. The preview hides itself once the structure is complete, or when the player goes far away
 * or changes dimension.
 *
 * <p>It is worked out on the client from the synced structure and the blocks the client sees, so it
 * sends nothing over the network. Previews refresh every {@link #REFRESH_TICKS} ticks. Only the
 * {@link #MAX_OUTLINES} problem spots nearest the player are traced each time, and a floating block
 * is only respawned when the previous one runs out, which keeps a preview to a few hundred particles.
 * The traced edges follow the player's particle setting; the floating blocks always show.
 */
public final class MultiblockPreview {
    /** How often previews are compared with the world and their sparks renewed. */
    static final int REFRESH_TICKS = 10;
    /** How long vanilla's block marker particle lasts. */
    private static final int MARKER_TICKS = 80;
    /** The most floating blocks one preview shows at once. */
    private static final int MAX_MARKERS = 512;
    /** The most spots one preview traces per refresh, nearest first. */
    private static final int MAX_OUTLINES = 32;
    /** Previews of controllers further away than this are dropped. */
    private static final double MAX_DISTANCE = 96;

    private static final ParticleOptions MISSING_SPARK = ParticleTypes.SCRAPE;
    private static final ParticleOptions WRONG_SPARK = ParticleTypes.WAX_ON;
    /** The corners of a cube and two points along each edge, as fractions of its side. */
    private static final List<Vec3> OUTLINE_POINTS = outlinePoints();

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

    /** A spot that doesn't hold the right block. */
    private record Problem(BlockPos pos, BlockState expected, boolean wrongBlock) {
    }

    /**
     * Traces the problem spots of one preview and floats the missing blocks.
     *
     * @param markers when each spot's floating block runs out
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
        MultiblockStructure structure = controller.structure(level);
        if (structure == null) {
            return false;
        }

        Direction facing = controller.facing();
        List<Problem> problems = new ArrayList<>();
        for (MultiblockStructure.Entry entry : structure.entries()) {
            BlockPos at = MultiblockStructure.place(entry, pos, facing);
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState current = level.getBlockState(at);
            if (current.is(entry.part().blocks())) {
                continue;
            }
            BlockState expected = shownState(entry.part().blocks());
            problems.add(new Problem(at, expected, !current.canBeReplaced() || expected.isAir()));
        }
        if (problems.isEmpty()) {
            return false;
        }

        Vec3 eye = player.getEyePosition();
        problems.sort(Comparator.comparingDouble(problem -> Vec3.atCenterOf(problem.pos()).distanceToSqr(eye)));
        markers.values().removeIf(until -> until <= now);
        int floating = markers.size();
        for (int i = 0; i < problems.size(); i++) {
            Problem problem = problems.get(i);
            if (i < MAX_OUTLINES) {
                traceEdges(level, problem.pos(), problem.wrongBlock() ? WRONG_SPARK : MISSING_SPARK);
            }
            if (!problem.wrongBlock() && floating < MAX_MARKERS && !markers.containsKey(problem.pos())) {
                BlockPos at = problem.pos();
                level.addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, problem.expected()),
                        at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0, 0, 0);
                // Respawn a little before it runs out, so it doesn't flicker.
                markers.put(at, now + MARKER_TICKS - REFRESH_TICKS);
                floating++;
            }
        }
        return true;
    }

    /** Spawns a spark at each corner of the block and along each of its edges. */
    private static void traceEdges(Level level, BlockPos pos, ParticleOptions spark) {
        // Slightly larger than the block, so sparks around a solid block aren't hidden inside it.
        double min = -0.02;
        double side = 1.04;
        for (Vec3 point : OUTLINE_POINTS) {
            level.addParticle(spark,
                    pos.getX() + min + point.x * side,
                    pos.getY() + min + point.y * side,
                    pos.getZ() + min + point.z * side,
                    0, 0, 0);
        }
    }

    /** The 32 points of a cube's edges at thirds: 8 corners and 2 points along each of the 12 edges. */
    private static List<Vec3> outlinePoints() {
        List<Vec3> points = new ArrayList<>();
        for (int x = 0; x <= 3; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = 0; z <= 3; z++) {
                    int onFaces = (x % 3 == 0 ? 1 : 0) + (y % 3 == 0 ? 1 : 0) + (z % 3 == 0 ? 1 : 0);
                    if (onFaces >= 2) {
                        points.add(new Vec3(x / 3.0, y / 3.0, z / 3.0));
                    }
                }
            }
        }
        return List.copyOf(points);
    }

    /** The block to show for a part: the first of its blocks. */
    private static BlockState shownState(HolderSet<Block> blocks) {
        return blocks.size() == 0 ? Blocks.AIR.defaultBlockState() : blocks.get(0).value().defaultBlockState();
    }
}
