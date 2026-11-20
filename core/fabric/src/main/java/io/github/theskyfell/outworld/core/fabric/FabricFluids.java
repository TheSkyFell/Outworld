package io.github.theskyfell.outworld.core.fabric;

import io.github.theskyfell.outworld.core.fluid.FluidAccess;
import io.github.theskyfell.outworld.core.fluid.FluidAmount;
import io.github.theskyfell.outworld.core.fluid.FluidTank;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Bridges Outworld fluid storage and Fabric's fluid {@link Storage}, in both directions. Both count in
 * droplets, so amounts pass through unchanged.
 */
final class FabricFluids {
    private FabricFluids() {
    }

    /** The one Fabric storage for an Outworld tank. */
    static Storage<FluidVariant> storageFor(FluidTank tank) {
        return tank.platformView(TankStorage.class, TankStorage::new);
    }

    /** Another mod's storage, usable from common code. */
    static FluidAccess access(Storage<FluidVariant> storage) {
        return new StorageAccess(storage);
    }

    /** An Outworld tank as a Fabric storage. Changes made in a transaction roll back with it. */
    private static final class TankStorage extends SnapshotParticipant<FluidAmount> implements SingleSlotStorage<FluidVariant> {
        private final FluidTank tank;

        TankStorage(FluidTank tank) {
            this.tank = tank;
        }

        @Override
        public boolean supportsInsertion() {
            return tank.canInsert();
        }

        @Override
        public boolean supportsExtraction() {
            return tank.canExtract();
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            Fluid fluid = resource.getFluid();
            DataComponentPatch components = resource.getComponentsPatch();
            long accepted = tank.insertable(fluid, components, maxAmount);
            if (accepted > 0) {
                updateSnapshots(transaction);
                tank.setContentsInTransaction(new FluidAmount(fluid, components, tank.getAmount() + accepted));
            }
            return accepted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            FluidAmount contents = tank.getContents();
            long removed = tank.extractable(resource.getFluid(), resource.getComponentsPatch(), maxAmount);
            if (removed > 0) {
                updateSnapshots(transaction);
                tank.setContentsInTransaction(new FluidAmount(contents.fluid(), contents.components(), contents.amount() - removed));
            }
            return removed;
        }

        @Override
        public boolean isResourceBlank() {
            return tank.isEmpty();
        }

        @Override
        public FluidVariant getResource() {
            FluidAmount contents = tank.getContents();
            return contents.isEmpty() ? FluidVariant.blank() : FluidVariant.of(contents.fluid(), contents.components());
        }

        @Override
        public long getAmount() {
            return tank.getAmount();
        }

        @Override
        public long getCapacity() {
            return tank.getCapacity();
        }

        @Override
        protected FluidAmount createSnapshot() {
            return tank.getContents();
        }

        @Override
        protected void readSnapshot(FluidAmount snapshot) {
            tank.setContentsInTransaction(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            tank.notifyCommitted();
        }
    }

    /** Another mod's storage as {@link FluidAccess}. Each call is its own transaction. */
    private record StorageAccess(Storage<FluidVariant> storage) implements FluidAccess {
        @Override
        public long insert(Fluid fluid, DataComponentPatch components, long maxAmount, boolean simulate) {
            if (maxAmount <= 0 || fluid == Fluids.EMPTY || !storage.supportsInsertion()) {
                return 0;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = storage.insert(FluidVariant.of(fluid, components), maxAmount, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return inserted;
            }
        }

        @Override
        public FluidAmount extract(Predicate<Fluid> filter, long maxAmount, boolean simulate) {
            if (maxAmount <= 0 || !storage.supportsExtraction()) {
                return FluidAmount.EMPTY;
            }
            // Pick the candidates first: the storage's views shouldn't be iterated while it changes.
            List<FluidVariant> candidates = new ArrayList<>();
            for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
                FluidVariant variant = view.getResource();
                if (!candidates.contains(variant) && filter.test(variant.getFluid())) {
                    candidates.add(variant);
                }
            }
            for (FluidVariant variant : candidates) {
                try (Transaction transaction = Transaction.openOuter()) {
                    long removed = storage.extract(variant, maxAmount, transaction);
                    if (removed > 0) {
                        if (!simulate) {
                            transaction.commit();
                        }
                        return new FluidAmount(variant.getFluid(), variant.getComponentsPatch(), removed);
                    }
                }
            }
            return FluidAmount.EMPTY;
        }
    }
}
