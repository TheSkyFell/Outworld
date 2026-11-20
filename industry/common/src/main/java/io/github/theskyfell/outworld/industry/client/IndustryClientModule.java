package io.github.theskyfell.outworld.industry.client;

import io.github.theskyfell.outworld.core.client.BlockEntityRendererRegistrar;
import io.github.theskyfell.outworld.core.client.ClientPayloadHandlers;
import io.github.theskyfell.outworld.core.client.OutworldClientModule;
import io.github.theskyfell.outworld.core.client.ScreenRegistrar;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.test.TestPong;

/** The client part of Outworld Industry. */
public final class IndustryClientModule implements OutworldClientModule {
    @Override
    public void registerScreens(ScreenRegistrar screens) {
        screens.register(IndustryContent.TEST_MACHINE_MENU.get(), TestMachineScreen::new);
    }

    @Override
    public void registerBlockEntityRenderers(BlockEntityRendererRegistrar renderers) {
        renderers.register(IndustryContent.BELT.get(), BeltRenderer::new);
    }

    @Override
    public void registerPayloadHandlers(ClientPayloadHandlers handlers) {
        handlers.register(TestPong.TYPE, TestMachineScreen::onPong);
    }
}
