package io.github.theskyfell.outworld.industry;

import io.github.theskyfell.outworld.core.energy.EnergyGrids;
import io.github.theskyfell.outworld.core.fluid.FluidGrids;
import io.github.theskyfell.outworld.core.module.OutworldModule;
import io.github.theskyfell.outworld.core.module.Transfers;
import io.github.theskyfell.outworld.core.network.Payloads;
import io.github.theskyfell.outworld.core.platform.Services;
import io.github.theskyfell.outworld.core.registry.Registrar;
import io.github.theskyfell.outworld.core.stress.StressScenarios;
import io.github.theskyfell.outworld.industry.test.MachinesStressScenario;
import io.github.theskyfell.outworld.industry.test.TestMachine;
import io.github.theskyfell.outworld.industry.test.TestPing;
import io.github.theskyfell.outworld.industry.test.TestPong;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;

/** Outworld Industry, as handed to Core's loader layer. */
public final class IndustryModule implements OutworldModule {
    @Override
    public String modId() {
        return OutworldIndustry.MOD_ID;
    }

    @Override
    public List<Registrar<?>> registrars() {
        return IndustryContent.registrars();
    }

    @Override
    public void registerPayloads(Payloads payloads) {
        payloads.serverbound(TestPing.TYPE, TestPing.CODEC, TestMachine::handlePing)
                .clientbound(TestPong.TYPE, TestPong.CODEC);
    }

    @Override
    public void registerTransfers(Transfers transfers) {
        transfers.energy(IndustryContent.TEST_MACHINE.get(), (machine, side) -> machine.energy());
        transfers.energy(IndustryContent.TEST_BATTERY.get(), (battery, side) -> battery.energy());
        transfers.items(IndustryContent.TEST_CRATE.get(), (crate, side) -> crate.items());
        transfers.energy(IndustryContent.CRUSHER.get(), (crusher, side) -> crusher.energy());
        transfers.items(IndustryContent.CRUSHER.get(), (crusher, side) -> crusher.items());
        transfers.energy(IndustryContent.ELECTRIC_FURNACE.get(), (furnace, side) -> furnace.energy());
        transfers.items(IndustryContent.ELECTRIC_FURNACE.get(), (furnace, side) -> furnace.items());
        transfers.energy(IndustryContent.ORE_WASHER.get(), (washer, side) -> washer.energy());
        transfers.items(IndustryContent.ORE_WASHER.get(), (washer, side) -> washer.items());
        transfers.fluids(IndustryContent.ORE_WASHER.get(), (washer, side) -> washer.exposedFluidTank(side));
        transfers.energy(IndustryContent.ELECTROLYZER.get(), (electrolyzer, side) -> electrolyzer.energy());
        transfers.fluids(IndustryContent.ELECTROLYZER.get(), (electrolyzer, side) -> electrolyzer.exposedFluidTank(side));
        transfers.energy(IndustryContent.GAS_LIQUEFIER.get(), (liquefier, side) -> liquefier.energy());
        transfers.fluids(IndustryContent.GAS_LIQUEFIER.get(), (liquefier, side) -> liquefier.exposedFluidTank(side));
        transfers.energy(IndustryContent.COMBUSTION_GENERATOR.get(), (generator, side) -> generator.energy());
        transfers.items(IndustryContent.COMBUSTION_GENERATOR.get(), (generator, side) -> generator.items());
        transfers.fluids(IndustryContent.COMBUSTION_GENERATOR.get(), (generator, side) -> generator.exposedFluidTank(side));
        transfers.items(IndustryContent.BOILER.get(), (boiler, side) -> boiler.items());
        transfers.fluids(IndustryContent.BOILER.get(), (boiler, side) -> boiler.exposedFluidTank(side));
        transfers.energy(IndustryContent.STEAM_TURBINE.get(), (turbine, side) -> turbine.energy());
        transfers.energy(IndustryContent.SOLAR_PANEL.get(), (panel, side) -> panel.energy());
        transfers.blockEnergy((level, pos, side) -> EnergyGrids.bufferAt(level, pos),
                IndustryContent.CABLE_BLOCK.get(), IndustryContent.BATTERY_BLOCK.get());
        transfers.blockFluids((level, pos, side) -> FluidGrids.tankAt(level, pos),
                IndustryContent.FLUID_PIPE_BLOCK.get(), IndustryContent.FLUID_TANK_BLOCK.get());
        // Fluid only enters a test tank from below, so a pipe on top doesn't hand it back what it pushed up.
        transfers.fluids(IndustryContent.TEST_TANK.get(), (tank, side) -> side == null || side == Direction.DOWN ? tank.tank() : null);
    }

    @Override
    public void init() {
        OutworldIndustry.LOG.info("Outworld Industry starting on {}", Services.PLATFORM.getPlatformName());
        StressScenarios.register("machines", new MachinesStressScenario());
        // Ores in every overworld biome (NeoForge reads data/outworld_industry/neoforge/biome_modifier).
        for (String ore : List.of("ore_bauxite", "ore_titanium")) {
            Services.PLATFORM.addOverworldFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                    ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, ore)));
        }
    }
}
