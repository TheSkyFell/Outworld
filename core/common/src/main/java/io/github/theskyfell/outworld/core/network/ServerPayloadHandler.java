package io.github.theskyfell.outworld.core.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Handles a payload a client sent. Runs on the server thread. */
@FunctionalInterface
public interface ServerPayloadHandler<T extends CustomPacketPayload> {
    void handle(T payload, ServerPlayer sender);
}
