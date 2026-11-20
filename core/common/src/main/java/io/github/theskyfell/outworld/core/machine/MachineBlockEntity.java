package io.github.theskyfell.outworld.core.machine;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.perf.PerfSection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The base of every Outworld machine. Machines never tick every tick. Instead:
 *
 * <ul>
 *   <li><b>Batches:</b> an awake machine works once every {@link #workIntervalTicks()} ticks, and
 *       {@link #work} is told how many ticks have passed, so it does that much work at once. The
 *       throughput is the same as ticking every tick, at a fraction of the cost.</li>
 *   <li><b>Sleep:</b> when {@link #work} returns false, the machine sleeps and costs nothing until
 *       {@link #wake()} is called. Call it whenever something could give the machine work: an item
 *       was inserted, power arrived, a setting changed. Neighbour changes wake it automatically (see
 *       {@link MachineBlock}).</li>
 *   <li><b>Spread:</b> each machine has a fixed slot within the interval, based on its position, so
 *       many machines don't all work on the same tick.</li>
 * </ul>
 *
 * <p>Machines wake once when they load or are placed, so they can check whether they have work. Time
 * spent asleep, or in a chunk that isn't ticking, is never counted as elapsed time.
 */
public abstract class MachineBlockEntity extends BlockEntity {
    /** The most ticks one batch can cover. Keeps a single batch's work bounded. */
    public static final int MAX_ELAPSED_TICKS = 200;

    static final long ASLEEP = -1;

    private long scheduledTime = ASLEEP;
    private long lastWorkTime;
    private boolean working;
    private PerfSection perfSection;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Does {@code elapsedTicks} ticks' worth of work at once. Runs on the server thread.
     *
     * @param elapsedTicks ticks since the last batch, or since the machine woke: 1 to {@link #MAX_ELAPSED_TICKS}
     * @return true to stay awake, false to sleep until {@link #wake()} is called
     */
    protected abstract boolean work(ServerLevel level, int elapsedTicks);

    /** Ticks between batches while awake. Defaults to Core's {@code machines.workIntervalTicks} setting. */
    protected int workIntervalTicks() {
        return OutworldCore.config().machines.workIntervalTicks;
    }

    /**
     * Schedules the machine to work at its next slot. Safe to call from any thread, and when already
     * awake. Calls made by the machine's own {@link #work} (for example through its own energy buffer's
     * change listener) are ignored: the return value of {@code work} decides whether it stays awake.
     */
    public final void wake() {
        if (!(level instanceof ServerLevel serverLevel) || isRemoved() || working) {
            return;
        }
        MinecraftServer server = serverLevel.getServer();
        if (!server.isSameThread()) {
            server.execute(this::wake);
            return;
        }
        if (scheduledTime != ASLEEP) {
            return;
        }
        long now = serverLevel.getGameTime();
        lastWorkTime = now;
        MachineScheduler.of(serverLevel).schedule(this, nextSlot(now));
    }

    public final boolean isAwake() {
        return scheduledTime != ASLEEP;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel serverLevel) {
            onServerThread(serverLevel, () -> {
                MachineScheduler.of(serverLevel).add(this);
                wake();
            });
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        scheduledTime = ASLEEP;
        if (level instanceof ServerLevel serverLevel) {
            onServerThread(serverLevel, () -> MachineScheduler.removeIfPresent(serverLevel, this));
        }
    }

    /** The section this machine's batches are timed under: one per block entity type. */
    final PerfSection perfSection() {
        if (perfSection == null) {
            perfSection = MachineScheduler.perfSection(getType());
        }
        return perfSection;
    }

    /** Called by the scheduler when this machine's slot comes up. */
    final void runBatch(ServerLevel serverLevel, long now, MachineScheduler scheduler) {
        scheduledTime = ASLEEP;
        if (!serverLevel.shouldTickBlocksAt(worldPosition)) {
            // The chunk is loaded but not ticking (e.g. at the edge of view). Try again next slot
            // without counting this time as work.
            lastWorkTime = now;
            scheduler.schedule(this, nextSlot(now));
            return;
        }
        int elapsed = (int) Math.clamp(now - lastWorkTime, 1, MAX_ELAPSED_TICKS);
        lastWorkTime = now;
        boolean stayAwake;
        working = true;
        try {
            stayAwake = work(serverLevel, elapsed);
        } finally {
            working = false;
        }
        scheduler.countBatch();
        if (stayAwake && scheduledTime == ASLEEP && !isRemoved()) {
            scheduler.schedule(this, nextSlot(now));
        }
    }

    long scheduledTime() {
        return scheduledTime;
    }

    void setScheduledTime(long time) {
        scheduledTime = time;
    }

    /** The first tick after {@code now} that falls on this machine's slot. */
    private long nextSlot(long now) {
        int interval = Math.max(1, workIntervalTicks());
        // Scramble the position so neighbouring machines get different slots.
        long hash = worldPosition.asLong();
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        int slot = Math.floorMod(hash, interval);
        long next = now + 1;
        return next + Math.floorMod(slot - next, interval);
    }

    /** Runs {@code action} now if this is the server thread, otherwise queues it for the server thread. */
    protected static void onServerThread(ServerLevel level, Runnable action) {
        MinecraftServer server = level.getServer();
        if (server.isSameThread()) {
            action.run();
        } else {
            server.execute(action);
        }
    }
}
