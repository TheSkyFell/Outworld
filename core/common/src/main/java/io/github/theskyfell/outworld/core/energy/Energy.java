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
}
