package io.github.theskyfell.outworld.core.gas;

import io.github.theskyfell.outworld.core.OutworldCore;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** The gases Core defines (in its data), for every Outworld mod to share. */
public final class Gases {
    public static final ResourceKey<Gas> HYDROGEN = Gas.key(Identifier.fromNamespaceAndPath(OutworldCore.MOD_ID, "hydrogen"));
    public static final ResourceKey<Gas> OXYGEN = Gas.key(Identifier.fromNamespaceAndPath(OutworldCore.MOD_ID, "oxygen"));

    /** Gases are measured in millibuckets: a thousand to a bucket. */
    public static final long BUCKET = 1_000;

    private Gases() {
    }
}
