package io.github.theskyfell.outworld.core.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Connects menu types to screens. Given to {@link OutworldClientModule#registerScreens}. */
public interface ScreenRegistrar {
    <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type,
            ScreenFactory<M, S> factory);
}
