package io.github.theskyfell.outworld.core.fluid;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.grid.GridKind;
import io.github.theskyfell.outworld.core.grid.Grids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fluid grids: touching {@link FluidConductor}s (pipes, tank blocks) share one tank, which each of them
 * exposes on every side. Machines push what they make into it and draw what they need from it, and
 * whatever is put in is offered to the storage next to the grid. A grid holds one fluid at a time. See
 * {@link Grids} for how grids are kept.
 */
public final class FluidGrids {
    public static final Grids<FluidTank, FluidAmount> GRIDS = Grids.register(new Kind());

    private FluidGrids() {
    }

    /** Registers the kind; called by Core at startup. */
    public static void bootstrap() {
    }

    /** The shared tank of the grid the block at {@code pos} is in, or null if there is none. */
    public static FluidTank tankAt(ServerLevel level, BlockPos pos) {
        return GRIDS.storageAt(level, pos);
    }

    private static final class Kind extends GridKind<FluidTank, FluidAmount> {
        Kind() {
            super(Identifier.fromNamespaceAndPath(OutworldCore.MOD_ID, "fluid"), FluidAmount.CODEC);
        }

        @Override
        public long capacity(BlockState state) {
            return state.getBlock() instanceof FluidConductor conductor ? conductor.fluidGridCapacity() : 0;
        }

        @Override
        protected FluidTank createStorage(long capacity) {
            return new FluidTank(capacity);
        }

        @Override
        protected FluidAmount contents(FluidTank storage) {
            return storage.getContents();
        }

        @Override
        protected void setContents(FluidTank storage, FluidAmount contents) {
            storage.setContents(contents);
        }

        @Override
        protected void listen(FluidTank storage, Runnable listener) {
            storage.onChange(listener);
        }

        @Override
        protected FluidAmount empty() {
            return FluidAmount.EMPTY;
        }

        @Override
        protected long amount(FluidAmount portion) {
            return portion.isEmpty() ? 0 : portion.amount();
        }

        @Override
        protected FluidAmount withAmount(FluidAmount portion, long amount) {
            return amount <= 0 ? FluidAmount.EMPTY : new FluidAmount(portion.fluid(), portion.components(), amount);
        }

        @Override
        protected boolean canMix(FluidAmount first, FluidAmount second) {
            return first.isSameFluid(second.fluid(), second.components());
        }

        @Override
        protected String describe(FluidAmount contents, long capacity) {
            String held = contents.isEmpty() ? "empty" : BuiltInRegistries.FLUID.getKey(contents.fluid()) + " "
                    + String.format("%,d", contents.amount() / FluidUnits.MILLIBUCKET);
            return held + String.format(" / %,d mB", capacity / FluidUnits.MILLIBUCKET);
        }

        @Override
        protected long offer(ServerLevel level, FluidTank storage, BlockPos pos, Direction side) {
            FluidAccess target = FluidTransfer.find(level, pos, side);
            return target == null ? 0 : FluidTransfer.move(storage, target, fluid -> true, storage.getAmount());
        }
    }
}
