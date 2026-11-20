package io.github.theskyfell.outworld.core.client;

/**
 * The client-only part of an Outworld mod: screens, block entity renderers and client payload handlers. Its loader entry point
 * only runs on the client, so the classes it refers to never load on a dedicated server.
 */
public interface OutworldClientModule {
    /** Connects each menu type to the screen that shows it. */
    default void registerScreens(ScreenRegistrar screens) {
    }

    /**
     * Connects block entity types to the renderers that draw them in the world (items on a belt, say).
     * A renderer's {@code extractRenderState} and {@code submit} should catch their own exceptions:
     * Minecraft turns one thrown there into a crash.
     */
    default void registerBlockEntityRenderers(BlockEntityRendererRegistrar renderers) {
    }

    /** Adds a handler for each of the mod's clientbound payloads. */
    default void registerPayloadHandlers(ClientPayloadHandlers handlers) {
    }

    /** Runs once at client startup. Register client event listeners here. */
    default void initClient() {
    }
}
