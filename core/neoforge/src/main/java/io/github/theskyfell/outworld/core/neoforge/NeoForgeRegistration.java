package io.github.theskyfell.outworld.core.neoforge;

import io.github.theskyfell.outworld.core.registry.Registrar;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers common-code {@link Registrar}s on NeoForge, which only allows registration inside each
 * registry's {@link RegisterEvent}.
 */
public final class NeoForgeRegistration {
    private NeoForgeRegistration() {
    }

    /** Call from a mod's constructor with that mod's own event bus. */
    public static void registerAll(IEventBus modBus, List<Registrar<?>> registrars) {
        modBus.addListener(RegisterEvent.class, event -> {
            for (Registrar<?> registrar : registrars) {
                register(event, registrar);
            }
        });
    }

    private static <T> void register(RegisterEvent event, Registrar<T> registrar) {
        // Runs the callback only when this event is for the registrar's registry.
        event.register(registrar.registryKey(), helper -> registrar.registerAll(helper::register));
    }
}
