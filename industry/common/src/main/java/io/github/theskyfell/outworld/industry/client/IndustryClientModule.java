package io.github.theskyfell.outworld.industry.client;

import io.github.theskyfell.outworld.core.client.BlockEntityRendererRegistrar;
import net.minecraft.client.Minecraft;
import io.github.theskyfell.outworld.core.client.ClientPayloadHandlers;
import io.github.theskyfell.outworld.core.client.OutworldClientModule;
import io.github.theskyfell.outworld.core.client.ScreenRegistrar;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.machine.AssemblerMenu;
import io.github.theskyfell.outworld.industry.test.TestPong;

/** The client part of Outworld Industry. */
public final class IndustryClientModule implements OutworldClientModule {
    @Override
    public void registerScreens(ScreenRegistrar screens) {
        screens.register(IndustryContent.ASSEMBLER_MENU.get(), AssemblerScreen::new);
        screens.register(IndustryContent.TEST_MACHINE_MENU.get(), TestMachineScreen::new);
    }

    @Override
    public void registerBlockEntityRenderers(BlockEntityRendererRegistrar renderers) {
        renderers.register(IndustryContent.BELT.get(), BeltRenderer::new);
        renderers.register(IndustryContent.INSERTER.get(), InserterRenderer::new);
    }

    @Override
    public void registerPayloadHandlers(ClientPayloadHandlers handlers) {
        handlers.register(AssemblerMenu.RecipeList.TYPE, list -> AssemblerMenu.onRecipeList(list, Minecraft.getInstance().player));
        handlers.register(AssemblerMenu.Chosen.TYPE, chosen -> AssemblerMenu.onChosen(chosen, Minecraft.getInstance().player));
        handlers.register(TestPong.TYPE, TestMachineScreen::onPong);
    }
}
