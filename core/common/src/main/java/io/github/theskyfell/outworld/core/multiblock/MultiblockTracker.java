package io.github.theskyfell.outworld.core.multiblock;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Tells multiblock controllers when a block in their structure changes, so they re-check only then and
 * never scan the world on a timer.
 *
 * <p>Each controller claims the positions its structure covers. Core hooks every block change on the
 * server (see {@code LevelChunkMixin}); a change at a claimed position marks the controller for a
 * re-check and wakes it. A change anywhere else costs one hash lookup. Server thread only.
 */
public final class MultiblockTracker {
    private static final Map<ServerLevel, MultiblockTracker> TRACKERS = new IdentityHashMap<>();

    private final Long2ObjectOpenHashMap<List<MultiblockControllerBlockEntity>> watchers = new Long2ObjectOpenHashMap<>();
    private final Map<MultiblockControllerBlockEntity, long[]> claims = new IdentityHashMap<>();

    private MultiblockTracker() {
    }

    /** Called by Core for every block change in a loaded chunk on the server. */
    public static void blockChanged(ServerLevel level, BlockPos pos) {
        if (TRACKERS.isEmpty()) {
            return;
        }
        if (!level.getServer().isSameThread()) {
            BlockPos immutable = pos.immutable();
            level.getServer().execute(() -> blockChanged(level, immutable));
            return;
        }
        MultiblockTracker tracker = TRACKERS.get(level);
        if (tracker == null) {
            return;
        }
        List<MultiblockControllerBlockEntity> controllers = tracker.watchers.get(pos.asLong());
        if (controllers != null) {
            // Copy: a controller's reaction may change the claims.
            for (MultiblockControllerBlockEntity controller : List.copyOf(controllers)) {
                controller.structureChanged();
            }
        }
    }

    /** Replaces a controller's claimed positions. */
    static void claim(ServerLevel level, MultiblockControllerBlockEntity controller, List<BlockPos> positions) {
        MultiblockTracker tracker = TRACKERS.computeIfAbsent(level, l -> new MultiblockTracker());
        tracker.unclaim(controller);
        long[] packed = new long[positions.size()];
        for (int i = 0; i < packed.length; i++) {
            packed[i] = positions.get(i).asLong();
            List<MultiblockControllerBlockEntity> controllers = tracker.watchers.get(packed[i]);
            if (controllers == null) {
                controllers = new ArrayList<>(1);
                tracker.watchers.put(packed[i], controllers);
            }
            controllers.add(controller);
        }
        tracker.claims.put(controller, packed);
    }

    /** Forgets a controller's claims. Never creates a tracker. */
    static void release(ServerLevel level, MultiblockControllerBlockEntity controller) {
        MultiblockTracker tracker = TRACKERS.get(level);
        if (tracker != null) {
            tracker.unclaim(controller);
            if (tracker.claims.isEmpty()) {
                TRACKERS.remove(level);
            }
        }
    }

    /** Forgets every level. Called when the server stops. */
    public static void clear() {
        TRACKERS.clear();
    }

    /** How many positions are watched in a level, for debugging. */
    public static int watchedPositions(ServerLevel level) {
        MultiblockTracker tracker = TRACKERS.get(level);
        return tracker == null ? 0 : tracker.watchers.size();
    }

    private void unclaim(MultiblockControllerBlockEntity controller) {
        long[] packed = claims.remove(controller);
        if (packed == null) {
            return;
        }
        for (long pos : packed) {
            List<MultiblockControllerBlockEntity> controllers = watchers.get(pos);
            if (controllers != null) {
                controllers.remove(controller);
                if (controllers.isEmpty()) {
                    watchers.remove(pos);
                }
            }
        }
    }
}
