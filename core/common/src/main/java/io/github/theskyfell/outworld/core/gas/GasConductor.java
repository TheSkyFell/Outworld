package io.github.theskyfell.outworld.core.gas;

/**
 * A block that joins touching blocks like it into a {@link GasGrids gas grid} with one shared tank,
 * such as a gas pipe or a gas tank block. Implement it on the {@code Block}; it needs no block entity.
 * The grid holds one gas at a time.
 */
public interface GasConductor {
    /** What this block adds to its grid's tank, in millibuckets. */
    long gasGridCapacity();
}
