package io.github.theskyfell.outworld.core.grid;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One kind of block network, such as cables or fluid pipes: blocks of the kind that touch form a grid
 * that shares one storage. Each kind is given to {@link Grids#register} once, which does the rest.
 *
 * @param <S> the shared storage, such as an energy buffer or a fluid tank
 * @param <P> what a storage holds, such as an amount of energy or of one fluid; it is shared out over
 *     a grid's blocks when the grid changes, and saved with the world
 */
public abstract class GridKind<S, P> {
    private final Identifier id;
    private final Codec<P> codec;

    /**
     * @param id names the kind in logs and in the world's saved data, e.g. {@code outworld:energy}
     * @param codec saves what a grid holds; never asked to save an empty portion
     */
    protected GridKind(Identifier id, Codec<P> codec) {
        this.id = id;
        this.codec = codec;
    }

    public final Identifier id() {
        return id;
    }

    final Codec<P> codec() {
        return codec;
    }

    /** What this block adds to its grid's capacity, or 0 if it doesn't join grids of this kind. */
    public abstract long capacity(BlockState state);

    /** A new, empty storage for a whole grid. */
    protected abstract S createStorage(long capacity);

    protected abstract P contents(S storage);

    /** Fills a new storage, or empties one when its grid is dropped. Never more than its capacity. */
    protected abstract void setContents(S storage, P contents);

    /** Calls {@code listener} after every change to the storage's contents. */
    protected abstract void listen(S storage, Runnable listener);

    protected abstract P empty();

    protected abstract long amount(P portion);

    protected abstract P withAmount(P portion, long amount);

    /** True if the two (neither empty) can be stored together, such as the same fluid. */
    protected abstract boolean canMix(P first, P second);

    /** What a grid holds and could hold, in words, for commands: e.g. {@code 1,200 / 3,000 FE}. */
    protected abstract String describe(P contents, long capacity);

    /**
     * Offers the storage's contents to the storage at {@code pos}, from any mod, as seen from
     * {@code side}.
     *
     * @return how much moved
     */
    protected abstract long offer(ServerLevel level, S storage, BlockPos pos, Direction side);
}
