package io.github.theskyfell.outworld.core.client;

import io.github.theskyfell.outworld.core.multiblock.MultiblockControllerBlock;

/** Core's own client-side setup. Called once by each loader's client entry point. */
public final class CoreClient {
    private CoreClient() {
    }

    public static void init() {
        MultiblockControllerBlock.setPreviewToggle(MultiblockPreview::toggle);
        OutworldClientEvents.CLIENT_TICK_END.register(MultiblockPreview::tick);
    }
}
