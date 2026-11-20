package io.github.theskyfell.outworld.core.fluid;

/**
 * A block that joins touching blocks like it into a {@link FluidGrids fluid grid} with one shared
 * tank, such as a fluid pipe or a tank block. Implement it on the {@code Block}; it needs no block
 * entity. The grid holds one fluid at a time.
 */
public interface FluidConductor {
    /** What this block adds to its grid's tank, in droplets. */
    long fluidGridCapacity();
}
