package io.github.theskyfell.outworld.core.machine;

import io.github.theskyfell.outworld.core.item.ItemBuffer;
import net.minecraft.world.item.ItemStack;

/**
 * A recipe machine's upgrade slots, and what the upgrades in them do. There are two slots, each
 * holding up to eight of one kind of {@link MachineUpgrade}; the two slots can't hold the same kind.
 *
 * <p>Hoppers, pipes and inserters never reach these slots: players put upgrades in through the
 * machine's screen.
 *
 * <p>With {@code s} speed and {@code e} efficiency upgrades, a recipe that takes {@code t} ticks and
 * {@code p} FE a tick takes {@code ceil(t / (1 + s))} ticks, and costs
 * {@code p × t × (1 + s/4) × (1 − e/10)} FE in all, spread over those ticks. A generator's recipe
 * makes {@code p × t × (1 + e/10)} FE in all instead, and speed only makes it burn faster.
 */
public final class MachineUpgrades extends ItemBuffer {
    public static final int SLOTS = 2;
    /** The most upgrades of one kind a machine takes. */
    public static final int MAX_PER_KIND = 8;

    public MachineUpgrades() {
        super(Role.INPUT, Role.INPUT);
    }

    /** Only upgrades, and not a kind the other slot already holds. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof MachineUpgrade upgrade)) {
            return false;
        }
        for (int other = 0; other < getContainerSize(); other++) {
            if (other != slot && getItem(other).getItem() instanceof MachineUpgrade held
                    && held.upgradeKind() == upgrade.upgradeKind()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return MAX_PER_KIND;
    }

    /** How many upgrades of this kind are in the slots, at most {@link #MAX_PER_KIND}. */
    public int count(MachineUpgrade.Kind kind) {
        int count = 0;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.getItem() instanceof MachineUpgrade upgrade && upgrade.upgradeKind() == kind) {
                count += stack.getCount();
            }
        }
        return Math.min(count, MAX_PER_KIND);
    }

    /** How many ticks a recipe that takes {@code baseTime} ticks takes with these upgrades. */
    public int time(int baseTime) {
        int speed = count(MachineUpgrade.Kind.SPEED);
        return Math.max(1, (baseTime + speed) / (1 + speed)); // rounded up
    }

    /**
     * The energy a recipe uses (or, for a generator, makes) each tick with these upgrades, for a
     * recipe that takes {@code baseTime} ticks and {@code basePerTick} FE a tick without them.
     */
    public long energyPerTick(long basePerTick, int baseTime, boolean generator) {
        int speed = count(MachineUpgrade.Kind.SPEED);
        int efficiency = count(MachineUpgrade.Kind.EFFICIENCY);
        if (basePerTick <= 0 || speed == 0 && efficiency == 0) {
            return basePerTick;
        }
        long total = basePerTick * baseTime;
        int time = time(baseTime);
        if (generator) {
            // Percent: 100 + 10 for each efficiency upgrade. Rounded down, so upgrades never make energy from nothing.
            return Math.max(1, total * (100 + 10L * efficiency) / (100L * time));
        }
        // Parts in 10,000: (100 + 25 per speed upgrade) × (100 − 10 per efficiency upgrade). Rounded up.
        long factor = (100 + 25L * speed) * (100 - 10L * efficiency);
        return Math.max(1, Math.ceilDiv(total * factor, 10_000L * time));
    }
}
