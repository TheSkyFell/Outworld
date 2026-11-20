package io.github.theskyfell.outworld.industry.logistics;

import io.github.theskyfell.outworld.core.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A conveyor belt: carries items on two lanes in the direction it faces (away from the player who
 * placed it). Items are data on the belt, not item entities. See {@link BeltBlockEntity}.
 *
 * <p>Right-click a belt with an item to put one on the lane you clicked; right-click with an empty hand
 * to take that lane's items.
 */
public final class BeltBlock extends MachineBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BeltShape> SHAPE = EnumProperty.create("shape", BeltShape.class);
    /** How high the belt's surface is; items ride on it. */
    public static final float HEIGHT = 3 / 16F;
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 3, 16);

    public BeltBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, BeltShape.STRAIGHT));
    }

    /** How the belt at {@code pos} facing {@code facing} is fed: see {@link BeltShape}. */
    public static BeltShape shapeFor(BlockGetter level, BlockPos pos, Direction facing) {
        if (feeds(level, pos.relative(facing.getOpposite()), facing)) {
            return BeltShape.STRAIGHT;
        }
        boolean fromLeft = feeds(level, pos.relative(facing.getCounterClockWise()), facing.getClockWise());
        boolean fromRight = feeds(level, pos.relative(facing.getClockWise()), facing.getCounterClockWise());
        if (fromLeft != fromRight) {
            return fromLeft ? BeltShape.LEFT : BeltShape.RIGHT;
        }
        return BeltShape.STRAIGHT;
    }

    /** Whether a belt at {@code pos} moves items in {@code direction}. */
    static boolean feeds(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BeltBlock && state.getValue(FACING) == direction;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing)
                .setValue(SHAPE, shapeFor(context.getLevel(), context.getClickedPos(), facing));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction.getAxis().isHorizontal()
                ? state.setValue(SHAPE, shapeFor(level, pos, state.getValue(FACING)))
                : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeltBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof BeltBlockEntity belt) {
            if (!belt.placeByHand(stack, hit.getLocation())) {
                return InteractionResult.FAIL;
            }
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof BeltBlockEntity belt) {
            for (ItemStack item : belt.takeByHand(hit.getLocation())) {
                if (!player.addItem(item)) {
                    Block.popResource(level, player.blockPosition(), item);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
