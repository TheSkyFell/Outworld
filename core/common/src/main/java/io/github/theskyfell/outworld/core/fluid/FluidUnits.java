package io.github.theskyfell.outworld.core.fluid;

/**
 * Outworld measures fluids in droplets: 81,000 per bucket, the same unit as Fabric. NeoForge counts in
 * millibuckets (1,000 per bucket); one millibucket is exactly 81 droplets, so converting is lossless
 * in that direction. Amounts smaller than a millibucket, which only Fabric mods can produce, are
 * invisible to NeoForge mods.
 */
public final class FluidUnits {
    public static final long BUCKET = 81_000;
    public static final long MILLIBUCKET = 81;
    /** A glass bottle: a third of a bucket. */
    public static final long BOTTLE = 27_000;

    private FluidUnits() {
    }
}
