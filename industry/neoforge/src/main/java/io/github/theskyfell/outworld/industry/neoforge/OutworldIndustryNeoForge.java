package io.github.theskyfell.outworld.industry.neoforge;

import io.github.theskyfell.outworld.core.neoforge.NeoForgeRegistration;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(OutworldIndustry.MOD_ID)
public final class OutworldIndustryNeoForge {
    public OutworldIndustryNeoForge(IEventBus modBus) {
        NeoForgeRegistration.registerAll(modBus, OutworldIndustry.registrars());
        OutworldIndustry.init();
    }
}
