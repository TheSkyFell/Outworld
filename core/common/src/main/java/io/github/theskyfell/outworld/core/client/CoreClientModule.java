package io.github.theskyfell.outworld.core.client;

import io.github.theskyfell.outworld.core.CoreContent;
import io.github.theskyfell.outworld.core.machine.MachineValues;
import io.github.theskyfell.outworld.core.multiblock.MultiblockControllerBlock;

/** Core's own client part: the machine screen and the multiblock build preview. */
public final class CoreClientModule implements OutworldClientModule {
    @Override
    public void registerScreens(ScreenRegistrar screens) {
        screens.register(CoreContent.MACHINE_MENU.get(), MachineScreen::new);
    }

    @Override
    public void registerPayloadHandlers(ClientPayloadHandlers handlers) {
        handlers.register(MachineValues.TYPE, MachineScreen::onValues);
    }

    @Override
    public void initClient() {
        MultiblockControllerBlock.setPreviewToggle(MultiblockPreview::toggle);
        OutworldClientEvents.CLIENT_TICK_END.register(MultiblockPreview::tick);
    }
}
