package io.github.theskyfell.outworld.core.machine;

/**
 * An item that goes in a recipe machine's upgrade slots. Any Outworld mod can make one by having its
 * item implement this; {@link MachineUpgrades} works out what the upgrades in a machine do.
 */
public interface MachineUpgrade {
    /** What an upgrade does. */
    enum Kind {
        /** Each one runs recipes as fast again, and makes each recipe cost 25% more energy. */
        SPEED,
        /**
         * Each one makes a recipe cost 10% less energy, down to a fifth with eight; a generator makes
         * 10% more energy from each recipe instead.
         */
        EFFICIENCY
    }

    Kind upgradeKind();
}
