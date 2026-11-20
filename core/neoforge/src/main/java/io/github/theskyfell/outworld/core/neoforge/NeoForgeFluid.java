package io.github.theskyfell.outworld.core.neoforge;

import io.github.theskyfell.outworld.core.fluid.OutworldFluid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * An {@link OutworldFluid} on NeoForge, where every fluid needs a fluid type. Its type has the same id
 * and is registered by {@link NeoForgeModules} for every fluid an Outworld module registers.
 */
final class NeoForgeFluid extends OutworldFluid {
    private final ResourceKey<Fluid> key;
    private FluidType type;

    NeoForgeFluid(ResourceKey<Fluid> key, int color) {
        super(color);
        this.key = key;
    }

    @Override
    public FluidType getFluidType() {
        if (type == null) {
            type = NeoForgeRegistries.FLUID_TYPES.getValue(key.identifier());
        }
        return type;
    }

    /** The fluid type for an Outworld fluid with this id. */
    static FluidType createType(ResourceKey<Fluid> key) {
        String id = key.identifier().getNamespace() + "." + key.identifier().getPath();
        return new FluidType(FluidType.Properties.create().descriptionId("fluid." + id));
    }
}
