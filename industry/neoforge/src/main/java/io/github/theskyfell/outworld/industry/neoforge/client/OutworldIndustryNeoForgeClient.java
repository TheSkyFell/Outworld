package io.github.theskyfell.outworld.industry.neoforge.client;

import io.github.theskyfell.outworld.core.neoforge.client.NeoForgeClientModules;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import io.github.theskyfell.outworld.industry.client.IndustryClientModule;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Industry's client entry point. NeoForge only constructs it on the client. */
@Mod(value = OutworldIndustry.MOD_ID, dist = Dist.CLIENT)
public final class OutworldIndustryNeoForgeClient {
    public OutworldIndustryNeoForgeClient(IEventBus modBus) {
        NeoForgeClientModules.load(new IndustryClientModule(), modBus);
    }
}
