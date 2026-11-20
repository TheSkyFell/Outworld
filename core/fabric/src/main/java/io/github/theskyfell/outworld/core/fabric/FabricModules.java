package io.github.theskyfell.outworld.core.fabric;

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
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;

/**
 * Loads Outworld modules on Fabric. A mod's {@code onInitialize} calls {@link #load} with its module;
 * Fabric allows registration right then, so everything is registered immediately.
 */
public final class FabricModules {
    private FabricModules() {
    }

    public static void load(OutworldModule module) {
        Modules.add(module);

        for (Registrar<?> registrar : module.registrars()) {
            register(registrar);
        }

        Payloads payloads = new Payloads();
        module.registerPayloads(payloads);
        for (Payloads.Clientbound<?> payload : payloads.clientbound()) {
            registerClientbound(payload);
        }
        for (Payloads.Serverbound<?> payload : payloads.serverbound()) {
            registerServerbound(payload);
        }

        module.registerDataRegistries(new FabricDataRegistries());
        module.init();
        module.registerTransfers(new LookupTransfers());
    }

    /** Declares data pack registries with Fabric's dynamic registry API. */
    private static final class FabricDataRegistries implements DataRegistries {
        @Override
        public <T> void synced(ResourceKey<Registry<T>> key, Codec<T> codec) {
            DynamicRegistries.registerSynced(key, codec);
        }
    }

    /** Exposes Outworld storages through Fabric's block API lookups. */
    private static final class LookupTransfers implements Transfers {
        @Override
        public <T extends BlockEntity> void energy(BlockEntityType<T> type, Sided<T, EnergyBuffer> energy) {
            EnergyStorage.SIDED.registerForBlockEntity((blockEntity, side) -> {
                EnergyBuffer buffer = energy.get(blockEntity, side);
                return buffer == null ? null : FabricEnergy.storageFor(buffer);
            }, type);
        }

        @Override
        public <T extends BlockEntity> void items(BlockEntityType<T> type, Sided<T, ItemBuffer> items) {
            ItemStorage.SIDED.registerForBlockEntity((blockEntity, side) -> {
                ItemBuffer buffer = items.get(blockEntity, side);
                return buffer == null ? null : FabricItems.storageFor(buffer, side);
            }, type);
        }

        @Override
        public <T extends BlockEntity> void fluids(BlockEntityType<T> type, Sided<T, FluidTank> fluids) {
            FluidStorage.SIDED.registerForBlockEntity((blockEntity, side) -> {
                FluidTank tank = fluids.get(blockEntity, side);
                return tank == null ? null : FabricFluids.storageFor(tank);
            }, type);
        }
    }

    private static <T> void register(Registrar<T> registrar) {
        Registry<T> registry = lookup(registrar.registryKey());
        registrar.registerAll((key, value) -> Registry.register(registry, key, value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Registry<T> lookup(ResourceKey<? extends Registry<T>> key) {
        return (Registry<T>) BuiltInRegistries.REGISTRY.getValueOrThrow((ResourceKey) key);
    }

    private static <T extends CustomPacketPayload> void registerClientbound(Payloads.Clientbound<T> payload) {
        PayloadTypeRegistry.clientboundPlay().register(payload.type(), payload.codec());
    }

    private static <T extends CustomPacketPayload> void registerServerbound(Payloads.Serverbound<T> payload) {
        PayloadTypeRegistry.serverboundPlay().register(payload.type(), payload.codec());
        ServerPlayNetworking.registerGlobalReceiver(payload.type(),
                (received, context) -> payload.handler().handle(received, context.player()));
    }
}
