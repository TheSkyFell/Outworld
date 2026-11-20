package io.github.theskyfell.outworld.core.registry;

import io.github.theskyfell.outworld.core.platform.Services;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Creative tabs, placed by each loader on its own page of mod tabs. */
public final class CreativeTabs {
    private CreativeTabs() {
    }

    /**
     * A tab listing every item in {@code items}, in the order they were declared, titled by the
     * translation key {@code itemGroup.<namespace>}. Register it in a {@code Registrar} for
     * {@code Registries.CREATIVE_MODE_TAB}.
     */
    public static CreativeModeTab listing(Registrar<Item> items, Supplier<ItemStack> icon) {
        return Services.PLATFORM.creativeTabBuilder()
                .title(Component.translatable("itemGroup." + items.namespace()))
                .icon(icon)
                .displayItems((parameters, output) -> {
                    for (RegistryEntry<Item, ? extends Item> entry : items.entries()) {
                        output.accept(entry.get());
                    }
                })
                .build();
    }
}
