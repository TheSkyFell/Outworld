package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.fluid.FluidAccess;
import io.github.theskyfell.outworld.core.fluid.FluidTank;
import io.github.theskyfell.outworld.core.fluid.FluidTransfer;
import io.github.theskyfell.outworld.core.fluid.FluidUnits;
import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Temporary test tank: holds {@link #CAPACITY} of any one fluid, open to pipes and buckets from any
 * mod through the loaders' fluid APIs. Fluid rises through it: it pulls from the block below and
 * pushes into the block above, up to {@link #AMOUNT_PER_BATCH} each way per batch, which tests finding
 * other storages (vanilla cauldrons included) from common code. It sleeps when nothing moves and wakes
 * when its contents or a neighbour change.
 */
public final class TestTankBlockEntity extends MachineBlockEntity {
    public static final long CAPACITY = 4 * FluidUnits.BUCKET;
    public static final long AMOUNT_PER_BATCH = FluidUnits.BUCKET;

    private final FluidTank tank = new FluidTank(CAPACITY);

    public TestTankBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.TEST_TANK.get(), pos, state);
        tank.onChange(() -> {
            setChanged();
            wake();
        });
    }

    public FluidTank tank() {
        return tank;
    }

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        long moved = 0;
        FluidAccess below = FluidTransfer.find(level, worldPosition.below(), Direction.UP);
        if (below != null) {
            moved += FluidTransfer.move(below, tank, fluid -> true, AMOUNT_PER_BATCH);
        }
        FluidAccess above = FluidTransfer.find(level, worldPosition.above(), Direction.DOWN);
        if (above != null) {
            moved += FluidTransfer.move(tank, above, fluid -> true, AMOUNT_PER_BATCH);
        }
        return moved > 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output, "tank");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input, "tank");
    }
}
