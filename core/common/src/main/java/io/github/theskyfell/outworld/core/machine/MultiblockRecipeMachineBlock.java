package io.github.theskyfell.outworld.core.machine;

import io.github.theskyfell.outworld.core.multiblock.MultiblockControllerBlock;
import io.github.theskyfell.outworld.core.multiblock.MultiblockController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The block of a recipe machine that is also a multiblock's controller. Its block entity must be a
 * {@link MultiblockRecipeMachineBlockEntity}.
 *
 * <p>Like any controller: right-clicking it while holding a block the structure needs places that
 * block in the nearest empty spot, and right-clicking while sneaking shows or hides the build preview
 * and says what is missing. A plain right-click opens the machine screen once the structure is
 * complete, and shows the structure until then.
 */
public abstract class MultiblockRecipeMachineBlock extends RecipeMachineBlock {
    protected MultiblockRecipeMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        InteractionResult placed = MultiblockControllerBlock.placeNeededBlock(stack, level, pos, player, hand);
        return placed != null ? placed : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        boolean formed = level.getBlockEntity(pos) instanceof MultiblockController controller && controller.multiblock().isFormed();
        if (player.isSecondaryUseActive() || !formed) {
            return MultiblockControllerBlock.showStructure(level, pos, player);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
