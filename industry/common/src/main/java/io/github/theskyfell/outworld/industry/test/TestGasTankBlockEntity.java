package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.gas.GasAccess;
import io.github.theskyfell.outworld.core.gas.GasHolder;
import io.github.theskyfell.outworld.core.gas.GasTank;
import io.github.theskyfell.outworld.core.gas.GasTransfer;
import io.github.theskyfell.outworld.core.gas.Gases;
import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Temporary test gas tank: holds {@link #CAPACITY} of any one gas, open on every side through
 * {@link GasHolder}. Gas rises through it: it pulls from the block below and pushes into the block
 * above, up to a bucket each way per batch, which tests finding gas storage from common code. It
 * sleeps when nothing moves and wakes when its contents or a neighbour change.
 */
public final class TestGasTankBlockEntity extends MachineBlockEntity implements GasHolder {
    public static final long CAPACITY = 8 * Gases.BUCKET;
    public static final long AMOUNT_PER_BATCH = Gases.BUCKET;

    private final GasTank tank = new GasTank(CAPACITY);

    public TestGasTankBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.TEST_GAS_TANK.get(), pos, state);
        tank.onChange(() -> {
            setChanged();
            wake();
        });
    }

    @Override
    public GasAccess gasAccess(Direction side) {
        return tank;
    }

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        long moved = 0;
        GasAccess below = GasTransfer.find(level, worldPosition.below(), Direction.UP);
        if (below != null) {
            moved += GasTransfer.move(below, tank, gas -> true, AMOUNT_PER_BATCH);
        }
        GasAccess above = GasTransfer.find(level, worldPosition.above(), Direction.DOWN);
        if (above != null) {
            moved += GasTransfer.move(tank, above, gas -> true, AMOUNT_PER_BATCH);
        }
        return moved > 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output, "gas");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input, "gas");
    }
}
