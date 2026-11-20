package io.github.theskyfell.outworld.core.fabric.client;

import io.github.theskyfell.outworld.core.client.BlockEntityRendererRegistrar;
import io.github.theskyfell.outworld.core.client.ClientPayloadHandler;
import io.github.theskyfell.outworld.core.client.ClientPayloadHandlers;
import io.github.theskyfell.outworld.core.client.OutworldClientModule;
import io.github.theskyfell.outworld.core.client.ScreenFactory;
import io.github.theskyfell.outworld.core.client.ScreenRegistrar;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Loads the client part of Outworld modules on Fabric. Called from a mod's {@code onInitializeClient}. */
public final class FabricClientModules {
    private FabricClientModules() {
    }

    public static void load(OutworldClientModule module) {
        module.registerScreens(new Screens());
        module.registerBlockEntityRenderers(new Renderers());
        module.registerPayloadHandlers(new Handlers());
        module.initClient();
    }

    private static final class Screens implements ScreenRegistrar {
        @Override
        public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type,
                ScreenFactory<M, S> factory) {
            MenuScreens.register(type, factory::create);
        }
    }

    private static final class Renderers implements BlockEntityRendererRegistrar {
        @Override
        public <T extends BlockEntity, S extends BlockEntityRenderState> void register(BlockEntityType<? extends T> type,
                BlockEntityRendererProvider<T, S> provider) {
            BlockEntityRenderers.register(type, provider);
        }
    }

    private static final class Handlers implements ClientPayloadHandlers {
        @Override
        public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, ClientPayloadHandler<T> handler) {
            ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.handle(payload));
        }
    }
}
