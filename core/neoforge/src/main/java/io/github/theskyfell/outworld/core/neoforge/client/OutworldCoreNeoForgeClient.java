package io.github.theskyfell.outworld.core.neoforge.client;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.client.OutworldClientEvents;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Core's client entry point. NeoForge only constructs it on the client. */
@Mod(value = OutworldCore.MOD_ID, dist = Dist.CLIENT)
public final class OutworldCoreNeoForgeClient {
    public OutworldCoreNeoForgeClient() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class,
                event -> OutworldClientEvents.CLIENT_TICK_END.forEach(l -> l.accept(Minecraft.getInstance())));
    }
}
