package io.github.theskyfell.outworld.core.neoforge.client;

import io.github.theskyfell.outworld.core.client.ClientPayloadHandler;
import io.github.theskyfell.outworld.core.client.ClientPayloadHandlers;
import io.github.theskyfell.outworld.core.client.OutworldClientModule;
import io.github.theskyfell.outworld.core.client.ScreenFactory;
import io.github.theskyfell.outworld.core.client.ScreenRegistrar;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Loads the client part of Outworld modules on NeoForge. Called from a mod's client-only constructor
 * ({@code @Mod(value = ..., dist = Dist.CLIENT)}) with its own event bus.
 */
public final class NeoForgeClientModules {
    private NeoForgeClientModules() {
    }

    public static void load(OutworldClientModule module, IEventBus modBus) {
        modBus.addListener(RegisterMenuScreensEvent.class, event -> module.registerScreens(new Screens(event)));
        modBus.addListener(RegisterClientPayloadHandlersEvent.class, event -> module.registerPayloadHandlers(new Handlers(event)));
        module.initClient();
    }

    private record Screens(RegisterMenuScreensEvent event) implements ScreenRegistrar {
        @Override
        public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type,
                ScreenFactory<M, S> factory) {
            event.register(type, factory::create);
        }
    }

    private record Handlers(RegisterClientPayloadHandlersEvent event) implements ClientPayloadHandlers {
        @Override
        public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, ClientPayloadHandler<T> handler) {
            event.register(type, (payload, context) -> handler.handle(payload));
        }
    }
}
