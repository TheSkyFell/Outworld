package io.github.theskyfell.outworld.core.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-only calls that differ between loaders. Each loader project implements this and names its
 * implementation in {@code META-INF/services/io.github.theskyfell.outworld.core.client.ClientPlatform}.
 * It is only looked up on the client.
 */
public interface ClientPlatform {
    void sendToServer(CustomPacketPayload payload);
}
