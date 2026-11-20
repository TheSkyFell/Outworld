package io.github.theskyfell.outworld.industry.pipe;

import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import io.github.theskyfell.outworld.core.fluid.FluidAmount;
import io.github.theskyfell.outworld.core.fluid.FluidTank;
import io.github.theskyfell.outworld.core.fluid.FluidUnits;
import io.github.theskyfell.outworld.core.gas.Gas;
import io.github.theskyfell.outworld.core.gas.GasStack;
import io.github.theskyfell.outworld.core.gas.GasTank;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;

/** What a grid holds, in words, for players who right-click a cable, pipe or tank block. */
public final class GridInfo {
    private GridInfo() {
    }

    public static Component energy(EnergyBuffer buffer) {
        if (buffer == null) {
            return Component.translatable("message.outworld_industry.grid.none");
        }
        return Component.translatable("message.outworld_industry.grid.energy",
                String.format("%,d", buffer.getAmount()), String.format("%,d", buffer.getCapacity()));
    }

    public static Component fluid(FluidTank tank) {
        if (tank == null) {
            return Component.translatable("message.outworld_industry.grid.none");
        }
        FluidAmount contents = tank.getContents();
        String capacity = String.format("%,d", tank.getCapacity() / FluidUnits.MILLIBUCKET);
        if (contents.isEmpty()) {
            return Component.translatable("message.outworld_industry.grid.empty", capacity);
        }
        // A placeable fluid is named after its block; anything else by its id.
        var block = contents.fluid().defaultFluidState().createLegacyBlock().getBlock();
        Component name = block == Blocks.AIR
                ? Component.literal(BuiltInRegistries.FLUID.getKey(contents.fluid()).toString())
                : block.getName();
        return Component.translatable("message.outworld_industry.grid.contents", name,
                String.format("%,d", contents.amount() / FluidUnits.MILLIBUCKET), capacity);
    }

    public static Component gas(GasTank tank) {
        if (tank == null) {
            return Component.translatable("message.outworld_industry.grid.none");
        }
        GasStack contents = tank.getContents();
        String capacity = String.format("%,d", tank.getCapacity());
        if (contents.isEmpty()) {
            return Component.translatable("message.outworld_industry.grid.empty", capacity);
        }
        Component name = Gas.name(contents.gas());
        return Component.translatable("message.outworld_industry.grid.contents", name,
                String.format("%,d", contents.amount()), capacity);
    }
}
