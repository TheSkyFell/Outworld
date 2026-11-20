package io.github.theskyfell.outworld.core.perf;

import java.util.Arrays;

/**
 * Time spent on one thing over the last {@link #TICKS} server ticks: a ring of per-tick totals. Time
 * added during a tick counts towards that tick once {@link Perf} closes it.
 */
public final class PerfWindow {
    /** Five seconds at 20 ticks per second. */
    public static final int TICKS = 100;

    private final long[] nanos = new long[TICKS];
    private final int[] calls = new int[TICKS];
    private int next;
    private int filled;
    private long tickNanos;
    private int tickCalls;

    void add(long elapsedNanos) {
        tickNanos += elapsedNanos;
        tickCalls++;
    }

    /** Closes the current tick and starts the next. */
    void roll() {
        nanos[next] = tickNanos;
        calls[next] = tickCalls;
        next = (next + 1) % TICKS;
        if (filled < TICKS) {
            filled++;
        }
        tickNanos = 0;
        tickCalls = 0;
    }

    void clear() {
        Arrays.fill(nanos, 0);
        Arrays.fill(calls, 0);
        next = 0;
        filled = 0;
        tickNanos = 0;
        tickCalls = 0;
    }

    /** Nanoseconds spent in the most recently closed tick, or 0 if none has closed. */
    public long lastNanos() {
        return filled == 0 ? 0 : nanos[(next + TICKS - 1) % TICKS];
    }

    /** How many closed ticks the window holds, up to {@link #TICKS}. */
    public int ticks() {
        return filled;
    }

    /** Average milliseconds per tick across the window, counting ticks where nothing ran as zero. */
    public double averageMillis() {
        if (filled == 0) {
            return 0;
        }
        long total = 0;
        for (int i = 0; i < filled; i++) {
            total += nanos[i];
        }
        return total / (double) filled / 1_000_000.0;
    }

    /** The most milliseconds spent in a single tick of the window. */
    public double maxMillis() {
        long max = 0;
        for (int i = 0; i < filled; i++) {
            max = Math.max(max, nanos[i]);
        }
        return max / 1_000_000.0;
    }

    /** All calls across the window. */
    public long totalCalls() {
        long total = 0;
        for (int i = 0; i < filled; i++) {
            total += calls[i];
        }
        return total;
    }

    /** Average calls per tick across the window. */
    public double averageCalls() {
        return filled == 0 ? 0 : totalCalls() / (double) filled;
    }

    /** Average microseconds per call across the window, or 0 if nothing was called. */
    public double microsPerCall() {
        long count = totalCalls();
        if (count == 0) {
            return 0;
        }
        long total = 0;
        for (int i = 0; i < filled; i++) {
            total += nanos[i];
        }
        return total / (double) count / 1_000.0;
    }
}
