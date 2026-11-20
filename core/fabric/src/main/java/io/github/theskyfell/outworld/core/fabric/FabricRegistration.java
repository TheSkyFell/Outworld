package io.github.theskyfell.outworld.core.fabric;

import io.github.theskyfell.outworld.core.registry.Registrar;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

/** Registers common-code {@link Registrar}s on Fabric, which allows registration during mod initialization. */
public final class FabricRegistration {
    private FabricRegistration() {
    }

    /** Registers every entry immediately. Call from a mod's {@code onInitialize}. */
    public static void registerAll(List<Registrar<?>> registrars) {
        for (Registrar<?> registrar : registrars) {
            register(registrar);
        }
    }

    private static <T> void register(Registrar<T> registrar) {
        Registry<T> registry = lookup(registrar.registryKey());
        registrar.registerAll((key, value) -> Registry.register(registry, key, value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Registry<T> lookup(ResourceKey<? extends Registry<T>> key) {
        return (Registry<T>) BuiltInRegistries.REGISTRY.getValueOrThrow((ResourceKey) key);
    }
}
