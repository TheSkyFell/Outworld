package io.github.theskyfell.outworld.core.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Collects one mod's network payloads in common code. The loader layer registers them with the loader.
 *
 * <p>Server handlers are given here. Client handlers are registered separately by the mod's client
 * module, so a dedicated server never loads client code. Every clientbound payload needs one.
 */
public final class Payloads {
    private final List<Clientbound<?>> clientbound = new ArrayList<>();
    private final List<Serverbound<?>> serverbound = new ArrayList<>();

    /** A payload the server sends to clients. */
    public <T extends CustomPacketPayload> Payloads clientbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        clientbound.add(new Clientbound<>(type, codec));
        return this;
    }

    /**
     * A payload clients send to the server. The handler runs on the server thread. It must check
     * everything the payload claims: a client can send anything.
     */
    public <T extends CustomPacketPayload> Payloads serverbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, ServerPayloadHandler<T> handler) {
        serverbound.add(new Serverbound<>(type, codec, handler));
        return this;
    }

    public List<Clientbound<?>> clientbound() {
        return List.copyOf(clientbound);
    }

    public List<Serverbound<?>> serverbound() {
        return List.copyOf(serverbound);
    }

    public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
    }

    public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, ServerPayloadHandler<T> handler) {
    }
}
