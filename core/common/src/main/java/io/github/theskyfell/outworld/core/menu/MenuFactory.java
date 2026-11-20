package io.github.theskyfell.outworld.core.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Creates a menu on the client from the data the server sent when it opened the menu.
 *
 * @param <M> the menu
 * @param <D> the open data, e.g. a block position
 */
@FunctionalInterface
public interface MenuFactory<M extends AbstractContainerMenu, D> {
    M create(int containerId, Inventory inventory, D data);
}
