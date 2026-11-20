package io.github.theskyfell.outworld.core.grid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.perf.Perf;
import io.github.theskyfell.outworld.core.perf.PerfSection;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The grids of one {@link GridKind} in every level: groups of touching blocks of that kind that share
 * one storage.
 *
 * <ul>
 *   <li>A grid is found (by flood fill) the first time something asks for it, and kept until a block
 *       in or next to it changes or one of its chunks loads or unloads; a grid that held something
 *       is then found again at the end of the tick. Nothing is searched on a timer, and a grid with
 *       nothing moving costs nothing.
 *   <li>When something is put into a grid, or a grid holding something is found again, the grid
 *       offers its contents to the storage next to it (machines, batteries, other mods' blocks) at
 *       the end of that tick, starting with a different neighbour each time so they share fairly.
 *       Neighbours take from a grid themselves too.
 *   <li>When a grid is dropped, its contents are shared out over its blocks and handed to whichever
 *       grids they join next, so building onto a grid or cutting it in two loses nothing. A block
 *       that is removed takes its share with it.
 *   <li>Contents are saved with the level, so tanks and pipes keep what they hold.
 * </ul>
 *
 * <p>Server thread only.
 *
 * @param <S> the shared storage
 * @param <P> what a storage holds
 */
public final class Grids<S, P> {
    /** The most blocks one grid may have. A bigger network splits into several grids. */
    public static final int MAX_GRID_SIZE = 65_536;

    private static final List<Grids<?, ?>> ALL = new CopyOnWriteArrayList<>();
    private static final PerfSection PERF = Perf.section(OutworldCore.MOD_ID + ":grids", null);

    private final GridKind<S, P> kind;
    private final SavedDataType<Saved<S, P>> savedType;
    private final Map<ServerLevel, LevelGrids> levels = new IdentityHashMap<>();

    private Grids(GridKind<S, P> kind) {
        this.kind = kind;
        Codec<Entry<P>> entry = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
                kind.codec().fieldOf("contents").forGetter(Entry::contents)
        ).apply(instance, Entry::new));
        Codec<Saved<S, P>> codec = entry.listOf().xmap(entries -> new Saved<S, P>(entries), Saved::entries);
        Identifier id = kind.id();
        // Saved as data/<namespace>/<kind>_grids.dat. Vanilla needs some data fixer to read the file
        // back; the command storage one leaves data of the current version alone.
        this.savedType = new SavedDataType<>(Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_grids"),
                () -> new Saved<S, P>(List.of()), codec, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    /** Starts keeping grids of this kind. Call once per kind, during startup. */
    public static <S, P> Grids<S, P> register(GridKind<S, P> kind) {
        Grids<S, P> grids = new Grids<>(kind);
        ALL.add(grids);
        return grids;
    }

    public GridKind<S, P> kind() {
        return kind;
    }

    /**
     * The shared storage of the grid the block at {@code pos} belongs to, or null if no block of this
     * kind is there (or its chunk isn't loaded, or this isn't the server thread).
     */
    public S storageAt(ServerLevel level, BlockPos pos) {
        if (!level.getServer().isSameThread() || !level.isLoaded(pos)) {
            return null;
        }
        LevelGrids grids = levelGrids(level);
        Grid grid = grids.byPos.get(pos.asLong());
        if (grid == null) {
            grid = grids.build(pos);
        }
        return grid == null ? null : grid.storage;
    }

    /**
     * What every kind of grid holds at {@code pos}, one line each, for {@code /outworld grid}; empty if
     * no grid block is there.
     */
    public static String describeAt(ServerLevel level, BlockPos pos) {
        StringBuilder text = new StringBuilder();
        for (Grids<?, ?> grids : ALL) {
            String line = grids.describe(level, pos);
            if (line != null) {
                text.append(text.isEmpty() ? "" : "\n").append(line);
            }
        }
        return text.toString();
    }

    private String describe(ServerLevel level, BlockPos pos) {
        if (storageAt(level, pos) == null) {
            return null;
        }
        Grid grid = levels.get(level).byPos.get(pos.asLong());
        return kind.id() + " grid of " + grid.positions.length + " blocks, " + grid.neighbours.length + " neighbours: "
                + kind.describe(kind.contents(grid.storage), grid.capacity);
    }

    /** How many grids and blocks of this kind are known in a level, for debugging. */
    public int[] stats(ServerLevel level) {
        LevelGrids grids = levels.get(level);
        return grids == null ? new int[] {0, 0} : new int[] {grids.live.size(), grids.byPos.size()};
    }

    /**
     * A block changed at {@code pos}. If a grid block came or went, the grids it touches are dropped;
     * if a block entity came or went next to one, that grid is dropped so it finds its neighbours again.
     */
    public static void blockChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (oldState.getBlock() == newState.getBlock()) {
            return; // a block's own state changing (a pipe's connections) doesn't matter
        }
        for (Grids<?, ?> grids : ALL) {
            grids.changed(level, pos, oldState, newState);
        }
    }

    /** A chunk loaded or unloaded: grids in or next to it may have grown or shrunk. */
    public static void chunkChanged(ServerLevel level, int chunkX, int chunkZ) {
        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> chunkChanged(level, chunkX, chunkZ));
            return;
        }
        for (Grids<?, ?> grids : ALL) {
            var levelGrids = grids.levels.get(level);
            if (levelGrids != null) {
                levelGrids.chunkChanged(chunkKey(chunkX, chunkZ));
            }
        }
    }

    /**
     * Finds again the grids dropped this tick that held something, then offers what was put into grids
     * to their neighbours. Called at the end of every level tick.
     */
    public static void tick(ServerLevel level) {
        for (Grids<?, ?> grids : ALL) {
            var levelGrids = grids.levels.get(level);
            if (levelGrids != null && (!levelGrids.rebuild.isEmpty() || !levelGrids.pending.isEmpty())) {
                try (var _ = PERF.start()) {
                    levelGrids.rebuild();
                    levelGrids.distribute();
                }
            }
        }
    }

    /**
     * Forgets every level. Called when the server stops; levels saved after this still save what
     * their grids held.
     */
    public static void clear() {
        for (Grids<?, ?> grids : ALL) {
            grids.levels.clear();
        }
    }

    private void changed(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        boolean memberBefore = kind.capacity(oldState) > 0;
        boolean memberAfter = kind.capacity(newState) > 0;
        boolean blockEntityChanged = oldState.hasBlockEntity() || newState.hasBlockEntity();
        if (!memberBefore && !memberAfter && !blockEntityChanged) {
            return;
        }
        if (!level.getServer().isSameThread()) {
            BlockPos immutable = pos.immutable();
            level.getServer().execute(() -> changed(level, immutable, oldState, newState));
            return;
        }
        // A removed block's saved share must go even if nothing has asked for grids here yet.
        LevelGrids grids = memberBefore && !memberAfter ? levelGrids(level) : levels.get(level);
        if (grids == null) {
            return;
        }
        grids.drop(pos.asLong());
        for (Direction direction : Direction.values()) {
            grids.drop(BlockPos.offset(pos.asLong(), direction));
        }
        if (memberBefore && !memberAfter) {
            grids.leftover.remove(pos.asLong()); // the removed block's share goes with it
            grids.saved.setDirty();
        }
    }

    private LevelGrids levelGrids(ServerLevel level) {
        LevelGrids grids = levels.get(level);
        if (grids == null) {
            grids = new LevelGrids(level);
            levels.put(level, grids);
        }
        return grids;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private final class Grid {
        final long[] positions;
        final long[] chunks;
        /** Block entities next to the grid, and the face of each that touches it. */
        final long[] neighbours;
        final byte[] neighbourSides;
        final S storage;
        final long capacity;
        long lastAmount;
        int nextNeighbour;
        boolean live = true;

        Grid(long[] positions, long[] chunks, long[] neighbours, byte[] neighbourSides, S storage, long capacity) {
            this.capacity = capacity;
            this.positions = positions;
            this.chunks = chunks;
            this.neighbours = neighbours;
            this.neighbourSides = neighbourSides;
            this.storage = storage;
        }
    }

    /** One level's grids of this kind. */
    private final class LevelGrids {
        final ServerLevel level;
        final Long2ObjectOpenHashMap<Grid> byPos = new Long2ObjectOpenHashMap<>();
        /** Grids by every chunk they touch or border, for chunk loads and unloads. */
        final Long2ObjectOpenHashMap<List<Grid>> byChunk = new Long2ObjectOpenHashMap<>();
        final ReferenceOpenHashSet<Grid> live = new ReferenceOpenHashSet<>();
        /** Grids that were given something this tick. */
        final ReferenceLinkedOpenHashSet<Grid> pending = new ReferenceLinkedOpenHashSet<>();
        /**
         * Blocks of dropped grids that held something, to find their grids again at the end of the
         * tick, so a new neighbour of a full grid is offered its contents.
         */
        final LongLinkedOpenHashSet rebuild = new LongLinkedOpenHashSet();
        /** What dropped grids held, per block, waiting for that block's next grid. */
        final Long2ObjectOpenHashMap<P> leftover = new Long2ObjectOpenHashMap<>();
        final Saved<S, P> saved;

        LevelGrids(ServerLevel level) {
            this.level = level;
            this.saved = level.getDataStorage().computeIfAbsent(savedType);
            for (Entry<P> entry : saved.loaded) {
                leftover.put(entry.pos(), entry.contents());
            }
            saved.loaded = List.of();
            saved.owner = this;
        }

        Grid build(BlockPos start) {
            if (kind.capacity(level.getBlockState(start)) <= 0) {
                return null;
            }
            LongOpenHashSet visited = new LongOpenHashSet();
            LongOpenHashSet chunks = new LongOpenHashSet();
            LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
            LongArrayList found = new LongArrayList();
            LongArrayList neighbours = new LongArrayList();
            List<Byte> sides = new ArrayList<>();
            long capacity = 0;
            queue.enqueue(start.asLong());
            visited.add(start.asLong());
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
            while (!queue.isEmpty() && found.size() < MAX_GRID_SIZE) {
                long packed = queue.dequeueLong();
                cursor.set(packed);
                if (!level.isLoaded(cursor)) {
                    chunks.add(chunkKey(cursor.getX() >> 4, cursor.getZ() >> 4)); // the grid may grow when it loads
                    continue;
                }
                long blockCapacity = kind.capacity(level.getBlockState(cursor));
                if (blockCapacity <= 0 || byPos.containsKey(packed)) {
                    continue;
                }
                found.add(packed);
                capacity += blockCapacity;
                chunks.add(chunkKey(cursor.getX() >> 4, cursor.getZ() >> 4));
                for (Direction direction : Direction.values()) {
                    long nextPacked = BlockPos.offset(packed, direction);
                    next.set(nextPacked);
                    if (level.isLoaded(next)) {
                        BlockState state = level.getBlockState(next);
                        if (kind.capacity(state) <= 0 && state.hasBlockEntity()) {
                            neighbours.add(nextPacked);
                            sides.add((byte) direction.getOpposite().get3DDataValue());
                        }
                    }
                    if (visited.add(nextPacked)) {
                        queue.enqueue(nextPacked);
                    }
                }
            }

            long[] positions = found.toLongArray();
            P contents = kind.empty();
            for (long pos : positions) {
                P share = leftover.remove(pos);
                if (share != null) {
                    contents = combine(contents, share);
                }
            }
            if (kind.amount(contents) > capacity) {
                contents = kind.withAmount(contents, capacity);
            }
            byte[] neighbourSides = new byte[sides.size()];
            for (int i = 0; i < neighbourSides.length; i++) {
                neighbourSides[i] = sides.get(i);
            }
            S storage = kind.createStorage(capacity);
            kind.setContents(storage, contents);
            Grid grid = new Grid(positions, chunks.toLongArray(), neighbours.toLongArray(), neighbourSides, storage, capacity);
            grid.lastAmount = kind.amount(contents);
            kind.listen(storage, () -> contentsChanged(grid));
            if (grid.lastAmount > 0 && grid.neighbours.length > 0) {
                pending.add(grid); // offer it to its neighbours, which may be new
            }

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
            live.add(grid);
            if (positions.length >= MAX_GRID_SIZE) {
                OutworldCore.LOG.warn("A {} grid at {} has over {} blocks; it is split into several", kind.id(), start, MAX_GRID_SIZE);
            }
            return grid;
        }

        /** Adds what two blocks held; if they held things that can't mix, the bigger one stays. */
        P combine(P first, P second) {
            if (kind.amount(first) <= 0) {
                return second;
            }
            if (kind.amount(second) <= 0) {
                return first;
            }
            if (kind.canMix(first, second)) {
                return kind.withAmount(first, kind.amount(first) + kind.amount(second));
            }
            return kind.amount(second) > kind.amount(first) ? second : first;
        }

        void contentsChanged(Grid grid) {
            if (!grid.live) {
                return;
            }
            long amount = kind.amount(kind.contents(grid.storage));
            if (amount > grid.lastAmount && grid.neighbours.length > 0) {
                pending.add(grid);
            }
            grid.lastAmount = amount;
            saved.setDirty();
        }

        void rebuild() {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            while (!rebuild.isEmpty()) {
                long pos = rebuild.removeFirstLong();
                cursor.set(pos);
                if (!byPos.containsKey(pos) && level.isLoaded(cursor) && kind.capacity(level.getBlockState(cursor)) > 0) {
                    build(cursor.immutable());
                }
            }
        }

        void distribute() {
            List<Grid> due = new ArrayList<>(pending);
            pending.clear();
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (Grid grid : due) {
                int count = grid.neighbours.length;
                int start = grid.nextNeighbour;
                grid.nextNeighbour = (start + 1) % count;
                for (int i = 0; i < count && grid.live; i++) {
                    if (kind.amount(kind.contents(grid.storage)) <= 0) {
                        break;
                    }
                    int index = (start + i) % count;
                    cursor.set(grid.neighbours[index]);
                    if (level.isLoaded(cursor)) {
                        kind.offer(level, grid.storage, cursor, Direction.from3DDataValue(grid.neighbourSides[index]));
                    }
                }
            }
        }

        void chunkChanged(long chunk) {
            List<Grid> touching = byChunk.get(chunk);
            if (touching != null) {
                for (Grid grid : List.copyOf(touching)) {
                    drop(grid);
                }
            }
        }

        void drop(long pos) {
            Grid grid = byPos.get(pos);
            if (grid != null) {
                drop(grid);
            }
        }

        void drop(Grid grid) {
            grid.live = false;
            live.remove(grid);
            P contents = kind.contents(grid.storage);
            long amount = kind.amount(contents);
            int size = grid.positions.length;
            for (int i = 0; i < size; i++) {
                long pos = grid.positions[i];
                byPos.remove(pos);
                // Share the contents out over the blocks, the remainder to the first.
                long share = amount / size + (i == 0 ? amount % size : 0);
                if (share > 0) {
                    leftover.put(pos, kind.withAmount(contents, share));
                    rebuild.add(pos);
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
            kind.setContents(grid.storage, kind.empty()); // anyone still holding the old storage finds it empty
            saved.setDirty();
        }

        /** What every block holds right now: leftovers, and each live grid shared out over its blocks. */
        List<Entry<P>> snapshot() {
            List<Entry<P>> entries = new ArrayList<>(leftover.size());
            for (Long2ObjectMap.Entry<P> entry : leftover.long2ObjectEntrySet()) {
                if (kind.amount(entry.getValue()) > 0) {
                    entries.add(new Entry<>(entry.getLongKey(), entry.getValue()));
                }
            }
            for (Grid grid : live) {
                P contents = kind.contents(grid.storage);
                long amount = kind.amount(contents);
                int size = grid.positions.length;
                for (int i = 0; i < size && amount > 0; i++) {
                    long share = amount / size + (i == 0 ? amount % size : 0);
                    if (share > 0) {
                        entries.add(new Entry<>(grid.positions[i], kind.withAmount(contents, share)));
                    }
                }
            }
            return entries;
        }
    }

    private record Entry<P>(long pos, P contents) {
    }

    /** A level's grid contents of one kind, as saved with the level. */
    private static final class Saved<S, P> extends SavedData {
        /** What was read from disk, until the level's grids take it. */
        List<Entry<P>> loaded;
        Grids<S, P>.LevelGrids owner;

        Saved(List<Entry<P>> loaded) {
            this.loaded = loaded;
        }

        List<Entry<P>> entries() {
            return owner != null ? owner.snapshot() : loaded;
        }
    }
}
