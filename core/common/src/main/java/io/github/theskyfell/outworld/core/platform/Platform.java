package io.github.theskyfell.outworld.core.platform;

import io.github.theskyfell.outworld.core.energy.EnergyAccess;
import io.github.theskyfell.outworld.core.fluid.FluidAccess;
import io.github.theskyfell.outworld.core.item.ItemAccess;
import io.github.theskyfell.outworld.core.menu.MenuFactory;
import io.github.theskyfell.outworld.core.registry.BlockEntityTypes;
import java.nio.file.Path;
import java.util.Optional;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * What common code needs from the mod loader, on both sides. Each loader project implements this and
 * names its implementation in {@code META-INF/services/io.github.theskyfell.outworld.core.platform.Platform}.
 *
 * <p>Only things that genuinely differ between loaders belong here, never gameplay logic. Common code
 * normally goes through the friendlier wrappers ({@code Menus}, {@code Network}, {@code ConfigFile}).
 * Client-only calls are in {@link io.github.theskyfell.outworld.core.client.ClientPlatform}.
 */
public interface Platform {
    /** "Fabric" or "NeoForge". */
    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /** The game's config folder. */
    Path getConfigDirectory();

    Optional<String> getModVersion(String modId);

    <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(MenuFactory<M, D> factory,
            StreamCodec<? super RegistryFriendlyByteBuf, D> codec);

    <D> void openMenu(ServerPlayer player, MenuProvider provider, StreamCodec<? super RegistryFriendlyByteBuf, D> codec, D data);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendToPlayersTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload);

    <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BlockEntityTypes.Factory<T> factory, Block... blocks);

    /** The energy storage at {@code pos} as seen from {@code side}, from any mod, or null. */
    EnergyAccess findEnergy(ServerLevel level, BlockPos pos, Direction side);

    /** The item storage at {@code pos} as seen from {@code side}, from any mod or vanilla, or null. */
    ItemAccess findItems(ServerLevel level, BlockPos pos, Direction side);

    /** The fluid storage at {@code pos} as seen from {@code side}, from any mod or vanilla, or null. */
    FluidAccess findFluids(ServerLevel level, BlockPos pos, Direction side);
}
