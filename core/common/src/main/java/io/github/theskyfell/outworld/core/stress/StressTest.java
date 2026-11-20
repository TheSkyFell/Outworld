package io.github.theskyfell.outworld.core.stress;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.machine.MachineScheduler;
import io.github.theskyfell.outworld.core.perf.Perf;
import io.github.theskyfell.outworld.core.perf.PerfSection;
import io.github.theskyfell.outworld.core.platform.Services;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Runs one stress scenario and measures it: builds it in kept-loaded chunks, lets it warm up, measures
 * Outworld's cost per tick under load, makes it go quiet, then measures the cost again once everything
 * is asleep. The result goes to the log, to {@code /outworld stress status}, and to a JSON report in
 * {@code outworld-stress/} in the game folder.
 *
 * <p>Outworld's cost is the time spent in every {@link Perf} section; the budget is
 * {@link #BUDGET_MS} per tick under load, from the v1 scope. Server thread only; one test at a time.
 */
public final class StressTest {
    /** The v1 performance budget: the whole mod under this many milliseconds per server tick. */
    public static final double BUDGET_MS = 5.0;

    static final int WARMUP_TICKS = 100;
    static final int ACTIVE_TICKS = 600;
    /** Long enough for every test machine to finish the job it is on (100 ticks of work). */
    static final int SETTLE_TICKS = 140;
    static final int IDLE_TICKS = 200;

    private enum Phase {
        WARMUP("warming up"),
        ACTIVE("measuring under load"),
        SETTLE("letting work finish"),
        IDLE("measuring idle"),
        DONE("done"),
        STOPPED("stopped");

        final String label;

        Phase(String label) {
            this.label = label;
        }
    }

    private static StressTest current;

    private final String name;
    private final int count;
    private final ServerLevel level;
    private final BoundingBox area;
    private final List<int[]> forcedChunks;
    private final StressRun run;
    private final Recorder active = new Recorder(ACTIVE_TICKS);
    private final Recorder idle = new Recorder(IDLE_TICKS);
    private Phase phase = Phase.WARMUP;
    private int ticks;
    private String summary;
    private Path reportPath;

    private StressTest(String name, int count, ServerLevel level, BoundingBox area, List<int[]> forcedChunks, StressRun run) {
        this.name = name;
        this.count = count;
        this.level = level;
        this.area = area;
        this.forcedChunks = forcedChunks;
        this.run = run;
    }

    /** Called once by Core: stress tests advance as each tick's measurements close. */
    public static void init() {
        Perf.onTickClosed(StressTest::tickClosed);
    }

    /** Forgets the current test without touching the world. Called by Core when the server stops. */
    public static void reset() {
        current = null;
    }

    /**
     * Builds a scenario and starts measuring it.
     *
     * @return a message for the player
     * @throws IllegalStateException with a message for the player, if it can't start
     */
    public static String start(ServerLevel level, String name, Integer requestedCount, BlockPos origin) {
        if (current != null && current.isRunning()) {
            throw new IllegalStateException("A stress test is already running: " + current.progress());
        }
        if (current != null) {
            throw new IllegalStateException("Clear the last stress test's area first, with /outworld stress clear");
        }
        StressScenario scenario = StressScenarios.get(name);
        if (scenario == null) {
            throw new IllegalStateException("No stress scenario called " + name + ". See /outworld stress list");
        }
        int count = requestedCount != null ? requestedCount : scenario.defaultCount();
        BoundingBox area = scenario.area(origin, count);

        // Keep the area loaded (and ticking) for the whole test, then build.
        List<int[]> forced = new ArrayList<>();
        for (int x = area.minX() >> 4; x <= area.maxX() >> 4; x++) {
            for (int z = area.minZ() >> 4; z <= area.maxZ() >> 4; z++) {
                if (level.setChunkForced(x, z, true)) {
                    forced.add(new int[] {x, z});
                }
            }
        }
        StressRun run = scenario.build(level, origin, count);
        current = new StressTest(name, count, level, area, forced, run);
        OutworldCore.LOG.info("Stress test {} x{} built at {}, {}, {}", name, count, origin.getX(), origin.getY(), origin.getZ());
        return String.format(Locale.ROOT,
                "Built %s x%d. Measuring: %d s warm-up, %d s under load, then %d s idle. See /outworld stress status",
                name, count, WARMUP_TICKS / 20, ACTIVE_TICKS / 20, IDLE_TICKS / 20);
    }

    /** The running test's progress, or the last test's result. */
    public static String status() {
        if (current == null) {
            return "No stress test has run since the last clear. See /outworld stress list";
        }
        if (current.phase == Phase.DONE) {
            return current.summary + (current.reportPath != null ? "\nReport: " + current.reportPath : "");
        }
        return current.progress();
    }

    /** Stops measuring and leaves the built area as it is. */
    public static String stop() {
        if (current == null || !current.isRunning()) {
            return "No stress test is running";
        }
        current.phase = Phase.STOPPED;
        current.run.quiet(current.level);
        return "Stopped. The area is still there: /outworld stress clear removes it";
    }

    /** Stops any test, removes everything the last test built, and lets its chunks unload. */
    public static String clear() {
        if (current == null) {
            return "There is no stress test area to clear";
        }
        StressTest test = current;
        current = null;
        int removed = 0;
        BoundingBox area = test.area;
        for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
            if (!test.level.getBlockState(pos).isAir()) {
                // Clients only: no neighbour updates, so nothing wakes while the area empties.
                test.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                removed++;
            }
        }
        for (int[] chunk : test.forcedChunks) {
            test.level.setChunkForced(chunk[0], chunk[1], false);
        }
        return "Cleared the " + test.name + " stress test area (" + removed + " blocks)";
    }

    /** One line per registered scenario. */
    public static String list() {
        if (StressScenarios.all().isEmpty()) {
            return "No stress scenarios are registered";
        }
        StringBuilder text = new StringBuilder("Stress scenarios (/outworld stress start <name> [count] [x y z]):");
        StressScenarios.all().forEach((name, scenario) -> text.append("\n  ").append(name)
                .append(" (").append(scenario.defaultCount()).append("): ").append(scenario.description()));
        return text.toString();
    }

    private boolean isRunning() {
        return phase != Phase.DONE && phase != Phase.STOPPED;
    }

    private String progress() {
        int length = switch (phase) {
            case WARMUP -> WARMUP_TICKS;
            case ACTIVE -> ACTIVE_TICKS;
            case SETTLE -> SETTLE_TICKS;
            case IDLE -> IDLE_TICKS;
            default -> 0;
        };
        String header = "Stress test " + name + " x" + count + ": " + phase.label;
        return length > 0 ? header + ", " + ticks + "/" + length + " ticks" : header;
    }

    private static void tickClosed() {
        StressTest test = current;
        if (test == null || !test.isRunning()) {
            return;
        }
        test.ticks++;
        switch (test.phase) {
            case WARMUP -> {
                if (test.ticks >= WARMUP_TICKS) {
                    test.next(Phase.ACTIVE);
                }
            }
            case ACTIVE -> {
                test.active.record(test.awake());
                if (test.ticks >= ACTIVE_TICKS) {
                    test.run.quiet(test.level);
                    test.next(Phase.SETTLE);
                }
            }
            case SETTLE -> {
                if (test.ticks >= SETTLE_TICKS) {
                    test.next(Phase.IDLE);
                }
            }
            case IDLE -> {
                test.idle.record(test.awake());
                if (test.ticks >= IDLE_TICKS) {
                    test.finish();
                }
            }
            default -> {
            }
        }
    }

    private void next(Phase phase) {
        this.phase = phase;
        ticks = 0;
    }

    private int awake() {
        int awake = 0;
        for (MachineScheduler.Stats stats : MachineScheduler.stats()) {
            if (stats.level() == level) {
                awake += stats.awake();
            }
        }
        return awake;
    }

    private void finish() {
        phase = Phase.DONE;
        boolean withinBudget = active.outworldAverage() <= BUDGET_MS;
        summary = String.format(Locale.ROOT,
                "Stress test %s x%d on %s: under load %.2f ms per tick (p95 %.2f, max %.2f), %.1f%% of a %.2f ms server tick, "
                        + "%d machines awake; idle %.2f ms per tick, %d awake. Budget %.0f ms: %s",
                name, count, Services.PLATFORM.getPlatformName(),
                active.outworldAverage(), active.outworldPercentile(0.95), active.outworldMax(),
                active.outworldShare(), active.serverAverage(), active.lastAwake,
                idle.outworldAverage(), idle.lastAwake,
                BUDGET_MS, withinBudget ? "within budget" : "OVER BUDGET");
        OutworldCore.LOG.info(summary);

        JsonObject report = new JsonObject();
        report.addProperty("scenario", name);
        report.addProperty("count", count);
        report.addProperty("loader", Services.PLATFORM.getPlatformName());
        report.addProperty("minecraft", SharedConstants.getCurrentVersion().name());
        report.addProperty("outworld", Services.PLATFORM.getModVersion(OutworldCore.MOD_ID).orElse("unknown"));
        report.addProperty("budget_ms", BUDGET_MS);
        report.addProperty("within_budget", withinBudget);
        report.add("active", active.toJson());
        report.add("idle", idle.toJson());
        report.addProperty("summary", summary);
        try {
            Path folder = Services.PLATFORM.getConfigDirectory().toAbsolutePath().getParent().resolve("outworld-stress");
            Files.createDirectories(folder);
            reportPath = folder.resolve(name + ".json");
            Files.writeString(reportPath, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (IOException e) {
            OutworldCore.LOG.warn("Couldn't write the stress test report", e);
            reportPath = null;
        }
    }

    /** Per-tick measurements of one phase. */
    private static final class Recorder {
        private final long[] outworld;
        private final long[] server;
        private final Map<String, long[]> sections = new LinkedHashMap<>();
        private int ticks;
        private int lastAwake;

        Recorder(int length) {
            outworld = new long[length];
            server = new long[length];
        }

        void record(int awake) {
            if (ticks >= outworld.length) {
                return;
            }
            long total = 0;
            for (PerfSection section : Perf.sections()) {
                long nanos = section.window().lastNanos();
                total += nanos;
                add(section.id(), nanos);
                for (PerfSection part : section.children()) {
                    add(part.id(), part.window().lastNanos());
                }
            }
            outworld[ticks] = total;
            server[ticks] = Perf.serverTicks().lastNanos();
            ticks++;
            lastAwake = awake;
        }

        private void add(String id, long nanos) {
            long[] totals = sections.computeIfAbsent(id, key -> new long[2]);
            totals[0] += nanos;
            totals[1] = Math.max(totals[1], nanos);
        }

        double outworldAverage() {
            return average(outworld);
        }

        double outworldMax() {
            return max(outworld);
        }

        double outworldPercentile(double fraction) {
            return percentile(outworld, fraction);
        }

        double serverAverage() {
            return average(server);
        }

        double outworldShare() {
            double serverAverage = serverAverage();
            return serverAverage > 0 ? 100 * outworldAverage() / serverAverage : 0;
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("ticks", ticks);
            json.addProperty("machines_awake", lastAwake);
            json.add("outworld_ms", stats(outworld));
            json.add("server_tick_ms", stats(server));
            json.addProperty("outworld_share_percent", round(outworldShare()));
            JsonObject parts = new JsonObject();
            sections.forEach((id, totals) -> {
                JsonObject part = new JsonObject();
                part.addProperty("avg", round(ticks == 0 ? 0 : totals[0] / (double) ticks / 1_000_000.0));
                part.addProperty("max", round(totals[1] / 1_000_000.0));
                parts.add(id, part);
            });
            json.add("sections_ms", parts);
            return json;
        }

        private JsonObject stats(long[] values) {
            JsonObject json = new JsonObject();
            json.addProperty("avg", round(average(values)));
            json.addProperty("p95", round(percentile(values, 0.95)));
            json.addProperty("max", round(max(values)));
            return json;
        }

        private double average(long[] values) {
            if (ticks == 0) {
                return 0;
            }
            long total = 0;
            for (int i = 0; i < ticks; i++) {
                total += values[i];
            }
            return total / (double) ticks / 1_000_000.0;
        }

        private double max(long[] values) {
            long max = 0;
            for (int i = 0; i < ticks; i++) {
                max = Math.max(max, values[i]);
            }
            return max / 1_000_000.0;
        }

        private double percentile(long[] values, double fraction) {
            if (ticks == 0) {
                return 0;
            }
            long[] sorted = Arrays.copyOf(values, ticks);
            Arrays.sort(sorted);
            int index = Math.min(ticks - 1, (int) Math.ceil(fraction * ticks) - 1);
            return sorted[Math.max(0, index)] / 1_000_000.0;
        }

        private static double round(double value) {
            return Math.round(value * 1000) / 1000.0;
        }
    }
}
