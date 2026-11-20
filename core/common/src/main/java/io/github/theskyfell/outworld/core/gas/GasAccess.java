package io.github.theskyfell.outworld.core.gas;

import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;

/**
 * Something gas can be moved into or out of: an Outworld {@link GasTank}, or another block's gas
 * storage found with {@link GasTransfer#find}. Amounts are in millibuckets.
 *
 * <p>Use it on the server thread.
 */
public interface GasAccess {
    /**
     * Inserts up to {@code maxAmount} of a gas.
     *
     * @param simulate true to only ask how much would be accepted
     * @return the amount accepted
     */
    long insert(ResourceKey<Gas> gas, long maxAmount, boolean simulate);

    /**
     * Removes up to {@code maxAmount} of the first gas that matches {@code filter}.
     *
     * @param simulate true to only ask what would be removed
     * @return what was removed, or {@link GasStack#EMPTY}
     */
    GasStack extract(Predicate<ResourceKey<Gas>> filter, long maxAmount, boolean simulate);
}
