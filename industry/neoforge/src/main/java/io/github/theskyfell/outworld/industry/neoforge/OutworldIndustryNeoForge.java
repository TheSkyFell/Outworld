package io.github.theskyfell.outworld.industry.neoforge;

import io.github.theskyfell.outworld.core.neoforge.NeoForgeModules;
import io.github.theskyfell.outworld.industry.IndustryModule;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(OutworldIndustry.MOD_ID)
public final class OutworldIndustryNeoForge {
    public OutworldIndustryNeoForge(IEventBus modBus) {
        NeoForgeModules.load(new IndustryModule(), modBus);
    }
}
