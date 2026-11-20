package io.github.theskyfell.outworld.core.fabric;

import io.github.theskyfell.outworld.core.event.OutworldEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Forwards Fabric's server events to {@link OutworldEvents}. Called once, by Core. */
final class FabricEvents {
    private FabricEvents() {
    }

    static void forward() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> OutworldEvents.SERVER_STARTING.forEach(l -> l.accept(server)));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> OutworldEvents.SERVER_STARTED.forEach(l -> l.accept(server)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> OutworldEvents.SERVER_STOPPING.forEach(l -> l.accept(server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> OutworldEvents.SERVER_TICK_END.forEach(l -> l.accept(server)));
        ServerTickEvents.END_LEVEL_TICK.register(level -> OutworldEvents.LEVEL_TICK_END.forEach(l -> l.accept(level)));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
                OutworldEvents.REGISTER_COMMANDS.forEach(l -> l.register(dispatcher, context, selection)));
    }
}
