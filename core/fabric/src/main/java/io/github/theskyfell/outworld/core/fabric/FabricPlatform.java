package io.github.theskyfell.outworld.core.fabric;

import io.github.theskyfell.outworld.core.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricPlatform implements Platform {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
