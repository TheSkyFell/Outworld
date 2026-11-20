package io.github.theskyfell.outworld.core.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;

/**
 * An amount of one gas, in millibuckets. The empty stack has no gas and an amount of 0.
 *
 * @param gas the gas, or null when empty
 */
public record GasStack(ResourceKey<Gas> gas, long amount) {
    public static final GasStack EMPTY = new GasStack(null, 0);

    public static final Codec<GasStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Gas.REGISTRY).optionalFieldOf("gas").forGetter(stack -> Optional.ofNullable(stack.gas())),
            Codec.LONG.fieldOf("amount").forGetter(GasStack::amount)
    ).apply(instance, (gas, amount) -> of(gas.orElse(null), amount)));

    /** A stack, or {@link #EMPTY} if there is no gas or nothing of it. */
    public static GasStack of(ResourceKey<Gas> gas, long amount) {
        return gas == null || amount <= 0 ? EMPTY : new GasStack(gas, amount);
    }

    public boolean isEmpty() {
        return gas == null || amount <= 0;
    }

    public boolean is(ResourceKey<Gas> other) {
        return gas != null && gas.equals(other);
    }

    public GasStack withAmount(long newAmount) {
        return of(gas, newAmount);
    }
}
