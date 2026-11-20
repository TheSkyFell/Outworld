package io.github.theskyfell.outworld.core.module;

import io.github.theskyfell.outworld.core.network.Payloads;
import io.github.theskyfell.outworld.core.registry.Registrar;
import java.util.List;

/**
 * One Outworld mod, as seen from common code. Each mod provides one of these, and its loader entry
 * point hands it to Core's loader layer, which registers everything at the right moment for that
 * loader.
 *
 * <p>Client-only parts (screens, client payload handlers) go in an
 * {@link io.github.theskyfell.outworld.core.client.OutworldClientModule} instead, so dedicated servers
 * never load client classes.
 */
public interface OutworldModule {
    String modId();

    /** Everything the mod registers, in order. Blocks before items, since block items need their block. */
    default List<Registrar<?>> registrars() {
        return List.of();
    }

    /** Declares the mod's network payloads. Client handlers are added by the client module. */
    default void registerPayloads(Payloads payloads) {
    }

    /**
     * The mod's network protocol version. A client and server must agree on it to connect, so bump it
     * whenever a payload's format changes.
     */
    default String networkVersion() {
        return "1";
    }

    /** Runs once at startup, on both sides, after registration has been set up. Register event listeners here. */
    default void init() {
    }
}
