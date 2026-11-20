package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.stress.StressRun;
import io.github.theskyfell.outworld.core.stress.StressScenario;
import io.github.theskyfell.outworld.industry.IndustryContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The {@code machines} stress scenario: test machines that work nonstop, each sitting on a creative
 * test battery that it draws 10 FE per tick from through the loader's energy API. It loads the
 * machine scheduler and energy transfer; quieting it lets every machine finish its job and sleep.
 *
 * <p>The pairs stand in a square grid, two blocks apart so no machine touches another pair.
 */
public final class MachinesStressScenario implements StressScenario {
    private static final int SPACING = 2;

    @Override
    public String description() {
        return "test machines working nonstop, each drawing energy from its own creative battery";
    }

    @Override
    public int defaultCount() {
        return 500;
    }

    @Override
    public BoundingBox area(BlockPos origin, int count) {
        int span = (side(count) - 1) * SPACING;
        return new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + span, origin.getY() + 1, origin.getZ() + span);
    }

    @Override
    public StressRun build(ServerLevel level, BlockPos origin, int count) {
        int side = side(count);
        List<BlockPos> machines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockPos batteryPos = origin.offset((i % side) * SPACING, 0, (i / side) * SPACING);
            BlockPos machinePos = batteryPos.above();
            level.setBlock(batteryPos, IndustryContent.TEST_BATTERY_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(batteryPos) instanceof TestBatteryBlockEntity battery) {
                battery.setCreative(true);
            }
            level.setBlock(machinePos, IndustryContent.TEST_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(machinePos) instanceof TestMachineBlockEntity machine) {
                machine.setRepeat(true);
                machines.add(machinePos);
            }
        }
        return quietLevel -> {
            for (BlockPos pos : machines) {
                if (quietLevel.getBlockEntity(pos) instanceof TestMachineBlockEntity machine) {
                    machine.setRepeat(false);
                }
            }
        };
    }

    private static int side(int count) {
        return (int) Math.ceil(Math.sqrt(count));
    }
}
