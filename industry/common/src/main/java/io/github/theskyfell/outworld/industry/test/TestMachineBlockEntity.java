package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Temporary test machine. A neighbour change starts a job of {@link #JOB_TICKS} ticks of work; the
 * machine works through it in batches, then sleeps. The totals are saved, so tests can read them with
 * {@code /data get block}.
 */
public final class TestMachineBlockEntity extends MachineBlockEntity {
    public static final int JOB_TICKS = 100;

    private int workRemaining;
    private int workDone;

    public TestMachineBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.TEST_MACHINE.get(), pos, state);
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

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        if (workRemaining <= 0) {
            return false;
        }
        int done = Math.min(elapsedTicks, workRemaining);
        workRemaining -= done;
        workDone += done;
        setChanged();
        return workRemaining > 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("work_remaining", workRemaining);
        output.putInt("work_done", workDone);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        workRemaining = input.getIntOr("work_remaining", 0);
        workDone = input.getIntOr("work_done", 0);
    }
}
