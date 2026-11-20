package io.github.theskyfell.outworld.core.machine;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.perf.Perf;
import io.github.theskyfell.outworld.core.perf.PerfSection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Runs the awake machines of one level. Driven by the level tick (see {@code CoreModule}); all methods
 * run on the server thread.
 *
 * <p>Awake machines sit in a queue ordered by the tick they are due. Each tick only the machines that
 * are due are touched, so sleeping machines and machines between batches cost nothing.
 *
 * <p>Batches are timed for {@code /outworld perf}: all together under {@code outworld:machines}, and
 * per block entity type below it.
 */
public final class MachineScheduler {
    private static final Map<ServerLevel, MachineScheduler> SCHEDULERS = new IdentityHashMap<>();
    private static final PerfSection PERF = Perf.section(OutworldCore.MOD_ID + ":machines", null);

    private final ServerLevel level;
    private final Set<MachineBlockEntity> loaded = Collections.newSetFromMap(new IdentityHashMap<>());
    private final PriorityQueue<Entry> queue = new PriorityQueue<>(Comparator.comparingLong(Entry::due));
    private long batches;

    private MachineScheduler(ServerLevel level) {
        this.level = level;
    }

    static MachineScheduler of(ServerLevel level) {
        return SCHEDULERS.computeIfAbsent(level, MachineScheduler::new);
    }

    /** Runs the machines due in {@code level}. Called at the end of every level tick. */
    public static void tick(ServerLevel level) {
        MachineScheduler scheduler = SCHEDULERS.get(level);
        if (scheduler != null) {
            scheduler.runDue();
        }
    }

    /** Forgets every level. Called when the server stops. */
    public static void clear() {
        SCHEDULERS.clear();
    }

    /** A snapshot of every level that has machines, for commands and debugging. */
    public static List<Stats> stats() {
        List<Stats> result = new ArrayList<>();
        for (MachineScheduler scheduler : SCHEDULERS.values()) {
            int awake = 0;
            for (MachineBlockEntity machine : scheduler.loaded) {
                if (machine.isAwake()) {
                    awake++;
                }
            }
            result.add(new Stats(scheduler.level, scheduler.loaded.size(), awake, scheduler.batches));
        }
        return result;
    }

    void add(MachineBlockEntity machine) {
        loaded.add(machine);
    }

    /**
     * Forgets a removed or unloaded machine. Never creates a scheduler, so machines unloading while the
     * server shuts down don't keep the old level alive. Any queued entry for the machine is now stale
     * and is skipped when it comes up.
     */
    static void removeIfPresent(ServerLevel level, MachineBlockEntity machine) {
        MachineScheduler scheduler = SCHEDULERS.get(level);
        if (scheduler != null) {
            scheduler.loaded.remove(machine);
        }
    }

    void schedule(MachineBlockEntity machine, long due) {
        machine.setScheduledTime(due);
        queue.add(new Entry(due, machine));
    }

    void countBatch() {
        batches++;
    }

    /** The section that times one machine type's batches. */
    static PerfSection perfSection(BlockEntityType<?> type) {
        return Perf.subsection(PERF, String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type)), "batches");
    }

    private void runDue() {
        long now = level.getGameTime();
        if (queue.isEmpty() || queue.peek().due() > now) {
            return;
        }
        try (var _ = PERF.start()) {
            while (!queue.isEmpty() && queue.peek().due() <= now) {
                Entry entry = queue.poll();
                MachineBlockEntity machine = entry.machine();
                if (machine.scheduledTime() != entry.due() || machine.isRemoved()) {
                    continue; // stale: the machine was removed, or rescheduled since
                }
                try (var _ = machine.perfSection().start()) {
                    machine.runBatch(level, now, this);
                }
            }
        }
    }

    private record Entry(long due, MachineBlockEntity machine) {
    }

    public record Stats(ServerLevel level, int loaded, int awake, long batches) {
    }
}
