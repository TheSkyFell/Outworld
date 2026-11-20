package io.github.theskyfell.outworld.core.client;

import io.github.theskyfell.outworld.core.multiblock.MultiblockControllerBlockEntity;
import io.github.theskyfell.outworld.core.multiblock.MultiblockStructure;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The build preview of multiblock controllers. Every spot where the structure is missing a block gets
 * its edges traced in teal sparks; a spot that holds the wrong block (or anything at all, where the
 * structure needs air) is traced in orange. It doesn't show which block goes where, since a spot often
 * takes any of several blocks (machine cores of different tiers, say); the status message says what is
 * needed. The preview hides itself once the structure is complete, or when the player goes far away or
 * changes dimension.
 *
 * <p>It is worked out on the client from the synced structure and the blocks the client sees, so it
 * sends nothing over the network. Previews refresh every {@link #REFRESH_TICKS} ticks, and only the
 * {@link #MAX_OUTLINES} problem spots nearest the player are traced each time, which keeps a preview
 * to about a thousand short-lived particles. The sparks follow the player's particle setting.
 */
public final class MultiblockPreview {
    /** How often previews are compared with the world and their sparks renewed. */
    static final int REFRESH_TICKS = 10;
    /** The most spots one preview traces per refresh, nearest first. */
    private static final int MAX_OUTLINES = 32;
    /** Previews of controllers further away than this are dropped. */
    private static final double MAX_DISTANCE = 96;

    private static final ParticleOptions MISSING_SPARK = ParticleTypes.SCRAPE;
    private static final ParticleOptions WRONG_SPARK = ParticleTypes.WAX_ON;
    /** The corners of a cube and two points along each edge, as fractions of its side. */
    private static final List<Vec3> OUTLINE_POINTS = outlinePoints();

    private static final Set<BlockPos> PREVIEWS = new HashSet<>();
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
        if (!PREVIEWS.remove(controller)) {
            Player player = Minecraft.getInstance().player;
            if (player != null && refresh(level, player, controller)) {
                PREVIEWS.add(controller); // unless already complete: then there's nothing to show
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
        Iterator<BlockPos> previews = PREVIEWS.iterator();
        while (previews.hasNext()) {
            if (!refresh(level, player, previews.next())) {
                previews.remove();
            }
        }
    }

    /** A spot that doesn't hold the right block. */
    private record Problem(BlockPos pos, boolean wrongBlock) {
    }

    /**
     * Traces the problem spots of one preview.
     *
     * @return false if the preview should end: the structure is complete, the controller is gone, or
     *         the player is far away
     */
    private static boolean refresh(Level level, Player player, BlockPos pos) {
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
            // Wrong: something solid is in the way, or anything at all where the structure needs air.
            boolean wrong = !current.isAir()
                    && (!current.canBeReplaced() || Blocks.AIR.defaultBlockState().is(entry.part().blocks()));
            problems.add(new Problem(at, wrong));
        }
        if (problems.isEmpty()) {
            return false;
        }

        Vec3 eye = player.getEyePosition();
        problems.sort(Comparator.comparingDouble(problem -> Vec3.atCenterOf(problem.pos()).distanceToSqr(eye)));
        for (int i = 0; i < Math.min(problems.size(), MAX_OUTLINES); i++) {
            Problem problem = problems.get(i);
            traceEdges(level, problem.pos(), problem.wrongBlock() ? WRONG_SPARK : MISSING_SPARK);
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
}
