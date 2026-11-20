package io.github.theskyfell.outworld.core.stress;

import net.minecraft.server.level.ServerLevel;

/** A built stress scenario. */
@FunctionalInterface
public interface StressRun {
    /**
     * Stops making new work, so that everything goes to sleep once its current work is done and the
     * idle cost can be measured.
     */
    void quiet(ServerLevel level);
}
