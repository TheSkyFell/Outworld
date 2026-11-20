package io.github.theskyfell.outworld.core.fluid;

import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;

/**
 * Something fluid can be moved into or out of: an Outworld {@link FluidTank}, or another block's fluid
 * storage found with {@link FluidTransfer#find}. Amounts are in droplets (see {@link FluidUnits}).
 *
 * <p>Use it on the server thread, outside of any loader transaction.
 */
public interface FluidAccess {
    /**
     * Inserts up to {@code maxAmount} of a fluid.
     *
     * @param simulate true to only ask how much would be accepted
     * @return the amount accepted
     */
    long insert(Fluid fluid, DataComponentPatch components, long maxAmount, boolean simulate);

    /**
     * Removes up to {@code maxAmount} of the first fluid that matches {@code filter}.
     *
     * @param simulate true to only ask what would be removed
     * @return what was removed, or {@link FluidAmount#EMPTY}
     */
    FluidAmount extract(Predicate<Fluid> filter, long maxAmount, boolean simulate);
}
