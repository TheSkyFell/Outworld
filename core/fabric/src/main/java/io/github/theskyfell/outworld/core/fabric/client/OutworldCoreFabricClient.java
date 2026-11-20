package io.github.theskyfell.outworld.core.fabric.client;

import io.github.theskyfell.outworld.core.client.OutworldClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class OutworldCoreFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> OutworldClientEvents.CLIENT_TICK_END.forEach(l -> l.accept(client)));
    }
}
