package io.github.theskyfell.outworld.core.neoforge;

import com.mojang.serialization.Codec;
import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import io.github.theskyfell.outworld.core.fluid.FluidTank;
import io.github.theskyfell.outworld.core.item.ItemBuffer;
import io.github.theskyfell.outworld.core.module.DataRegistries;
import io.github.theskyfell.outworld.core.module.Modules;
import io.github.theskyfell.outworld.core.module.OutworldModule;
import io.github.theskyfell.outworld.core.module.Transfers;
import io.github.theskyfell.outworld.core.network.Payloads;
import io.github.theskyfell.outworld.core.registry.Registrar;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;
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
            // Every fluid needs a fluid type on NeoForge: Outworld's fluids get one with the same id.
            event.register(NeoForgeRegistries.Keys.FLUID_TYPES, helper -> {
                for (Registrar<?> registrar : registrars) {
                    if (registrar.registryKey().equals(Registries.FLUID)) {
                        for (var entry : registrar.entries()) {
                            ResourceKey<Fluid> key = ResourceKey.create(Registries.FLUID, entry.id());
                            helper.register(entry.id(), NeoForgeFluid.createType(key));
                        }
                    }
                }
            });
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

        modBus.addListener(NewDatapackRegistryEvent.class, event -> module.registerDataRegistries(new EventDataRegistries(event)));

        modBus.addListener(RegisterCapabilitiesEvent.class, event -> module.registerTransfers(new CapabilityTransfers(event)));

        module.init();
    }

    /** Declares data pack registries through NeoForge's registry event. */
    private record EventDataRegistries(NewDatapackRegistryEvent event) implements DataRegistries {
        @Override
        public <T> void synced(ResourceKey<Registry<T>> key, Codec<T> codec) {
            event.worldRegistry(key, codec, codec);
        }
    }

    /** Exposes Outworld storages as NeoForge capabilities. */
    private record CapabilityTransfers(RegisterCapabilitiesEvent event) implements Transfers {
        @Override
        public <T extends BlockEntity> void energy(BlockEntityType<T> type, Sided<T, EnergyBuffer> energy) {
            event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (blockEntity, side) -> {
                EnergyBuffer buffer = energy.get(blockEntity, side);
                return buffer == null ? null : NeoForgeEnergy.handlerFor(buffer);
            });
        }

        @Override
        public void blockEnergy(AtBlock<EnergyBuffer> energy, Block... blocks) {
            event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, blockEntity, side) -> {
                EnergyBuffer buffer = level instanceof ServerLevel serverLevel ? energy.get(serverLevel, pos, side) : null;
                return buffer == null ? null : NeoForgeEnergy.handlerFor(buffer);
            }, blocks);
        }

        @Override
        public void blockFluids(AtBlock<FluidTank> fluids, Block... blocks) {
            event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, blockEntity, side) -> {
                FluidTank tank = level instanceof ServerLevel serverLevel ? fluids.get(serverLevel, pos, side) : null;
                return tank == null ? null : NeoForgeFluids.handlerFor(tank);
            }, blocks);
        }

        @Override
        public <T extends BlockEntity> void items(BlockEntityType<T> type, Sided<T, ItemBuffer> items) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, type, (blockEntity, side) -> {
                ItemBuffer buffer = items.get(blockEntity, side);
                return buffer == null ? null : NeoForgeItems.handlerFor(buffer, side);
            });
        }

        @Override
        public <T extends BlockEntity> void fluids(BlockEntityType<T> type, Sided<T, FluidTank> fluids) {
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, (blockEntity, side) -> {
                FluidTank tank = fluids.get(blockEntity, side);
                return tank == null ? null : NeoForgeFluids.handlerFor(tank);
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
