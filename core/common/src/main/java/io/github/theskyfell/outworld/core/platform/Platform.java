package io.github.theskyfell.outworld.core.platform;

/**
 * The few things common code needs to ask the mod loader. Each loader project implements this and
 * names its implementation in {@code META-INF/services/io.github.theskyfell.outworld.core.platform.Platform}.
 *
 * <p>Keep this small. Gameplay logic never belongs here; only facts and hooks that genuinely differ
 * between loaders do.
 */
public interface Platform {
    /** "Fabric" or "NeoForge". */
    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();
}
