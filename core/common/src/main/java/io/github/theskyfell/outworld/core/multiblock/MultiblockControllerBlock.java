package io.github.theskyfell.outworld.core.multiblock;

import io.github.theskyfell.outworld.core.machine.MachineBlock;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The base of every multiblock controller block. Its block entity must be a
 * {@link MultiblockControllerBlockEntity}.
 *
 * <p>It faces away from the player who places it, and the structure turns with it. Right-clicking it
 * while holding a block the structure needs places that block in the nearest empty spot that needs
 * it. Otherwise, right-clicking it while sneaking (with an empty hand) shows or hides the build
 * preview and tells the player what is missing; a plain right-click calls {@link #useController},
 * which does the same unless the controller does something else, such as opening its screen.
 */
public abstract class MultiblockControllerBlock extends MachineBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static BiConsumer<Level, BlockPos> previewToggle = (level, pos) -> { };

    protected MultiblockControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Called by Core's client code at startup: shows or hides the build preview of a controller. */
    public static void setPreviewToggle(BiConsumer<Level, BlockPos> toggle) {
        previewToggle = toggle;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem blockItem
                && level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity controller) {
            BlockPos target = controller.nextSpotFor(level, blockItem.getBlock(), player.getEyePosition());
            if (target != null) {
                return level.isClientSide() ? InteractionResult.SUCCESS : placeInStructure(blockItem, stack, level, target, player, hand);
            }
        }
        // Not a block the structure needs, or nowhere free to put it: act as if the hand were empty.
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Places the held block at {@code target} as if the player had clicked there. */
    private static InteractionResult placeInStructure(BlockItem blockItem, ItemStack stack, Level level, BlockPos target,
            Player player, InteractionHand hand) {
        if (!player.mayUseItemAt(target, Direction.UP, stack) || !level.mayInteract(player, target)) {
            return InteractionResult.FAIL;
        }
        BlockPlaceContext context = new BlockPlaceContext(player, hand, stack,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
        // Vanilla's own placement: block state, sounds, using up the item in survival, entity checks.
        return blockItem.place(context).consumesAction() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) {
            return showStructure(level, pos, player);
        }
        return useController(state, level, pos, player, hit);
    }

    /** A plain right-click with an empty hand. Shows the structure unless overridden. */
    protected InteractionResult useController(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return showStructure(level, pos, player);
    }

    /** Toggles the build preview on the client and sends the structure's status from the server. */
    protected final InteractionResult showStructure(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity controller) {
            if (level.isClientSide()) {
                previewToggle.accept(level, pos);
            } else if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(controller.status(), false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
