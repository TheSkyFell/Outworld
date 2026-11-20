package io.github.theskyfell.outworld.core;

import io.github.theskyfell.outworld.core.command.OutworldCommand;
import io.github.theskyfell.outworld.core.config.ConfigFile;
import io.github.theskyfell.outworld.core.event.OutworldEvents;
import io.github.theskyfell.outworld.core.machine.MachineScheduler;
import io.github.theskyfell.outworld.core.module.DataRegistries;
import io.github.theskyfell.outworld.core.module.Modules;
import io.github.theskyfell.outworld.core.module.OutworldModule;
import io.github.theskyfell.outworld.core.multiblock.MultiblockStructure;
import io.github.theskyfell.outworld.core.multiblock.MultiblockTracker;
import io.github.theskyfell.outworld.core.perf.Perf;
import io.github.theskyfell.outworld.core.platform.Services;

/** Outworld Core itself, loaded through the same loader layer as every feature mod. */
public final class CoreModule implements OutworldModule {
    @Override
    public String modId() {
        return OutworldCore.MOD_ID;
    }

    @Override
    public void registerDataRegistries(DataRegistries registries) {
        registries.synced(MultiblockStructure.REGISTRY, MultiblockStructure.CODEC);
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

        OutworldEvents.SERVER_TICK_START.register(server -> Perf.tickStarted());
        OutworldEvents.SERVER_TICK_END.register(server -> Perf.tickEnded());
        OutworldEvents.LEVEL_TICK_END.register(MachineScheduler::tick);
        OutworldEvents.SERVER_STOPPING.register(server -> {
            MachineScheduler.clear();
            MultiblockTracker.clear();
            Perf.clear();
        });
    }
}
