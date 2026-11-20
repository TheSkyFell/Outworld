package io.github.theskyfell.outworld.core;

/** Outworld Core's settings, in {@code config/outworld/core.json}. Defaults are the field values here. */
public final class CoreConfig {
    public Machines machines = new Machines();

    public static final class Machines {
        /**
         * How often machines do their work, in ticks (20 ticks = 1 second). Each batch does that many
         * ticks of work at once, so throughput is the same; higher values cost the server less but
         * react more slowly. 1 to 20.
         */
        public int workIntervalTicks = 4;
    }

    /** Replaces missing or out-of-range values with safe ones and logs what it changed. */
    void validate() {
        if (machines == null) {
            machines = new Machines();
        }
        if (machines.workIntervalTicks < 1 || machines.workIntervalTicks > 20) {
            int fixed = Math.clamp(machines.workIntervalTicks, 1, 20);
            OutworldCore.LOG.warn("machines.workIntervalTicks must be between 1 and 20; using {} instead of {}",
                    fixed, machines.workIntervalTicks);
            machines.workIntervalTicks = fixed;
        }
    }
}
