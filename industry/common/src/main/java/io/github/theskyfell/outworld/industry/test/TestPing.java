package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.industry.OutworldIndustry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: "ping" from the test machine screen at {@code pos}. */
public record TestPing(BlockPos pos, long sentAtMillis) implements CustomPacketPayload {
    public static final Type<TestPing> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "test_ping"));
    public static final StreamCodec<ByteBuf, TestPing> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TestPing::pos,
            ByteBufCodecs.VAR_LONG, TestPing::sentAtMillis,
            TestPing::new);

    @Override
    public Type<TestPing> type() {
        return TYPE;
    }
}
