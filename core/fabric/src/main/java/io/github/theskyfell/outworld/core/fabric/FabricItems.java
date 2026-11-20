package io.github.theskyfell.outworld.core.fabric;

import io.github.theskyfell.outworld.core.item.ItemAccess;
import io.github.theskyfell.outworld.core.item.ItemBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** Bridges Outworld item storage and Fabric's item {@link Storage}, in both directions. */
final class FabricItems {
    private FabricItems() {
    }

    /**
     * An Outworld buffer as a Fabric storage. Fabric's container wrapper does the transaction work and
     * shares its snapshots between sides.
     */
    static Storage<ItemVariant> storageFor(ItemBuffer buffer, Direction side) {
        // Fabric skips the take-out check when no side is given. A buffer offers the same slots and
        // rules on every side, so asking as if from above keeps the slot roles in force.
        return ContainerStorage.of(buffer, side == null ? Direction.UP : side);
    }

    /** Another mod's storage, usable from common code. */
    static ItemAccess access(Storage<ItemVariant> storage) {
        return new StorageAccess(storage);
    }

    /** Another mod's storage as {@link ItemAccess}. Each call is its own transaction. */
    private record StorageAccess(Storage<ItemVariant> storage) implements ItemAccess {
        @Override
        public int insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !storage.supportsInsertion()) {
                return 0;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return (int) inserted;
            }
        }

        @Override
        public ItemStack extract(Predicate<ItemStack> filter, int maxCount, boolean simulate) {
            if (maxCount <= 0 || !storage.supportsExtraction()) {
                return ItemStack.EMPTY;
            }
            // Pick the candidates first: the storage's views shouldn't be iterated while it changes.
            List<ItemVariant> candidates = new ArrayList<>();
            for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
                ItemVariant variant = view.getResource();
                if (!candidates.contains(variant) && filter.test(variant.toStack())) {
                    candidates.add(variant);
                }
            }
            for (ItemVariant variant : candidates) {
                try (Transaction transaction = Transaction.openOuter()) {
                    // Takes the item from every slot holding it, not just the first.
                    long taken = storage.extract(variant, maxCount, transaction);
                    if (taken > 0) {
                        if (!simulate) {
                            transaction.commit();
                        }
                        return variant.toStack((int) taken);
                    }
                }
            }
            return ItemStack.EMPTY;
        }
    }
}
