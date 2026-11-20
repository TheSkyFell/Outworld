package io.github.theskyfell.outworld.core.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * An amount of one fluid, in droplets (see {@link FluidUnits}). The components carry extra data some
 * mods attach to fluids, such as a potion type; most fluids have none.
 */
public record FluidAmount(Fluid fluid, DataComponentPatch components, long amount) {
    public static final FluidAmount EMPTY = new FluidAmount(Fluids.EMPTY, DataComponentPatch.EMPTY, 0);

    public static final Codec<FluidAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidAmount::fluid),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(FluidAmount::components),
            Codec.LONG.fieldOf("amount").forGetter(FluidAmount::amount)
    ).apply(instance, FluidAmount::new));

    public boolean isEmpty() {
        return amount <= 0 || fluid == Fluids.EMPTY;
    }

    /** True if both are the same fluid with the same components, whatever the amounts. */
    public boolean isSameFluid(Fluid otherFluid, DataComponentPatch otherComponents) {
        return fluid == otherFluid && components.equals(otherComponents);
    }
}
