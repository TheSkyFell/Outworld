package io.github.theskyfell.outworld.core.client;

import io.github.theskyfell.outworld.core.platform.Services;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sends payloads from the client to the server. Client only. */
public final class ClientNetwork {
    private static final ClientPlatform PLATFORM = Services.load(ClientPlatform.class);

    private ClientNetwork() {
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PLATFORM.sendToServer(payload);
    }
}
