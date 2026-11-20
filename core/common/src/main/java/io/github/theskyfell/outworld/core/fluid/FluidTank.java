package io.github.theskyfell.outworld.core.fluid;

import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One fluid tank in an Outworld block, holding a single fluid, in droplets (see {@link FluidUnits}).
 * Each loader exposes it to pipes and other mods through its own fluid API, with full transaction
 * support. Register it in {@link io.github.theskyfell.outworld.core.module.OutworldModule#registerTransfers}.
 *
 * <p>The insert and extract flags and the filter apply to the outside world and to {@link #insert} and
 * {@link #extract}. The block's own code can change the contents directly with {@link #setContents}.
 */
public class FluidTank implements FluidAccess {
    private final long capacity;
    private final boolean canInsert;
    private final boolean canExtract;
    private final Predicate<Fluid> filter;
    private FluidAmount contents = FluidAmount.EMPTY;
    private Runnable onChange = () -> { };
    private Object platformView;

    /** A tank that accepts any fluid and lets it out again. */
    public FluidTank(long capacity) {
        this(capacity, true, true, fluid -> true);
    }

    public FluidTank(long capacity, boolean canInsert, boolean canExtract, Predicate<Fluid> filter) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Tank capacity can't be negative");
        }
        this.capacity = capacity;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
        this.filter = filter;
    }

    /** Runs after every change that sticks (not after simulations or rolled-back transactions). */
    public void onChange(Runnable listener) {
        this.onChange = listener;
    }

    public FluidAmount getContents() {
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

    public boolean canInsert() {
        return canInsert;
    }

    public boolean canExtract() {
        return canExtract;
    }

    /** True if the outside world may put this fluid in (ignoring how full the tank is). */
    public boolean accepts(Fluid fluid) {
        return canInsert && fluid != Fluids.EMPTY && filter.test(fluid);
    }

    @Override
    public long insert(Fluid fluid, DataComponentPatch components, long maxAmount, boolean simulate) {
        long accepted = insertable(fluid, components, maxAmount);
        if (accepted > 0 && !simulate) {
            contents = new FluidAmount(fluid, components, contents.amount() + accepted);
            onChange.run();
        }
        return accepted;
    }

    @Override
    public FluidAmount extract(Predicate<Fluid> fluidFilter, long maxAmount, boolean simulate) {
        if (contents.isEmpty() || !fluidFilter.test(contents.fluid())) {
            return FluidAmount.EMPTY;
        }
        long removed = extractable(contents.fluid(), contents.components(), maxAmount);
        if (removed <= 0) {
            return FluidAmount.EMPTY;
        }
        FluidAmount result = new FluidAmount(contents.fluid(), contents.components(), removed);
        if (!simulate) {
            setContentsSilently(new FluidAmount(contents.fluid(), contents.components(), contents.amount() - removed));
            onChange.run();
        }
        return result;
    }

    /** Sets the contents directly, ignoring the flags and filter (amount clamped to the capacity). */
    public void setContents(FluidAmount newContents) {
        setContentsSilently(newContents);
        onChange.run();
    }

    public void save(ValueOutput output, String key) {
        output.store(key, FluidAmount.CODEC, contents);
    }

    public void load(ValueInput input, String key) {
        setContentsSilently(input.read(key, FluidAmount.CODEC).orElse(FluidAmount.EMPTY));
    }

    // The methods below are for Core's loader layer, which wraps the tank in each loader's
    // transactional fluid API.

    /** How much {@link #insert} would accept, without changing anything. */
    public long insertable(Fluid fluid, DataComponentPatch components, long maxAmount) {
        if (maxAmount <= 0 || !accepts(fluid)) {
            return 0;
        }
        if (!contents.isEmpty() && !contents.isSameFluid(fluid, components)) {
            return 0;
        }
        return Math.max(0, Math.min(maxAmount, capacity - contents.amount()));
    }

    /** How much of this exact fluid {@link #extract} would remove, without changing anything. */
    public long extractable(Fluid fluid, DataComponentPatch components, long maxAmount) {
        if (maxAmount <= 0 || !canExtract || contents.isEmpty() || !contents.isSameFluid(fluid, components)) {
            return 0;
        }
        return Math.min(maxAmount, contents.amount());
    }

    /** Changes the contents without notifying, inside a transaction that may still be rolled back. */
    public void setContentsInTransaction(FluidAmount newContents) {
        setContentsSilently(newContents);
    }

    /** Notifies the listener once a transaction that changed the contents has committed. */
    public void notifyCommitted() {
        onChange.run();
    }

    /**
     * The loader's wrapper for this tank, created on first use. There must be exactly one per tank, so
     * that transactions snapshot and roll it back exactly once.
     */
    public <V> V platformView(Class<V> type, Function<FluidTank, V> create) {
        if (platformView == null) {
            platformView = create.apply(this);
        }
        return type.cast(platformView);
    }

    private void setContentsSilently(FluidAmount newContents) {
        long amount = Math.clamp(newContents.amount(), 0, capacity);
        contents = amount == 0 || newContents.fluid() == Fluids.EMPTY
                ? FluidAmount.EMPTY
                : new FluidAmount(newContents.fluid(), newContents.components(), amount);
    }
}
