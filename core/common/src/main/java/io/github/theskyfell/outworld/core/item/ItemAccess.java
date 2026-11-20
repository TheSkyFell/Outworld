package io.github.theskyfell.outworld.core.item;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/**
 * Something items can be moved into or out of: an Outworld {@link ItemBuffer}, or another block's item
 * storage found with {@link ItemTransfer#find}.
 *
 * <p>Use it on the server thread, outside of any loader transaction.
 */
public interface ItemAccess {
    /**
     * Inserts as much of {@code stack} as fits. The stack itself is not changed.
     *
     * @param simulate true to only ask how much would fit
     * @return how many items were inserted
     */
    int insert(ItemStack stack, boolean simulate);

    /**
     * Takes up to {@code maxCount} of the first item that matches {@code filter}.
     *
     * @param simulate true to only ask what would be taken
     * @return what was taken, or an empty stack
     */
    ItemStack extract(Predicate<ItemStack> filter, int maxCount, boolean simulate);
}
