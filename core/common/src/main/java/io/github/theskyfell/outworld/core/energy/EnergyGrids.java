package io.github.theskyfell.outworld.core.energy;

import com.mojang.serialization.Codec;
import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.grid.GridKind;
import io.github.theskyfell.outworld.core.grid.Grids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Energy grids: touching {@link EnergyConductor}s (cables) share one energy buffer, which each of them
 * exposes on every side. Generators push into it, machines draw from it, and whatever is put in is
 * offered to the machines and batteries next to the grid. See {@link Grids} for how grids are kept.
 */
public final class EnergyGrids {
    public static final Grids<EnergyBuffer, Long> GRIDS = Grids.register(new Kind());

    private EnergyGrids() {
    }

    /** Registers the kind; called by Core at startup. */
    public static void bootstrap() {
    }

    /** The shared buffer of the grid the conductor at {@code pos} is in, or null if there is none. */
    public static EnergyBuffer bufferAt(ServerLevel level, BlockPos pos) {
        return GRIDS.storageAt(level, pos);
    }

    private static final class Kind extends GridKind<EnergyBuffer, Long> {
        Kind() {
            super(Identifier.fromNamespaceAndPath(OutworldCore.MOD_ID, "energy"), Codec.LONG);
        }

        @Override
        public long capacity(BlockState state) {
            return state.getBlock() instanceof EnergyConductor conductor ? conductor.gridCapacity() : 0;
        }

        @Override
        protected EnergyBuffer createStorage(long capacity) {
            return new EnergyBuffer(capacity, capacity, capacity);
        }

        @Override
        protected Long contents(EnergyBuffer storage) {
            return storage.getAmount();
        }

        @Override
        protected void setContents(EnergyBuffer storage, Long contents) {
            storage.setAmount(contents);
        }

        @Override
        protected void listen(EnergyBuffer storage, Runnable listener) {
            storage.onChange(listener);
        }

        @Override
        protected Long empty() {
            return 0L;
        }

        @Override
        protected long amount(Long portion) {
            return portion;
        }

        @Override
        protected Long withAmount(Long portion, long amount) {
            return amount;
        }

        @Override
        protected boolean canMix(Long first, Long second) {
            return true;
        }

        @Override
        protected String describe(Long contents, long capacity) {
            return String.format("%,d / %,d FE", contents, capacity);
        }

        @Override
        protected long offer(ServerLevel level, EnergyBuffer storage, BlockPos pos, Direction side) {
            EnergyAccess target = Energy.find(level, pos, side);
            if (target == null) {
                return 0;
            }
            long accepted = target.insert(storage.getAmount(), false);
            if (accepted > 0) {
                storage.setAmount(storage.getAmount() - accepted);
            }
            return accepted;
        }
    }
}
