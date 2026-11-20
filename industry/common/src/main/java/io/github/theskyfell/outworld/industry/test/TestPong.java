package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.industry.OutworldIndustry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the reply to a {@link TestPing}. */
public record TestPong(long sentAtMillis, String message) implements CustomPacketPayload {
    public static final Type<TestPong> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "test_pong"));
    public static final StreamCodec<ByteBuf, TestPong> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, TestPong::sentAtMillis,
            ByteBufCodecs.STRING_UTF8, TestPong::message,
            TestPong::new);

    @Override
    public Type<TestPong> type() {
        return TYPE;
    }
}
