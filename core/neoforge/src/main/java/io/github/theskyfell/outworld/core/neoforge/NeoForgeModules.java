package io.github.theskyfell.outworld.core.neoforge;

import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import io.github.theskyfell.outworld.core.module.Modules;
import io.github.theskyfell.outworld.core.module.OutworldModule;
import io.github.theskyfell.outworld.core.module.Transfers;
import io.github.theskyfell.outworld.core.network.Payloads;
import io.github.theskyfell.outworld.core.registry.Registrar;
import java.util.List;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Loads Outworld modules on NeoForge. A mod's constructor calls {@link #load} with its module and its
 * own event bus. NeoForge only allows registration inside its registration events, so this listens
 * for them and registers each module's entries when they fire.
 */
public final class NeoForgeModules {
    private NeoForgeModules() {
    }

    public static void load(OutworldModule module, IEventBus modBus) {
        Modules.add(module);

        List<Registrar<?>> registrars = module.registrars();
        modBus.addListener(RegisterEvent.class, event -> {
            for (Registrar<?> registrar : registrars) {
                register(event, registrar);
            }
        });

        Payloads payloads = new Payloads();
        module.registerPayloads(payloads);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(module.networkVersion());
            for (Payloads.Clientbound<?> payload : payloads.clientbound()) {
                registerClientbound(registrar, payload);
            }
            for (Payloads.Serverbound<?> payload : payloads.serverbound()) {
                registerServerbound(registrar, payload);
            }
        });

        modBus.addListener(RegisterCapabilitiesEvent.class, event -> module.registerTransfers(new CapabilityTransfers(event)));

        module.init();
    }

    /** Exposes Outworld storages as NeoForge capabilities. */
    private record CapabilityTransfers(RegisterCapabilitiesEvent event) implements Transfers {
        @Override
        public <T extends BlockEntity> void energy(BlockEntityType<T> type, SidedEnergy<T> energy) {
            event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (blockEntity, side) -> {
                EnergyBuffer buffer = energy.get(blockEntity, side);
                return buffer == null ? null : NeoForgeEnergy.handlerFor(buffer);
            });
        }
    }

    private static <T> void register(RegisterEvent event, Registrar<T> registrar) {
        // Runs the callback only when this event is for the registrar's registry.
        event.register(registrar.registryKey(), helper -> registrar.registerAll(helper::register));
    }

    private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar, Payloads.Clientbound<T> payload) {
        // The client handler is added by the client module (RegisterClientPayloadHandlersEvent).
        registrar.playToClient(payload.type(), payload.codec());
    }

    private static <T extends CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar, Payloads.Serverbound<T> payload) {
        registrar.playToServer(payload.type(), payload.codec(),
                (received, context) -> payload.handler().handle(received, (ServerPlayer) context.player()));
    }
}
