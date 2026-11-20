package io.github.theskyfell.outworld.core.neoforge;

import io.github.theskyfell.outworld.core.OutworldCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(OutworldCore.MOD_ID)
public final class OutworldCoreNeoForge {
    public OutworldCoreNeoForge(IEventBus modBus) {
        OutworldCore.init();
    }
}
