package io.github.theskyfell.outworld.core.energy;

/**
 * Something energy can be moved into or out of: an Outworld {@link EnergyBuffer}, or another mod's
 * energy storage found with {@link Energy#find}. Amounts are in FE.
 *
 * <p>Use it on the server thread, outside of any loader transaction.
 */
public interface EnergyAccess {
    /**
     * Adds up to {@code maxAmount} FE.
     *
     * @param simulate true to only ask how much would be accepted
     * @return the amount accepted
     */
    long insert(long maxAmount, boolean simulate);

    /**
     * Removes up to {@code maxAmount} FE.
     *
     * @param simulate true to only ask how much would be removed
     * @return the amount removed
     */
    long extract(long maxAmount, boolean simulate);

    long getAmount();

    long getCapacity();
}
