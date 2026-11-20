package io.github.theskyfell.outworld.core.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Creates the screen for an opened menu. Usually a screen's constructor: {@code MyScreen::new}. */
@FunctionalInterface
public interface ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
    S create(M menu, Inventory inventory, Component title);
}
