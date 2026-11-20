package io.github.theskyfell.outworld.core.gas;

import io.github.theskyfell.outworld.core.OutworldCore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Finds gas storages in the world and moves gas between them. Outworld's gases are its own kind of
 * resource, so there is no loader API to go through: blocks implement {@link GasHolder}, and other
 * mods' gas storage can be bridged in with {@link #registerProvider} (Mekanism's chemicals, say).
 */
public final class GasTransfer {
    /** Finds gas storage that isn't a {@link GasHolder}. */
    @FunctionalInterface
    public interface Provider {
        /** The gas storage at {@code pos} as seen from {@code side}, or null. */
        GasAccess find(ServerLevel level, BlockPos pos, BlockEntity blockEntity, Direction side);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private GasTransfer() {
    }

    /** Adds a way to find gas storage in blocks that don't implement {@link GasHolder}. */
    public static void registerProvider(Provider provider) {
        PROVIDERS.add(provider);
    }

    /**
     * The gas storage at {@code pos}, as seen from {@code side} (the face of that block you are
     * touching), or null if there is none or its chunk isn't loaded. At gas pipes and tank blocks
     * ({@link GasConductor}s) it is their grid's shared tank.
     */
    public static GasAccess find(ServerLevel level, BlockPos pos, Direction side) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        if (level.getBlockState(pos).getBlock() instanceof GasConductor) {
            return GasGrids.tankAt(level, pos);
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        if (blockEntity instanceof GasHolder holder) {
            return holder.gasAccess(side);
        }
        for (Provider provider : PROVIDERS) {
            GasAccess access = provider.find(level, pos, blockEntity, side);
            if (access != null) {
                return access;
            }
        }
        return null;
    }

    /**
     * Moves up to {@code maxAmount} of the first gas in {@code from} that matches {@code filter} into
     * {@code to}. Both sides are asked first, so normally nothing moves that can't arrive.
     *
     * @return how many millibuckets arrived
     */
    public static long move(GasAccess from, GasAccess to, Predicate<ResourceKey<Gas>> filter, long maxAmount) {
        GasStack offered = from.extract(filter, maxAmount, true);
        if (offered.isEmpty()) {
            return 0;
        }
        long accepted = to.insert(offered.gas(), offered.amount(), true);
        if (accepted <= 0) {
            return 0;
        }
        GasStack taken = from.extract(gas -> gas.equals(offered.gas()), accepted, false);
        if (taken.isEmpty()) {
            return 0;
        }
        long inserted = to.insert(taken.gas(), taken.amount(), false);
        if (inserted < taken.amount()) {
            long rest = taken.amount() - inserted;
            long returned = from.insert(taken.gas(), rest, false);
            if (returned < rest) {
                OutworldCore.LOG.warn("Lost {} mB of {} moving gas: neither side would take it back",
                        rest - returned, taken.gas().identifier());
            }
        }
        return inserted;
    }
}
