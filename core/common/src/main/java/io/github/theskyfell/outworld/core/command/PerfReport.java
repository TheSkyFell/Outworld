package io.github.theskyfell.outworld.core.command;

import io.github.theskyfell.outworld.core.perf.Perf;
import io.github.theskyfell.outworld.core.perf.PerfSection;
import io.github.theskyfell.outworld.core.perf.PerfWindow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** The text of {@code /outworld perf}. */
final class PerfReport {
    /** Each section lists at most this many of its parts, costliest first. */
    static final int MAX_PARTS = 8;

    private PerfReport() {
    }

    /*
     * Example:
     * Outworld cost over the last 100 ticks:
     * Server tick: 4.12 ms average, 9.80 ms max
     * Outworld: 0.35 ms per tick, 8.5% of the server tick
     * outworld:machines: 0.35 ms average, 1.20 ms max
     *   outworld_industry:test_tank: 0.20 ms average, 0.90 ms max, 12.5 batches per tick, 16 µs each
     */
    static String build() {
        PerfWindow ticks = Perf.serverTicks();
        if (ticks.ticks() == 0) {
            return "No server ticks measured yet.";
        }
        List<String> lines = new ArrayList<>();
        lines.add("Outworld cost over the last " + ticks.ticks() + " ticks:");
        lines.add(String.format(Locale.ROOT, "Server tick: %.2f ms average, %.2f ms max",
                ticks.averageMillis(), ticks.maxMillis()));

        List<PerfSection> sections = Perf.sections();
        double total = 0;
        for (PerfSection section : sections) {
            total += section.window().averageMillis();
        }
        double share = ticks.averageMillis() > 0 ? 100 * total / ticks.averageMillis() : 0;
        lines.add(String.format(Locale.ROOT, "Outworld: %.2f ms per tick, %.1f%% of the server tick", total, share));

        for (PerfSection section : sections) {
            lines.add(describe(section, ""));
            List<PerfSection> parts = new ArrayList<>();
            for (PerfSection child : section.children()) {
                if (child.window().totalCalls() > 0) {
                    parts.add(child);
                }
            }
            parts.sort(Comparator.comparingDouble((PerfSection part) -> part.window().averageMillis()).reversed());
            for (int i = 0; i < Math.min(parts.size(), MAX_PARTS); i++) {
                lines.add(describe(parts.get(i), "  "));
            }
            if (parts.size() > MAX_PARTS) {
                lines.add("  ... and " + (parts.size() - MAX_PARTS) + " more");
            }
        }
        return String.join("\n", lines);
    }

    private static String describe(PerfSection section, String indent) {
        PerfWindow window = section.window();
        String line = String.format(Locale.ROOT, "%s%s: %.2f ms average, %.2f ms max",
                indent, section.id(), window.averageMillis(), window.maxMillis());
        if (section.callName() != null) {
            line += String.format(Locale.ROOT, ", %.1f %s per tick, %.0f µs each",
                    window.averageCalls(), section.callName(), window.microsPerCall());
        }
        return line;
    }
}
