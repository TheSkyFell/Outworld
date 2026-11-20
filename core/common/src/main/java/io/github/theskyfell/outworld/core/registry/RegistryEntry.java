package io.github.theskyfell.outworld.core.registry;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * One declared registry object. {@link #get()} works once the loader has registered it.
 *
 * @param <T> the registry's type, e.g. {@code Item}
 * @param <R> the object's own type, e.g. {@code BlockItem}
 */
public final class RegistryEntry<T, R extends T> implements Supplier<R> {
    private final ResourceKey<T> key;
    private final Function<ResourceKey<T>, ? extends R> factory;
    private R value;

    RegistryEntry(ResourceKey<T> key, Function<ResourceKey<T>, ? extends R> factory) {
        this.key = key;
        this.factory = factory;
    }

    void register(BiConsumer<ResourceKey<T>, T> sink) {
        R created = factory.apply(key);
        sink.accept(key, created);
        value = created;
    }

    public ResourceKey<T> key() {
        return key;
    }

    public Identifier id() {
        return key.identifier();
    }

    public boolean isRegistered() {
        return value != null;
    }

    @Override
    public R get() {
        R current = value;
        if (current == null) {
            throw new IllegalStateException("Registry entry " + key.identifier() + " was used before it was registered");
        }
        return current;
    }
}
