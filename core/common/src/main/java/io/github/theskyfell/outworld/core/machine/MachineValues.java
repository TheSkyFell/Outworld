package io.github.theskyfell.outworld.core.machine;

import io.github.theskyfell.outworld.core.OutworldCore;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * What a machine's screen shows besides its slots: energy, progress and tanks. The server sends it to
 * a player only while they have the screen open, and only when something in it changed.
 *
 * @param containerId the open menu this is for
 */
public record MachineValues(int containerId, long energy, long energyCapacity, int progress, int recipeTime, List<Tank> tanks)
        implements CustomPacketPayload {
    public static final Type<MachineValues> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OutworldCore.MOD_ID, "machine_values"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineValues> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachineValues::containerId,
            ByteBufCodecs.VAR_LONG, MachineValues::energy,
            ByteBufCodecs.VAR_LONG, MachineValues::energyCapacity,
            ByteBufCodecs.VAR_INT, MachineValues::progress,
            ByteBufCodecs.VAR_INT, MachineValues::recipeTime,
            Tank.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineValues::tanks,
            MachineValues::new);

    /**
     * One tank as the screen shows it.
     *
     * @param gas true for a gas tank, false for a fluid tank
     * @param contents the fluid's or gas's id, or "" when empty
     * @param amount in millibuckets
     * @param capacity in millibuckets
     * @param color 0xRRGGBB
     */
    public record Tank(boolean gas, String contents, long amount, long capacity, int color) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Tank> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Tank::gas,
                ByteBufCodecs.STRING_UTF8, Tank::contents,
                ByteBufCodecs.VAR_LONG, Tank::amount,
                ByteBufCodecs.VAR_LONG, Tank::capacity,
                ByteBufCodecs.INT, Tank::color,
                Tank::new);
    }

    public static final MachineValues EMPTY = new MachineValues(-1, 0, 0, 0, 0, List.of());

    @Override
    public Type<MachineValues> type() {
        return TYPE;
    }
}
