package io.github.theskyfell.outworld.core.registry;

import io.github.theskyfell.outworld.core.OutworldCore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Collects one mod's entries for one registry, in common code.
 *
 * <p>Declaring an entry creates nothing. The loader layer calls {@link #registerAll} at the moment its
 * loader allows registration (Fabric: during mod initialization; NeoForge: in the matching
 * {@code RegisterEvent}). Each factory receives the entry's key, because objects must carry their id
 * from the moment they are constructed ({@code Properties.setId}).
 *
 * @param <T> the registry's type, e.g. {@code Block}
 */
public final class Registrar<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;
    private final List<RegistryEntry<T, ? extends T>> entries = new ArrayList<>();
    private boolean registered;

    private Registrar(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new Registrar<>(registryKey, namespace);
    }

    /** Declares an entry. The factory runs later, when the loader registers this registrar. */
    public <R extends T> RegistryEntry<T, R> register(String path, Function<ResourceKey<T>, ? extends R> factory) {
        if (registered) {
            throw new IllegalStateException("Cannot add " + namespace + ":" + path + " to "
                    + registryKey.identifier() + " after registration has run");
        }
        ResourceKey<T> key = ResourceKey.create(registryKey, Identifier.fromNamespaceAndPath(namespace, path));
        RegistryEntry<T, R> entry = new RegistryEntry<>(key, factory);
        entries.add(entry);
        return entry;
    }

    public ResourceKey<? extends Registry<T>> registryKey() {
        return registryKey;
    }

    public String namespace() {
        return namespace;
    }

    /** Every entry declared so far, in order. */
    public List<RegistryEntry<T, ? extends T>> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Creates every entry and hands it to {@code sink}, in the order they were declared. Only the
     * loader layer calls this, exactly once.
     */
    public void registerAll(BiConsumer<ResourceKey<T>, T> sink) {
        if (registered) {
            throw new IllegalStateException(namespace + " entries for " + registryKey.identifier()
                    + " were already registered");
        }
        registered = true;
        for (RegistryEntry<T, ? extends T> entry : entries) {
            entry.register(sink);
        }
        OutworldCore.LOG.info("Registered {} entries in {} for {}", entries.size(), registryKey.identifier(), namespace);
    }
}
