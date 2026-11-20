package io.github.theskyfell.outworld.core.fabric;

import io.github.theskyfell.outworld.core.CoreModule;
import net.fabricmc.api.ModInitializer;

public final class OutworldCoreFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricEvents.forward();
        FabricModules.load(new CoreModule());
    }
}
