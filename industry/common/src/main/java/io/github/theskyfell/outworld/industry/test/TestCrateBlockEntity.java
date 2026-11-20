package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.item.ItemAccess;
import io.github.theskyfell.outworld.core.item.ItemBuffer;
import io.github.theskyfell.outworld.core.item.ItemTransfer;
import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Temporary test crate: three slots that hoppers and pipes from any mod can fill and empty through the
 * loaders' item APIs. It also pulls items from the block above and pushes them into the block below,
 * up to {@link #ITEMS_PER_BATCH} each way per batch, which tests finding other storages from common
 * code. It sleeps when nothing moves and wakes when its contents or a neighbour change. Breaking it
 * destroys what's inside.
 */
public final class TestCrateBlockEntity extends MachineBlockEntity {
    public static final int ITEMS_PER_BATCH = 4;

    private final ItemBuffer items = new ItemBuffer(ItemBuffer.Role.STORAGE, ItemBuffer.Role.STORAGE, ItemBuffer.Role.STORAGE);

    public TestCrateBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.TEST_CRATE.get(), pos, state);
        items.onChange(() -> {
            setChanged();
            wake();
        });
    }

    public ItemBuffer items() {
        return items;
    }

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        int moved = 0;
        ItemAccess above = ItemTransfer.find(level, worldPosition.above(), Direction.DOWN);
        if (above != null) {
            moved += ItemTransfer.move(above, items, stack -> true, ITEMS_PER_BATCH);
        }
        ItemAccess below = ItemTransfer.find(level, worldPosition.below(), Direction.UP);
        if (below != null) {
            moved += ItemTransfer.move(items, below, stack -> true, ITEMS_PER_BATCH);
        }
        return moved > 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        items.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.load(input);
    }
}
