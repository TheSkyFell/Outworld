package io.github.theskyfell.outworld.core.network;

import io.github.theskyfell.outworld.core.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends payloads from the server. To send from the client, use
 * {@link io.github.theskyfell.outworld.core.client.ClientNetwork}.
 *
 * <p>Send only when something a player can see has changed, and only to the players who can see it.
 */
public final class Network {
    private Network() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        Services.PLATFORM.sendToPlayer(player, payload);
    }

    /** Sends to every player who has the chunk containing {@code pos} loaded. */
    public static void sendToPlayersTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        Services.PLATFORM.sendToPlayersTracking(level, pos, payload);
    }
}
