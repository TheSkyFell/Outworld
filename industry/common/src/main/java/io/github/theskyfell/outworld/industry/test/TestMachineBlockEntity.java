package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.energy.Energy;
import io.github.theskyfell.outworld.core.energy.EnergyAccess;
import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Temporary test machine. A neighbour change starts a job of {@link #JOB_TICKS} ticks of work. Each
 * tick of work costs {@link #ENERGY_PER_TICK} FE, which it pulls from neighbouring energy storages
 * (from any mod) through the loader's energy API. Without energy it sleeps until a neighbour changes
 * or energy is pushed into it. The totals are saved, so tests can read them with {@code /data get block}.
 */
public final class TestMachineBlockEntity extends MachineBlockEntity {
    public static final int JOB_TICKS = 100;
    public static final long ENERGY_PER_TICK = 10;

    private final EnergyBuffer energy = new EnergyBuffer(1_000, 1_000, 0);
    private int workRemaining;
    private int workDone;

    public TestMachineBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.TEST_MACHINE.get(), pos, state);
        energy.onChange(() -> {
            setChanged();
            wake();
        });
    }

    /** Queues a new job and wakes the machine. */
    public void startJob() {
        workRemaining = JOB_TICKS;
        setChanged();
        wake();
    }

    public int workDone() {
        return workDone;
    }

    public EnergyBuffer energy() {
        return energy;
    }

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        if (workRemaining <= 0) {
            return false;
        }
        int wanted = Math.min(elapsedTicks, workRemaining);
        long missing = wanted * ENERGY_PER_TICK - energy.getAmount();
        if (missing > 0) {
            pullEnergy(level, missing);
        }
        int affordable = (int) Math.min(wanted, energy.getAmount() / ENERGY_PER_TICK);
        if (affordable <= 0) {
            return false; // starved: sleep until a neighbour changes or energy arrives
        }
        energy.setAmount(energy.getAmount() - affordable * ENERGY_PER_TICK);
        workRemaining -= affordable;
        workDone += affordable;
        setChanged();
        return workRemaining > 0;
    }

    private void pullEnergy(ServerLevel level, long amount) {
        for (Direction direction : Direction.values()) {
            long room = energy.insertable(amount);
            if (room <= 0) {
                return;
            }
            EnergyAccess source = Energy.find(level, worldPosition.relative(direction), direction.getOpposite());
            if (source != null) {
                energy.insert(source.extract(room, false), false);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("work_remaining", workRemaining);
        output.putInt("work_done", workDone);
        energy.save(output, "energy");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        workRemaining = input.getIntOr("work_remaining", 0);
        workDone = input.getIntOr("work_done", 0);
        energy.load(input, "energy");
    }
}
