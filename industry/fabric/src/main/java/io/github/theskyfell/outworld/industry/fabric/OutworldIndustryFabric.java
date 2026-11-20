package io.github.theskyfell.outworld.industry.fabric;

import io.github.theskyfell.outworld.core.fabric.FabricRegistration;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.fabricmc.api.ModInitializer;

public final class OutworldIndustryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricRegistration.registerAll(OutworldIndustry.registrars());
        OutworldIndustry.init();
    }
}
