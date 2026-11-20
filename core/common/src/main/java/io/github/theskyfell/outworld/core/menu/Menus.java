package io.github.theskyfell.outworld.core.menu;

import io.github.theskyfell.outworld.core.platform.Services;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Menus that carry open data from the server to the client, such as the position of the machine
 * being opened. Each loader does this differently; these two calls hide that.
 */
public final class Menus {
    private Menus() {
    }

    /** Creates a menu type. Register the result through a {@code Registrar} for {@code Registries.MENU}. */
    public static <M extends AbstractContainerMenu, D> MenuType<M> createType(MenuFactory<M, D> factory,
            StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        return Services.PLATFORM.createMenuType(factory, codec);
    }

    /**
     * Opens a menu for {@code player} and sends {@code data} to their client. Use the same codec as the
     * menu type.
     */
    public static <D> void open(ServerPlayer player, MenuProvider provider,
            StreamCodec<? super RegistryFriendlyByteBuf, D> codec, D data) {
        Services.PLATFORM.openMenu(player, provider, codec, data);
    }
}
