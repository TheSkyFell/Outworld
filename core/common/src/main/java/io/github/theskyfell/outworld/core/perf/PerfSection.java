package io.github.theskyfell.outworld.core.perf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.profiling.Profiler;

/**
 * A named part of Outworld's server work whose cost {@code /outworld perf} reports. Get one from
 * {@link Perf#section}, keep it in a static field, and time the work with try-with-resources:
 *
 * <pre>{@code
 * try (var _ = NETWORKS.start()) {
 *     // work
 * }
 * }</pre>
 *
 * <p>Timed work also shows up as a section of the same name in Minecraft's own profiler
 * ({@code /debug start}, {@code /jfr}). Starting and stopping costs a few tens of nanoseconds.
 *
 * <p>Server thread only. A section may be started again inside itself; only the outermost start is
 * timed.
 */
public final class PerfSection implements AutoCloseable {
    private final String id;
    private final String callName;
    private final PerfWindow window = new PerfWindow();
    private final List<PerfSection> children = new ArrayList<>();
    private int depth;
    private long startNanos;

    PerfSection(String id, String callName) {
        this.id = id;
        this.callName = callName;
    }

    /** Starts timing. Close the returned section (this one) to stop. */
    public PerfSection start() {
        if (depth++ == 0) {
            Profiler.get().push(id);
            startNanos = System.nanoTime();
        }
        return this;
    }

    /** Stops timing. */
    @Override
    public void close() {
        if (depth == 0) {
            return; // closed more often than started: ignore rather than corrupt the profiler
        }
        if (--depth == 0) {
            window.add(System.nanoTime() - startNanos);
            Profiler.get().pop();
        }
    }

    public String id() {
        return id;
    }

    /**
     * What one call counts, such as "batches" for a machine type, or null if the number of calls
     * says nothing useful (a section entered once per level tick, say).
     */
    public String callName() {
        return callName;
    }

    public PerfWindow window() {
        return window;
    }

    /** The parts this section is broken down into, if any. */
    public List<PerfSection> children() {
        return Collections.unmodifiableList(children);
    }

    PerfSection child(String childId, String childCallName) {
        for (PerfSection child : children) {
            if (child.id.equals(childId)) {
                return child;
            }
        }
        PerfSection child = new PerfSection(childId, childCallName);
        children.add(child);
        return child;
    }

    void roll() {
        window.roll();
        for (PerfSection child : children) {
            child.roll();
        }
    }

    void clear() {
        window.clear();
        depth = 0;
        for (PerfSection child : children) {
            child.clear();
        }
    }
}
