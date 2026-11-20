package io.github.theskyfell.outworld.core.fabric;

import io.github.theskyfell.outworld.core.energy.EnergyAccess;
import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

/** Bridges Outworld energy and Team Reborn Energy, Fabric's shared energy API, in both directions. */
final class FabricEnergy {
    private FabricEnergy() {
    }

    /** The one Team Reborn Energy storage for an Outworld buffer. */
    static EnergyStorage storageFor(EnergyBuffer buffer) {
        return buffer.platformView(BufferStorage.class, BufferStorage::new);
    }

    /** Another mod's storage, usable from common code. */
    static EnergyAccess access(EnergyStorage storage) {
        return new StorageAccess(storage);
    }

    /** An Outworld buffer as a Team Reborn Energy storage. Changes made in a transaction roll back with it. */
    private static final class BufferStorage extends SnapshotParticipant<Long> implements EnergyStorage {
        private final EnergyBuffer buffer;

        BufferStorage(EnergyBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public boolean supportsInsertion() {
            return buffer.canInsert();
        }

        @Override
        public boolean supportsExtraction() {
            return buffer.canExtract();
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            long accepted = buffer.insertable(maxAmount);
            if (accepted > 0) {
                updateSnapshots(transaction);
                buffer.setAmountInTransaction(buffer.getAmount() + accepted);
            }
            return accepted;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            long removed = buffer.extractable(maxAmount);
            if (removed > 0) {
                updateSnapshots(transaction);
                buffer.setAmountInTransaction(buffer.getAmount() - removed);
            }
            return removed;
        }

        @Override
        public long getAmount() {
            return buffer.getAmount();
        }

        @Override
        public long getCapacity() {
            return buffer.getCapacity();
        }

        @Override
        protected Long createSnapshot() {
            return buffer.getAmount();
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            buffer.setAmountInTransaction(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            buffer.notifyCommitted();
        }
    }

    /** Another mod's storage as {@link EnergyAccess}. Each call is its own transaction. */
    private record StorageAccess(EnergyStorage storage) implements EnergyAccess {
        @Override
        public long insert(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) {
                return 0;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long accepted = storage.insert(maxAmount, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return accepted;
            }
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) {
                return 0;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long removed = storage.extract(maxAmount, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return removed;
            }
        }

        @Override
        public long getAmount() {
            return storage.getAmount();
        }

        @Override
        public long getCapacity() {
            return storage.getCapacity();
        }
    }
}
