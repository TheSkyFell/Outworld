package io.github.theskyfell.outworld.core.neoforge;

import io.github.theskyfell.outworld.core.item.ItemAccess;
import io.github.theskyfell.outworld.core.item.ItemBuffer;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Bridges Outworld item storage and NeoForge's item {@link ResourceHandler}, in both directions. */
final class NeoForgeItems {
    private NeoForgeItems() {
    }

    /**
     * An Outworld buffer as a NeoForge handler. NeoForge's container wrapper does the transaction work
     * and shares its snapshots between sides.
     */
    static ResourceHandler<ItemResource> handlerFor(ItemBuffer buffer, Direction side) {
        // NeoForge skips the take-out check when no side is given. A buffer offers the same slots
        // and rules on every side, so asking as if from above keeps the slot roles in force.
        return new WorldlyContainerWrapper(buffer, side == null ? Direction.UP : side);
    }

    /** Another mod's handler, usable from common code. */
    static ItemAccess access(ResourceHandler<ItemResource> handler) {
        return new HandlerAccess(handler);
    }

    /** Another mod's handler as {@link ItemAccess}. Each call is its own transaction. */
    private record HandlerAccess(ResourceHandler<ItemResource> handler) implements ItemAccess {
        @Override
        public int insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return 0;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return inserted;
            }
        }

        @Override
        public ItemStack extract(Predicate<ItemStack> filter, int maxCount, boolean simulate) {
            if (maxCount <= 0) {
                return ItemStack.EMPTY;
            }
            int size = handler.size();
            for (int index = 0; index < size; index++) {
                ItemResource resource = handler.getResource(index);
                if (resource.isEmpty() || !resource.test(filter)) {
                    continue;
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    // Take the matching item from every slot holding it, not just this one.
                    int taken = handler.extract(resource, maxCount, transaction);
                    if (taken > 0) {
                        if (!simulate) {
                            transaction.commit();
                        }
                        return resource.toStack(taken);
                    }
                }
            }
            return ItemStack.EMPTY;
        }
    }
}
