package io.github.theskyfell.outworld.core.neoforge;

import io.github.theskyfell.outworld.core.energy.EnergyAccess;
import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Bridges Outworld energy and NeoForge's {@link EnergyHandler}, in both directions. */
final class NeoForgeEnergy {
    private NeoForgeEnergy() {
    }

    /** The one NeoForge handler for an Outworld buffer. */
    static EnergyHandler handlerFor(EnergyBuffer buffer) {
        return buffer.platformView(BufferHandler.class, BufferHandler::new);
    }

    /** Another mod's handler, usable from common code. */
    static EnergyAccess access(EnergyHandler handler) {
        return new HandlerAccess(handler);
    }

    /** An Outworld buffer as a NeoForge handler. Changes made in a transaction roll back with it. */
    private static final class BufferHandler implements EnergyHandler {
        private final EnergyBuffer buffer;
        private final Journal journal = new Journal();

        BufferHandler(EnergyBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public long getAmountAsLong() {
            return buffer.getAmount();
        }

        @Override
        public long getCapacityAsLong() {
            return buffer.getCapacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int accepted = (int) buffer.insertable(amount);
            if (accepted > 0) {
                journal.updateSnapshots(transaction);
                buffer.setAmountInTransaction(buffer.getAmount() + accepted);
            }
            return accepted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int removed = (int) buffer.extractable(amount);
            if (removed > 0) {
                journal.updateSnapshots(transaction);
                buffer.setAmountInTransaction(buffer.getAmount() - removed);
            }
            return removed;
        }

        private final class Journal extends SnapshotJournal<Long> {
            @Override
            protected Long createSnapshot() {
                return buffer.getAmount();
            }

            @Override
            protected void revertToSnapshot(Long snapshot) {
                buffer.setAmountInTransaction(snapshot);
            }

            @Override
            protected void onRootCommit(Long originalState) {
                if (buffer.getAmount() != originalState) {
                    buffer.notifyCommitted();
                }
            }
        }
    }

    /** Another mod's handler as {@link EnergyAccess}. Each call is its own transaction. */
    private record HandlerAccess(EnergyHandler handler) implements EnergyAccess {
        @Override
        public long insert(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) {
                return 0;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = handler.insert((int) Math.min(maxAmount, Integer.MAX_VALUE), transaction);
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
            try (Transaction transaction = Transaction.openRoot()) {
                int removed = handler.extract((int) Math.min(maxAmount, Integer.MAX_VALUE), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return removed;
            }
        }

        @Override
        public long getAmount() {
            return handler.getAmountAsLong();
        }

        @Override
        public long getCapacity() {
            return handler.getCapacityAsLong();
        }
    }
}
