package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.industry.IndustryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** The test machine's menu. It has no slots; it only knows which block it belongs to. */
public final class TestMachineMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ContainerLevelAccess access;

    /** Used on both sides: the server creates it directly, the client from the position the server sent. */
    public TestMachineMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(IndustryContent.TEST_MACHINE_MENU.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, IndustryContent.TEST_BLOCK.get());
    }
}
