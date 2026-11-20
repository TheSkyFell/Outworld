package io.github.theskyfell.outworld.core.gas;

import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One gas tank in an Outworld block, holding a single gas, in millibuckets. Blocks expose their tanks
 * by implementing {@link GasHolder}.
 *
 * <p>The insert and extract flags and the filter apply to the outside world and to {@link #insert} and
 * {@link #extract}. The block's own code can change the contents directly with {@link #setContents}.
 */
public class GasTank implements GasAccess {
    private final long capacity;
    private final boolean canInsert;
    private final boolean canExtract;
    private final Predicate<ResourceKey<Gas>> filter;
    private GasStack contents = GasStack.EMPTY;
    private Runnable onChange = () -> { };

    /** A tank that accepts any gas and lets it out again. */
    public GasTank(long capacity) {
        this(capacity, true, true, gas -> true);
    }

    public GasTank(long capacity, boolean canInsert, boolean canExtract, Predicate<ResourceKey<Gas>> filter) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Tank capacity can't be negative");
        }
        this.capacity = capacity;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
        this.filter = filter;
    }

    /** Runs after every change. */
    public void onChange(Runnable listener) {
        this.onChange = listener;
    }

    public GasStack getContents() {
        return contents;
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public long getAmount() {
        return contents.amount();
    }

    public long getCapacity() {
        return capacity;
    }

    /** True if the outside world may put this gas in (ignoring how full the tank is). */
    public boolean accepts(ResourceKey<Gas> gas) {
        return canInsert && gas != null && filter.test(gas);
    }

    @Override
    public long insert(ResourceKey<Gas> gas, long maxAmount, boolean simulate) {
        if (maxAmount <= 0 || !accepts(gas) || (!contents.isEmpty() && !contents.is(gas))) {
            return 0;
        }
        long accepted = Math.min(maxAmount, capacity - contents.amount());
        if (accepted > 0 && !simulate) {
            setContents(GasStack.of(gas, contents.amount() + accepted));
        }
        return Math.max(0, accepted);
    }

    @Override
    public GasStack extract(Predicate<ResourceKey<Gas>> gasFilter, long maxAmount, boolean simulate) {
        if (maxAmount <= 0 || !canExtract || contents.isEmpty() || !gasFilter.test(contents.gas())) {
            return GasStack.EMPTY;
        }
        long removed = Math.min(maxAmount, contents.amount());
        GasStack result = GasStack.of(contents.gas(), removed);
        if (!simulate) {
            setContents(contents.withAmount(contents.amount() - removed));
        }
        return result;
    }

    /** Sets the contents directly, ignoring the flags and filter (amount clamped to the capacity). */
    public void setContents(GasStack newContents) {
        contents = newContents.isEmpty() ? GasStack.EMPTY : newContents.withAmount(Math.min(newContents.amount(), capacity));
        onChange.run();
    }

    public void save(ValueOutput output, String key) {
        output.store(key, GasStack.CODEC, contents);
    }

    public void load(ValueInput input, String key) {
        GasStack loaded = input.read(key, GasStack.CODEC).orElse(GasStack.EMPTY);
        contents = loaded.isEmpty() ? GasStack.EMPTY : loaded.withAmount(Math.min(loaded.amount(), capacity));
    }
}
