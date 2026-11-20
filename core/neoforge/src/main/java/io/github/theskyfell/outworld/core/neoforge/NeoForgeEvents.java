package io.github.theskyfell.outworld.core.neoforge;

import io.github.theskyfell.outworld.core.event.OutworldEvents;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Forwards NeoForge's server events to {@link OutworldEvents}. Called once, by Core. */
final class NeoForgeEvents {
    private NeoForgeEvents() {
    }

    static void forward() {
        NeoForge.EVENT_BUS.addListener(ServerStartingEvent.class,
                event -> OutworldEvents.SERVER_STARTING.forEach(l -> l.accept(event.getServer())));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class,
                event -> OutworldEvents.SERVER_STARTED.forEach(l -> l.accept(event.getServer())));
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class,
                event -> OutworldEvents.SERVER_STOPPING.forEach(l -> l.accept(event.getServer())));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class,
                event -> OutworldEvents.SERVER_TICK_END.forEach(l -> l.accept(event.getServer())));
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                OutworldEvents.LEVEL_TICK_END.forEach(l -> l.accept(level));
            }
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                OutworldEvents.REGISTER_COMMANDS.forEach(l -> l.register(event.getDispatcher(), event.getBuildContext(),
                        event.getCommandSelection())));
    }
}
