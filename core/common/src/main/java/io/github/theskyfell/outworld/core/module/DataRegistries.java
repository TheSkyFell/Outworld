package io.github.theskyfell.outworld.core.module;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Declares registries whose entries come from data packs. Given to
 * {@link OutworldModule#registerDataRegistries}.
 */
public interface DataRegistries {
    /**
     * A registry loaded from {@code data/<namespace>/<registry namespace>/<registry path>/*.json} when a
     * world loads, and sent to every client that joins. Its entries don't change with {@code /reload};
     * they change when the world is loaded again.
     */
    <T> void synced(ResourceKey<Registry<T>> key, Codec<T> codec);
}
