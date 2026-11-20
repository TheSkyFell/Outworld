package io.github.theskyfell.outworld.core.stress;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A repeatable load for {@code /outworld stress}: builds many of something (machines, belts, drones)
 * in a fresh area so its cost can be measured. Each mod registers its own with
 * {@link StressScenarios#register}.
 */
public interface StressScenario {
    /** One line for {@code /outworld stress list}. */
    String description();

    /** How many units to build when the command doesn't say. */
    int defaultCount();

    /** The area {@link #build} will use. Its chunks are loaded and kept loaded before building. */
    BoundingBox area(BlockPos origin, int count);

    /**
     * Builds {@code count} units at {@code origin} and sets them working. Server thread only.
     *
     * @return how to make them stop working, for the idle half of the test
     */
    StressRun build(ServerLevel level, BlockPos origin, int count);
}
