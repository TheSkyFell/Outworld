package io.github.theskyfell.outworld.core.neoforge;

import io.github.theskyfell.outworld.core.energy.EnergyAccess;
import io.github.theskyfell.outworld.core.menu.MenuFactory;
import io.github.theskyfell.outworld.core.platform.Platform;
import io.github.theskyfell.outworld.core.registry.BlockEntityTypes;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public final class NeoForgePlatform implements Platform {
    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Optional<String> getModVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(MenuFactory<M, D> factory,
            StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        return IMenuTypeExtension.create((containerId, inventory, buffer) -> factory.create(containerId, inventory, codec.decode(buffer)));
    }

    @Override
    public <D> void openMenu(ServerPlayer player, MenuProvider provider, StreamCodec<? super RegistryFriendlyByteBuf, D> codec, D data) {
        player.openMenu(provider, buffer -> codec.encode(buffer, data));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(pos), payload);
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BlockEntityTypes.Factory<T> factory, Block... blocks) {
        return new BlockEntityType<>(factory::create, Set.of(blocks));
    }

    @Override
    public EnergyAccess findEnergy(ServerLevel level, BlockPos pos, Direction side) {
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
        return handler == null ? null : NeoForgeEnergy.access(handler);
    }
}
