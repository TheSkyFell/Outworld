package io.github.theskyfell.outworld.core;

import io.github.theskyfell.outworld.core.command.OutworldCommand;
import io.github.theskyfell.outworld.core.config.ConfigFile;
import io.github.theskyfell.outworld.core.event.OutworldEvents;
import io.github.theskyfell.outworld.core.module.Modules;
import io.github.theskyfell.outworld.core.module.OutworldModule;
import io.github.theskyfell.outworld.core.platform.Services;

/** Outworld Core itself, loaded through the same loader layer as every feature mod. */
public final class CoreModule implements OutworldModule {
    @Override
    public String modId() {
        return OutworldCore.MOD_ID;
    }

    @Override
    public void init() {
        OutworldCore.LOG.info("Outworld Core starting on {} ({} environment)",
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.isDevelopmentEnvironment() ? "development" : "production");

        ConfigFile<CoreConfig> config = ConfigFile.load(OutworldCore.MOD_ID, "core", CoreConfig.class);
        config.get().validate();
        OutworldCore.setConfig(config.get());

        OutworldEvents.REGISTER_COMMANDS.register((dispatcher, context, selection) -> OutworldCommand.register(dispatcher));
        OutworldEvents.SERVER_STARTED.register(server -> OutworldCore.LOG.info("Outworld is ready: {}", Modules.describe()));
    }
}
