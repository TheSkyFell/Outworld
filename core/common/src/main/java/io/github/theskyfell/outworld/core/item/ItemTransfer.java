package io.github.theskyfell.outworld.core.item;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.platform.Services;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Finds item storages in the world, from any mod, and moves items between them. */
public final class ItemTransfer {
    private ItemTransfer() {
    }

    /**
     * The item storage at {@code pos}, as seen from {@code side} (the face of that block you are
     * touching), or null if there is none or its chunk isn't loaded. Works with vanilla containers and
     * any mod that uses the loader's item API.
     */
    public static ItemAccess find(ServerLevel level, BlockPos pos, Direction side) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return Services.PLATFORM.findItems(level, pos, side);
    }

    /**
     * Moves up to {@code maxCount} of the first item in {@code from} that matches {@code filter} into
     * {@code to}. Both sides are asked first, so normally nothing moves that can't arrive.
     *
     * @return how many items arrived
     */
    public static int move(ItemAccess from, ItemAccess to, Predicate<ItemStack> filter, int maxCount) {
        ItemStack offered = from.extract(filter, maxCount, true);
        if (offered.isEmpty()) {
            return 0;
        }
        int accepted = to.insert(offered, true);
        if (accepted <= 0) {
            return 0;
        }
        ItemStack taken = from.extract(stack -> ItemStack.isSameItemSameComponents(stack, offered), accepted, false);
        if (taken.isEmpty()) {
            return 0;
        }
        int inserted = to.insert(taken, false);
        if (inserted < taken.getCount()) {
            // The target took less than it promised: hand the rest back.
            ItemStack rest = taken.copyWithCount(taken.getCount() - inserted);
            int returned = from.insert(rest, false);
            if (returned < rest.getCount()) {
                OutworldCore.LOG.warn("Lost {} x {} moving items: neither side would take them back",
                        rest.getCount() - returned, rest.getItem());
            }
        }
        return inserted;
    }
}
