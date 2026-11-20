package io.github.theskyfell.outworld.core.neoforge;

import io.github.theskyfell.outworld.core.fluid.FluidAccess;
import io.github.theskyfell.outworld.core.fluid.FluidAmount;
import io.github.theskyfell.outworld.core.fluid.FluidTank;
import io.github.theskyfell.outworld.core.fluid.FluidUnits;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Bridges Outworld fluid storage and NeoForge's fluid {@link ResourceHandler}, in both directions.
 * NeoForge counts millibuckets and Outworld counts droplets (81 per millibucket), so only whole
 * millibuckets cross the bridge; a remainder smaller than that stays where it is.
 */
final class NeoForgeFluids {
    private static final long MB = FluidUnits.MILLIBUCKET;

    private NeoForgeFluids() {
    }

    /** The one NeoForge handler for an Outworld tank. */
    static ResourceHandler<FluidResource> handlerFor(FluidTank tank) {
        return tank.platformView(TankHandler.class, TankHandler::new);
    }

    /** Another mod's handler, usable from common code. */
    static FluidAccess access(ResourceHandler<FluidResource> handler) {
        return new HandlerAccess(handler);
    }

    private static int toMillibuckets(long droplets) {
        return (int) Math.min(droplets / MB, Integer.MAX_VALUE);
    }

    /** An Outworld tank as a NeoForge handler with one index. Changes roll back with the transaction. */
    private static final class TankHandler implements ResourceHandler<FluidResource> {
        private final FluidTank tank;
        private final Journal journal = new Journal();

        TankHandler(FluidTank tank) {
            this.tank = tank;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            checkIndex(index);
            FluidAmount contents = tank.getContents();
            return contents.isEmpty() ? FluidResource.EMPTY : FluidResource.of(contents.fluid(), contents.components());
        }

        @Override
        public long getAmountAsLong(int index) {
            checkIndex(index);
            return tank.getAmount() / MB;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            checkIndex(index);
            return tank.getCapacity() / MB;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            checkIndex(index);
            return tank.accepts(resource.getFluid());
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            checkIndex(index);
            Fluid fluid = resource.getFluid();
            DataComponentPatch components = resource.getComponentsPatch();
            int accepted = toMillibuckets(tank.insertable(fluid, components, amount * MB));
            if (accepted > 0) {
                journal.updateSnapshots(transaction);
                tank.setContentsInTransaction(new FluidAmount(fluid, components, tank.getAmount() + accepted * MB));
            }
            return accepted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            checkIndex(index);
            FluidAmount contents = tank.getContents();
            int removed = toMillibuckets(tank.extractable(resource.getFluid(), resource.getComponentsPatch(), amount * MB));
            if (removed > 0) {
                journal.updateSnapshots(transaction);
                tank.setContentsInTransaction(new FluidAmount(contents.fluid(), contents.components(), contents.amount() - removed * MB));
            }
            return removed;
        }

        private static void checkIndex(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException("A tank has one index, 0, not " + index);
            }
        }

        private final class Journal extends SnapshotJournal<FluidAmount> {
            @Override
            protected FluidAmount createSnapshot() {
                return tank.getContents();
            }

            @Override
            protected void revertToSnapshot(FluidAmount snapshot) {
                tank.setContentsInTransaction(snapshot);
            }

            @Override
            protected void onRootCommit(FluidAmount originalState) {
                if (!originalState.equals(tank.getContents())) {
                    tank.notifyCommitted();
                }
            }
        }
    }

    /** Another mod's handler as {@link FluidAccess}. Each call is its own transaction. */
    private record HandlerAccess(ResourceHandler<FluidResource> handler) implements FluidAccess {
        @Override
        public long insert(Fluid fluid, DataComponentPatch components, long maxAmount, boolean simulate) {
            int millibuckets = toMillibuckets(maxAmount);
            if (millibuckets <= 0 || fluid == Fluids.EMPTY) {
                return 0;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(FluidResource.of(fluid, components), millibuckets, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return inserted * MB;
            }
        }

        @Override
        public FluidAmount extract(Predicate<Fluid> filter, long maxAmount, boolean simulate) {
            int millibuckets = toMillibuckets(maxAmount);
            if (millibuckets <= 0) {
                return FluidAmount.EMPTY;
            }
            int size = handler.size();
            for (int index = 0; index < size; index++) {
                FluidResource resource = handler.getResource(index);
                if (resource.isEmpty() || !filter.test(resource.getFluid())) {
                    continue;
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    int removed = handler.extract(resource, millibuckets, transaction);
                    if (removed > 0) {
                        if (!simulate) {
                            transaction.commit();
                        }
                        return new FluidAmount(resource.getFluid(), resource.getComponentsPatch(), removed * MB);
                    }
                }
            }
            return FluidAmount.EMPTY;
        }
    }
}
