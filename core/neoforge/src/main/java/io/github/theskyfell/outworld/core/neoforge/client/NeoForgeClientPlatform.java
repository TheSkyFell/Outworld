package io.github.theskyfell.outworld.core.neoforge.client;

import io.github.theskyfell.outworld.core.client.ClientPlatform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class NeoForgeClientPlatform implements ClientPlatform {
    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
