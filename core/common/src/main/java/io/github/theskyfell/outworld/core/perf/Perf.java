package io.github.theskyfell.outworld.core.perf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Measures what Outworld costs the server, for {@code /outworld perf}. Each system (machines,
 * logistics, habitats) times its work under its own {@link PerfSection}; Core times whole server ticks
 * to compare against. Everything is averaged over the last {@link PerfWindow#TICKS} ticks.
 *
 * <p>Top-level sections must not run inside each other, or their time would be counted twice in the
 * total. Use {@link #subsection} to break a section down.
 */
public final class Perf {
    private static final List<PerfSection> SECTIONS = new ArrayList<>();
    private static final PerfWindow SERVER_TICK = new PerfWindow();
    private static final List<Runnable> TICK_LISTENERS = new ArrayList<>();
    private static long tickStartNanos = -1;
    private static long tickEndNanos = -1;

    private Perf() {
    }

    /**
     * The top-level section with this id, created on first use. Ids look like {@code modid:system},
     * such as {@code outworld:machines}.
     *
     * @param callName what one call counts (see {@link PerfSection#callName()}), or null
     */
    public static synchronized PerfSection section(String id, String callName) {
        for (PerfSection section : SECTIONS) {
            if (section.id().equals(id)) {
                return section;
            }
        }
        PerfSection section = new PerfSection(id, callName);
        SECTIONS.add(section);
        return section;
    }

    /** A part of {@code parent}, created on first use. Its time is also counted in the parent's. */
    public static synchronized PerfSection subsection(PerfSection parent, String id, String callName) {
        return parent.child(id, callName);
    }

    /** Every top-level section, in the order they were created. */
    public static synchronized List<PerfSection> sections() {
        return Collections.unmodifiableList(new ArrayList<>(SECTIONS));
    }

    /**
     * Runs {@code listener} each time a tick's measurements are closed (at the start of the next tick),
     * when each window's {@link PerfWindow#lastNanos()} holds that tick. Server thread only.
     */
    public static synchronized void onTickClosed(Runnable listener) {
        TICK_LISTENERS.add(listener);
    }

    /** Whole server ticks, from the start of the tick to the end of it. */
    public static PerfWindow serverTicks() {
        return SERVER_TICK;
    }

    /**
     * Called by Core at the start of every server tick. Closes the previous tick, so work done at its
     * very end (after Core's own end-of-tick listener) still counts towards it.
     */
    public static synchronized void tickStarted() {
        long now = System.nanoTime();
        if (tickStartNanos >= 0 && tickEndNanos >= tickStartNanos) {
            SERVER_TICK.add(tickEndNanos - tickStartNanos);
            SERVER_TICK.roll();
            for (PerfSection section : SECTIONS) {
                section.roll();
            }
            for (Runnable listener : TICK_LISTENERS) {
                listener.run();
            }
        }
        tickStartNanos = now;
    }

    /** Called by Core at the end of every server tick. */
    public static void tickEnded() {
        tickEndNanos = System.nanoTime();
    }

    /** Forgets all measurements. Called by Core when the server stops. */
    public static synchronized void clear() {
        SERVER_TICK.clear();
        for (PerfSection section : SECTIONS) {
            section.clear();
        }
        tickStartNanos = -1;
        tickEndNanos = -1;
    }
}
