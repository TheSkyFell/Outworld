package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.stress.StressRun;
import io.github.theskyfell.outworld.core.stress.StressScenario;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.logistics.BeltBlock;
import io.github.theskyfell.outworld.industry.logistics.BeltBlockEntity;
import io.github.theskyfell.outworld.industry.logistics.BeltLane;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The {@code belts} stress scenario: square loops of belt, {@value #RING} blocks a side, each belt
 * carrying four items (two per lane, half the lane's room), so every belt keeps moving and handing
 * items on, round the corners too. {@code count} is the number of belts, rounded up to whole loops.
 * Quieting it takes the items off.
 */
public final class BeltsStressScenario implements StressScenario {
    private static final int RING = 10;
    private static final int PER_RING = 4 * (RING - 1);
    private static final int SPACING = RING + 1;

    @Override
    public String description() {
        return "loops of conveyor belt full of moving items, " + PER_RING + " belts a loop";
    }

    @Override
    public int defaultCount() {
        return 5000;
    }

    @Override
    public BoundingBox area(BlockPos origin, int count) {
        int span = side(count) * SPACING;
        return new BoundingBox(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + span, origin.getY(), origin.getZ() + span);
    }

    @Override
    public StressRun build(ServerLevel level, BlockPos origin, int count) {
        int rings = rings(count);
        int side = side(count);
        List<BlockPos> belts = new ArrayList<>(rings * PER_RING);
        ItemStack item = new ItemStack(Items.IRON_INGOT);
        for (int ring = 0; ring < rings; ring++) {
            BlockPos corner = origin.offset((ring % side) * SPACING, 0, (ring / side) * SPACING);
            // Clockwise seen from above: east along the north edge, south down the east edge, and so on.
            for (int step = 0; step < PER_RING; step++) {
                int edge = step / (RING - 1);
                int along = step % (RING - 1);
                BlockPos pos;
                Direction facing;
                switch (edge) {
                    case 0 -> { pos = corner.offset(along, 0, 0); facing = Direction.EAST; }
                    case 1 -> { pos = corner.offset(RING - 1, 0, along); facing = Direction.SOUTH; }
                    case 2 -> { pos = corner.offset(RING - 1 - along, 0, RING - 1); facing = Direction.WEST; }
                    default -> { pos = corner.offset(0, 0, RING - 1 - along); facing = Direction.NORTH; }
                }
                level.setBlock(pos, IndustryContent.BELT_BLOCK.get().defaultBlockState().setValue(BeltBlock.FACING, facing), Block.UPDATE_ALL);
                belts.add(pos);
            }
        }
        for (BlockPos pos : belts) {
            if (level.getBlockEntity(pos) instanceof BeltBlockEntity belt) {
                for (int lane = 0; lane < 2; lane++) {
                    belt.receive(lane, item, 0);
                    belt.receive(lane, item, BeltLane.LENGTH / 2);
                }
            }
        }
        return quietLevel -> {
            for (BlockPos pos : belts) {
                if (quietLevel.getBlockEntity(pos) instanceof BeltBlockEntity belt) {
                    belt.clearItems();
                }
            }
        };
    }

    private static int rings(int count) {
        return Math.max(1, (count + PER_RING - 1) / PER_RING);
    }

    private static int side(int count) {
        return (int) Math.ceil(Math.sqrt(rings(count)));
    }
}
