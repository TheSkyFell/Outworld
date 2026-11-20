package io.github.theskyfell.outworld.core.fluid;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.platform.Services;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;

/** Finds fluid storages in the world, from any mod, and moves fluid between them. */
public final class FluidTransfer {
    private FluidTransfer() {
    }

    /**
     * The fluid storage at {@code pos}, as seen from {@code side} (the face of that block you are
     * touching), or null if there is none or its chunk isn't loaded. Works with vanilla cauldrons and
     * any mod that uses the loader's fluid API.
     */
    public static FluidAccess find(ServerLevel level, BlockPos pos, Direction side) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return Services.PLATFORM.findFluids(level, pos, side);
    }

    /**
     * Moves up to {@code maxAmount} droplets of the first fluid in {@code from} that matches
     * {@code filter} into {@code to}. Both sides are asked first, so normally nothing moves that can't
     * arrive. Some storages only move whole units (a cauldron only gives up a full bucket of water),
     * so less than both sides could hold may move, or nothing.
     *
     * @return how many droplets arrived
     */
    public static long move(FluidAccess from, FluidAccess to, Predicate<Fluid> filter, long maxAmount) {
        FluidAmount offered = from.extract(filter, maxAmount, true);
        if (offered.isEmpty()) {
            return 0;
        }
        long accepted = to.insert(offered.fluid(), offered.components(), offered.amount(), true);
        if (accepted <= 0) {
            return 0;
        }
        FluidAmount taken = from.extract(fluid -> fluid == offered.fluid(), accepted, false);
        if (taken.isEmpty()) {
            return 0;
        }
        long inserted = to.insert(taken.fluid(), taken.components(), taken.amount(), false);
        if (inserted < taken.amount()) {
            // The target took less than it promised: hand the rest back.
            long rest = taken.amount() - inserted;
            long returned = from.insert(taken.fluid(), taken.components(), rest, false);
            if (returned < rest) {
                OutworldCore.LOG.warn("Lost {} droplets of {} moving fluid: neither side would take them back",
                        rest - returned, taken.fluid());
            }
        }
        return inserted;
    }
}
