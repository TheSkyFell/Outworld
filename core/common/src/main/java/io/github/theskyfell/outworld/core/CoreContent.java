package io.github.theskyfell.outworld.core;

import io.github.theskyfell.outworld.core.machine.MachineMenu;
import io.github.theskyfell.outworld.core.menu.Menus;
import io.github.theskyfell.outworld.core.registry.Registrar;
import io.github.theskyfell.outworld.core.registry.RegistryEntry;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** What Core itself registers. */
public final class CoreContent {
    public static final Registrar<MenuType<?>> MENUS = Registrar.create(Registries.MENU, OutworldCore.MOD_ID);

    /** The screen shared by every recipe machine, from any Outworld mod. */
    public static final RegistryEntry<MenuType<?>, MenuType<MachineMenu>> MACHINE_MENU = MENUS.register("machine",
            key -> Menus.createType(MachineMenu::new, MachineMenu.OpenData.STREAM_CODEC));

    private CoreContent() {
    }

    static List<Registrar<?>> registrars() {
        return List.of(MENUS);
    }
}
