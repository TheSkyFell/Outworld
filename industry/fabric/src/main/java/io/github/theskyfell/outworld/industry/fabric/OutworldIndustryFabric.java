package io.github.theskyfell.outworld.industry.fabric;

import io.github.theskyfell.outworld.core.fabric.FabricModules;
import io.github.theskyfell.outworld.industry.IndustryModule;
import net.fabricmc.api.ModInitializer;

public final class OutworldIndustryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricModules.load(new IndustryModule());
    }
}
