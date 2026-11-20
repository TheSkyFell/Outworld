package io.github.theskyfell.outworld.core.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Handles a payload the server sent. Runs on the client's main thread. */
@FunctionalInterface
public interface ClientPayloadHandler<T extends CustomPacketPayload> {
    void handle(T payload);
}
