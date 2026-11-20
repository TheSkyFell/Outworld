package io.github.theskyfell.outworld.core.energy;

import java.util.function.Function;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Energy stored in an Outworld block, in FE. Each loader exposes it to other mods through its own
 * energy API (NeoForge's {@code EnergyHandler}, Team Reborn Energy on Fabric), with full transaction
 * support. Register it in {@link io.github.theskyfell.outworld.core.module.OutworldModule#registerTransfers}.
 *
 * <p>The rate limits apply to each single insert or extract call. A generator sets {@code maxInsert}
 * to 0, a consumer {@code maxExtract} to 0.
 *
 * <p>Typical use in a machine:
 * <pre>{@code
 * private final EnergyBuffer energy = new EnergyBuffer(10_000, 200, 0);
 * // in the constructor:
 * energy.onChange(() -> { setChanged(); wake(); });
 * }</pre>
 */
public class EnergyBuffer implements EnergyAccess {
    private final long capacity;
    private final long maxInsert;
    private final long maxExtract;
    private long amount;
    private Runnable onChange = () -> { };
    private Object platformView;

    public EnergyBuffer(long capacity, long maxInsert, long maxExtract) {
        if (capacity < 0 || maxInsert < 0 || maxExtract < 0) {
            throw new IllegalArgumentException("Energy capacity and rates can't be negative");
        }
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
    }

    /** Runs after every change that sticks (not after simulations or rolled-back transactions). */
    public void onChange(Runnable listener) {
        this.onChange = listener;
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        long accepted = insertable(maxAmount);
        if (accepted > 0 && !simulate) {
            amount += accepted;
            onChange.run();
        }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        long removed = extractable(maxAmount);
        if (removed > 0 && !simulate) {
            amount -= removed;
            onChange.run();
        }
        return removed;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    public boolean canInsert() {
        return maxInsert > 0;
    }

    public boolean canExtract() {
        return maxExtract > 0;
    }

    /** Sets the stored amount directly, ignoring rates (clamped to 0..capacity). */
    public void setAmount(long newAmount) {
        long clamped = Math.clamp(newAmount, 0, capacity);
        if (clamped != amount) {
            amount = clamped;
            onChange.run();
        }
    }

    public void save(ValueOutput output, String key) {
        output.putLong(key, amount);
    }

    public void load(ValueInput input, String key) {
        amount = Math.clamp(input.getLongOr(key, 0L), 0, capacity);
    }

    // The methods below are for Core's loader layer, which wraps the buffer in each loader's
    // transactional energy API.

    /** How much {@link #insert} would accept, without changing anything. */
    public long insertable(long maxAmount) {
        if (maxAmount <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(Math.min(maxAmount, maxInsert), capacity - amount));
    }

    /** How much {@link #extract} would remove, without changing anything. */
    public long extractable(long maxAmount) {
        if (maxAmount <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(Math.min(maxAmount, maxExtract), amount));
    }

    /** Changes the amount without notifying, inside a transaction that may still be rolled back. */
    public void setAmountInTransaction(long newAmount) {
        amount = newAmount;
    }

    /** Notifies the listener once a transaction that changed the amount has committed. */
    public void notifyCommitted() {
        onChange.run();
    }

    /**
     * The loader's wrapper for this buffer, created on first use. There must be exactly one per
     * buffer, so that transactions snapshot and roll it back exactly once.
     */
    public <V> V platformView(Class<V> type, Function<EnergyBuffer, V> create) {
        if (platformView == null) {
            platformView = create.apply(this);
        }
        return type.cast(platformView);
    }
}
