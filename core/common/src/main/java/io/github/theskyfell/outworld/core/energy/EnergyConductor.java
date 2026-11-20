package io.github.theskyfell.outworld.core.energy;

/**
 * Implemented by blocks that carry energy, such as cables. Touching conductors join into one
 * {@link EnergyGrids grid} with a shared buffer, which every conductor in it exposes on every side:
 * generators push into it and machines draw from it, wherever they touch it.
 */
public interface EnergyConductor {
    /** How much this conductor adds to its grid's buffer, in FE. */
    long gridCapacity();
}
