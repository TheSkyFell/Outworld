package io.github.theskyfell.outworld.core.fabric.client;

import io.github.theskyfell.outworld.core.client.ClientPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FabricClientPlatform implements ClientPlatform {
    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
