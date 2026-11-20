package io.github.theskyfell.outworld.industry.fabric.client;

import io.github.theskyfell.outworld.core.fabric.client.FabricClientModules;
import io.github.theskyfell.outworld.industry.client.IndustryClientModule;
import net.fabricmc.api.ClientModInitializer;

public final class OutworldIndustryFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientModules.load(new IndustryClientModule());
    }
}
