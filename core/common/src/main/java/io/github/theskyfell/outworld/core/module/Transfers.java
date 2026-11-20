package io.github.theskyfell.outworld.core.module;

import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import io.github.theskyfell.outworld.core.fluid.FluidTank;
import io.github.theskyfell.outworld.core.item.ItemBuffer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Exposes Outworld storages to other mods through each loader's transfer API. Given to
 * {@link OutworldModule#registerTransfers}.
 *
 * <p>Each method takes a function from a block entity and the side being accessed to the storage on
 * that side. The side is null when the caller doesn't say which face it is touching. Return null for
 * no storage on that side.
 */
public interface Transfers {
    /** Makes a block entity type's energy buffer visible to cables and machines from any mod. */
    <T extends BlockEntity> void energy(BlockEntityType<T> type, Sided<T, EnergyBuffer> energy);

    /** Makes a block entity type's item slots visible to hoppers, pipes and machines from any mod. */
    <T extends BlockEntity> void items(BlockEntityType<T> type, Sided<T, ItemBuffer> items);

    /** Makes a block entity type's fluid tank visible to pipes, buckets and machines from any mod. */
    <T extends BlockEntity> void fluids(BlockEntityType<T> type, Sided<T, FluidTank> fluids);

    /** The storage a block entity exposes on a side. */
    @FunctionalInterface
    interface Sided<T extends BlockEntity, S> {
        /**
         * The storage to expose on {@code side}, or null for none on that side.
         *
         * @param side the face being accessed, or null when the caller doesn't say
         */
        S get(T blockEntity, Direction side);
    }
}
