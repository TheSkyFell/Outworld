package io.github.theskyfell.outworld.core.gas;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.grid.GridKind;
import io.github.theskyfell.outworld.core.grid.Grids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gas grids: touching {@link GasConductor}s (gas pipes, gas tank blocks) share one tank, which
 * {@link GasTransfer#find} finds at each of them. Machines push what they make into it and draw what
 * they need from it, and whatever is put in is offered to the storage next to the grid. A grid holds
 * one gas at a time. See {@link Grids} for how grids are kept.
 */
public final class GasGrids {
    public static final Grids<GasTank, GasStack> GRIDS = Grids.register(new Kind());

    private GasGrids() {
    }

    /** Registers the kind; called by Core at startup. */
    public static void bootstrap() {
    }

    /** The shared tank of the grid the block at {@code pos} is in, or null if there is none. */
    public static GasTank tankAt(ServerLevel level, BlockPos pos) {
        return GRIDS.storageAt(level, pos);
    }

    private static final class Kind extends GridKind<GasTank, GasStack> {
        Kind() {
            super(Identifier.fromNamespaceAndPath(OutworldCore.MOD_ID, "gas"), GasStack.CODEC);
        }

        @Override
        public long capacity(BlockState state) {
            return state.getBlock() instanceof GasConductor conductor ? conductor.gasGridCapacity() : 0;
        }

        @Override
        protected GasTank createStorage(long capacity) {
            return new GasTank(capacity);
        }

        @Override
        protected GasStack contents(GasTank storage) {
            return storage.getContents();
        }

        @Override
        protected void setContents(GasTank storage, GasStack contents) {
            storage.setContents(contents);
        }

        @Override
        protected void listen(GasTank storage, Runnable listener) {
            storage.onChange(listener);
        }

        @Override
        protected GasStack empty() {
            return GasStack.EMPTY;
        }

        @Override
        protected long amount(GasStack portion) {
            return portion.isEmpty() ? 0 : portion.amount();
        }

        @Override
        protected GasStack withAmount(GasStack portion, long amount) {
            return amount <= 0 ? GasStack.EMPTY : portion.withAmount(amount);
        }

        @Override
        protected boolean canMix(GasStack first, GasStack second) {
            return first.is(second.gas());
        }

        @Override
        protected String describe(GasStack contents, long capacity) {
            String held = contents.isEmpty() ? "empty" : contents.gas().identifier() + String.format(" %,d", contents.amount());
            return held + String.format(" / %,d mB", capacity);
        }

        @Override
        protected long offer(ServerLevel level, GasTank storage, BlockPos pos, Direction side) {
            GasAccess target = GasTransfer.find(level, pos, side);
            return target == null ? 0 : GasTransfer.move(storage, target, gas -> true, storage.getAmount());
        }
    }
}
