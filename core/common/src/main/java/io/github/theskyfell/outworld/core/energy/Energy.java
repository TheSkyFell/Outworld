package io.github.theskyfell.outworld.core.energy;

import io.github.theskyfell.outworld.core.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Finds energy storages in the world, from any mod. */
public final class Energy {
    private Energy() {
    }

    /**
     * The energy storage at {@code pos}, as seen from {@code side} (the face of that block you are
     * touching), or null if there is none. Works with any mod that uses the loader's energy API.
     *
     * <p>Returns null for a position whose chunk isn't loaded, rather than loading it.
     *
     * <p>Looking up is not free: a machine that talks to the same neighbour often should keep the
     * result and look again only after a neighbour change.
     */
    public static EnergyAccess find(ServerLevel level, BlockPos pos, Direction side) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return Services.PLATFORM.findEnergy(level, pos, side);
    }

    /**
     * Pushes up to {@code maxAmount} FE out of {@code source} into the energy storage on each side of
     * {@code pos} (cables, machines, batteries), in turn.
     *
     * @return how much went out
     */
    public static long pushToNeighbours(ServerLevel level, BlockPos pos, EnergyBuffer source, long maxAmount) {
        long pushed = 0;
        for (Direction direction : Direction.values()) {
            long left = Math.min(maxAmount - pushed, source.getAmount());
            if (left <= 0) {
                break;
            }
            EnergyAccess target = find(level, pos.relative(direction), direction.getOpposite());
            if (target != null) {
                long accepted = target.insert(left, false);
                if (accepted > 0) {
                    source.setAmount(source.getAmount() - accepted);
                    pushed += accepted;
                }
            }
        }
        return pushed;
    }

    /**
     * Pulls up to {@code maxAmount} FE into {@code target} from the energy storage on each side of
     * {@code pos}, in turn, as far as {@code target} has room.
     *
     * @return how much came in
     */
    public static long pullFromNeighbours(ServerLevel level, BlockPos pos, EnergyBuffer target, long maxAmount) {
        long pulled = 0;
        for (Direction direction : Direction.values()) {
            long room = Math.min(maxAmount - pulled, target.getCapacity() - target.getAmount());
            if (room <= 0) {
                break;
            }
            EnergyAccess source = find(level, pos.relative(direction), direction.getOpposite());
            if (source != null) {
                long taken = source.extract(room, false);
                if (taken > 0) {
                    target.setAmount(target.getAmount() + taken);
                    pulled += taken;
                }
            }
        }
        return pulled;
    }
}
