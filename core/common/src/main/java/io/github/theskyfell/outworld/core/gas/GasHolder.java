package io.github.theskyfell.outworld.core.gas;

import net.minecraft.core.Direction;

/**
 * Implemented by block entities that hold gas. {@link GasTransfer#find} finds them, so gas pipes and
 * machines (from Outworld or any mod that builds on Outworld Core) can reach their tanks.
 */
public interface GasHolder {
    /**
     * The gas storage reachable from {@code side}, or null for none on that side.
     *
     * @param side the face being touched, or null when the caller doesn't say
     */
    GasAccess gasAccess(Direction side);
}
