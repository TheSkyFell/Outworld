package io.github.theskyfell.outworld.core.energy;

import io.github.theskyfell.outworld.core.OutworldCore;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The energy grids of each level: groups of touching {@link EnergyConductor}s that share one energy
 * buffer. A grid is found (by flood fill) the first time something asks for it, and kept until a
 * conductor in or next to it changes or one of its chunks loads or unloads; nothing is searched on a
 * timer. When a grid is dropped, its energy is shared out over its conductors and handed to whichever
 * grids they join next, so building onto a grid or cutting it in two loses nothing.
 *
 * <p>Energy in grids isn't saved: it is lost when the server stops. Server thread only.
 */
public final class EnergyGrids {
    /** The most conductors one grid may have. A bigger network splits into several grids. */
    public static final int MAX_GRID_SIZE = 65_536;

    private static final Map<ServerLevel, EnergyGrids> LEVELS = new IdentityHashMap<>();

    private final ServerLevel level;
    private final Long2ObjectOpenHashMap<Grid> byPos = new Long2ObjectOpenHashMap<>();
    /** Grids by every chunk they touch or border, for chunk loads and unloads. */
    private final Long2ObjectOpenHashMap<List<Grid>> byChunk = new Long2ObjectOpenHashMap<>();
    /** Energy left by dropped grids, per conductor, waiting for that conductor's next grid. */
    private final Long2LongOpenHashMap leftover = new Long2LongOpenHashMap();

    private static final class Grid {
        final long[] positions;
        final long[] chunks;
        final EnergyBuffer buffer;

        Grid(long[] positions, long[] chunks, EnergyBuffer buffer) {
            this.positions = positions;
            this.chunks = chunks;
            this.buffer = buffer;
        }
    }

    private EnergyGrids(ServerLevel level) {
        this.level = level;
    }

    /**
     * The shared buffer of the grid the conductor at {@code pos} belongs to, or null if there is no
     * conductor there (or its chunk isn't loaded).
     */
    public static EnergyBuffer bufferAt(ServerLevel level, BlockPos pos) {
        if (!level.getServer().isSameThread() || !level.isLoaded(pos)) {
            return null;
        }
        EnergyGrids grids = LEVELS.computeIfAbsent(level, EnergyGrids::new);
        Grid grid = grids.byPos.get(pos.asLong());
        if (grid == null) {
            grid = grids.build(pos);
        }
        return grid == null ? null : grid.buffer;
    }

    /** A block changed at {@code pos}. If a conductor came or went, the grids it touches are dropped. */
    public static void blockChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        boolean conductorBefore = oldState.getBlock() instanceof EnergyConductor;
        boolean conductorAfter = newState.getBlock() instanceof EnergyConductor;
        if (conductorBefore == conductorAfter && (!conductorBefore || oldState.getBlock() == newState.getBlock())) {
            return; // not a conductor change (a cable's connections changing doesn't matter)
        }
        if (!level.getServer().isSameThread()) {
            BlockPos immutable = pos.immutable();
            level.getServer().execute(() -> blockChanged(level, immutable, oldState, newState));
            return;
        }
        EnergyGrids grids = LEVELS.get(level);
        if (grids == null) {
            return;
        }
        grids.drop(pos.asLong());
        for (Direction direction : Direction.values()) {
            grids.drop(pos.relative(direction).asLong());
        }
        if (!conductorAfter) {
            grids.leftover.remove(pos.asLong()); // the removed conductor's share goes with it
        }
    }

    /** A chunk loaded or unloaded: grids in or next to it may have grown or shrunk. */
    public static void chunkChanged(ServerLevel level, int chunkX, int chunkZ) {
        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> chunkChanged(level, chunkX, chunkZ));
            return;
        }
        EnergyGrids grids = LEVELS.get(level);
        if (grids == null) {
            return;
        }
        List<Grid> touching = grids.byChunk.get(chunkKey(chunkX, chunkZ));
        if (touching != null) {
            for (Grid grid : List.copyOf(touching)) {
                grids.drop(grid);
            }
        }
    }

    /** Forgets every level. Called when the server stops. */
    public static void clear() {
        LEVELS.clear();
    }

    /** How many grids and conductors are known in a level, for debugging. */
    public static int[] stats(ServerLevel level) {
        EnergyGrids grids = LEVELS.get(level);
        if (grids == null) {
            return new int[] {0, 0};
        }
        int count = (int) grids.byPos.values().stream().distinct().count();
        return new int[] {count, grids.byPos.size()};
    }

    private Grid build(BlockPos start) {
        if (!(level.getBlockState(start).getBlock() instanceof EnergyConductor)) {
            return null;
        }
        LongOpenHashSet visited = new LongOpenHashSet();
        LongOpenHashSet chunks = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        List<Long> found = new ArrayList<>();
        long capacity = 0;
        queue.enqueue(start.asLong());
        visited.add(start.asLong());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty() && found.size() < MAX_GRID_SIZE) {
            long packed = queue.dequeueLong();
            cursor.set(packed);
            if (!level.isLoaded(cursor)) {
                chunks.add(chunkKey(cursor.getX() >> 4, cursor.getZ() >> 4)); // the grid may grow when it loads
                continue;
            }
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof EnergyConductor conductor) || byPos.containsKey(packed)) {
                continue;
            }
            found.add(packed);
            capacity += conductor.gridCapacity();
            chunks.add(chunkKey(cursor.getX() >> 4, cursor.getZ() >> 4));
            for (Direction direction : Direction.values()) {
                long next = BlockPos.offset(packed, direction);
                if (visited.add(next)) {
                    queue.enqueue(next);
                }
            }
        }
        long energy = 0;
        long[] positions = new long[found.size()];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = found.get(i);
            energy += leftover.remove(positions[i]);
        }
        EnergyBuffer buffer = new EnergyBuffer(capacity, capacity, capacity);
        buffer.setAmount(Math.min(energy, capacity));
        Grid grid = new Grid(positions, chunks.toLongArray(), buffer);
        for (long pos : positions) {
            byPos.put(pos, grid);
        }
        for (long chunk : grid.chunks) {
            List<Grid> list = byChunk.get(chunk);
            if (list == null) {
                list = new ArrayList<>(1);
                byChunk.put(chunk, list);
            }
            list.add(grid);
        }
        if (positions.length >= MAX_GRID_SIZE) {
            OutworldCore.LOG.warn("An energy grid at {} has over {} conductors; it is split into several", start, MAX_GRID_SIZE);
        }
        return grid;
    }

    private void drop(long pos) {
        Grid grid = byPos.get(pos);
        if (grid != null) {
            drop(grid);
        }
    }

    private void drop(Grid grid) {
        long amount = grid.buffer.getAmount();
        int size = grid.positions.length;
        for (int i = 0; i < size; i++) {
            long pos = grid.positions[i];
            byPos.remove(pos);
            // Share the energy out over the conductors, the remainder to the first.
            long share = amount / size + (i == 0 ? amount % size : 0);
            if (share > 0) {
                leftover.addTo(pos, share);
            }
        }
        for (long chunk : grid.chunks) {
            List<Grid> list = byChunk.get(chunk);
            if (list != null) {
                list.remove(grid);
                if (list.isEmpty()) {
                    byChunk.remove(chunk);
                }
            }
        }
        grid.buffer.setAmount(0); // anyone still holding the old buffer finds it empty
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
