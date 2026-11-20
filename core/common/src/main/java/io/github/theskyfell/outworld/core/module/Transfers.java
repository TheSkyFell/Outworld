package io.github.theskyfell.outworld.core.module;

import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Exposes Outworld storages to other mods through each loader's transfer API. Given to
 * {@link OutworldModule#registerTransfers}.
 */
public interface Transfers {
    /** Makes a block entity type's energy buffer visible to cables and machines from any mod. */
    <T extends BlockEntity> void energy(BlockEntityType<T> type, SidedEnergy<T> energy);

    @FunctionalInterface
    interface SidedEnergy<T extends BlockEntity> {
        /**
         * The buffer to expose on {@code side}, or null for none on that side.
         *
         * @param side the face being accessed, or null when the caller doesn't say
         */
        EnergyBuffer get(T blockEntity, Direction side);
    }
}
