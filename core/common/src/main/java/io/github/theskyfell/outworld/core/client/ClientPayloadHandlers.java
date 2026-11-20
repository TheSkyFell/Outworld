package io.github.theskyfell.outworld.core.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Adds client handlers for clientbound payloads. Given to {@link OutworldClientModule#registerPayloadHandlers}. */
public interface ClientPayloadHandlers {
    <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, ClientPayloadHandler<T> handler);
}
