package io.github.theskyfell.outworld.industry.pipe;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin block that carries something between the blocks around it: a cable or a pipe. Conduits that
 * touch form one grid in Core with a shared storage. They have no block entity and never tick. The arms
 * drawn towards neighbours are only looks; a conduit touches whatever is next to it. Right-clicking one
 * with an empty hand shows what its grid holds.
 */
public abstract class ConduitBlock extends Block {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;

    private final VoxelShape core;
    private final Map<Direction, VoxelShape> arms = new EnumMap<>(Direction.class);
    private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

    /** @param min where the conduit's square cross-section starts, in pixels; it ends at 16 - min */
    protected ConduitBlock(Properties properties, double min) {
        super(properties);
        double max = 16 - min;
        core = Block.box(min, min, min, max, max, max);
        arms.put(Direction.NORTH, Block.box(min, min, 0, max, max, min));
        arms.put(Direction.SOUTH, Block.box(min, min, max, max, max, 16));
        arms.put(Direction.WEST, Block.box(0, min, min, min, max, max));
        arms.put(Direction.EAST, Block.box(max, min, min, 16, max, max));
        arms.put(Direction.DOWN, Block.box(min, 0, min, max, min, max));
        arms.put(Direction.UP, Block.box(min, max, min, max, 16, max));
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    /** Whether to draw an arm towards this neighbour. */
    protected abstract boolean connectsTo(BlockState neighbour);

    /** What the grid at {@code pos} holds, for a player who right-clicks it. */
    protected abstract Component describeGrid(ServerLevel level, BlockPos pos);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = context.getClickedPos().relative(direction);
            state = state.setValue(CONNECTIONS.get(direction), connectsTo(context.getLevel().getBlockState(neighbour)));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.setValue(CONNECTIONS.get(direction), connectsTo(neighbourState));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, this::shapeOf);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(describeGrid(serverLevel, pos), true);
        }
        return InteractionResult.SUCCESS;
    }

    private VoxelShape shapeOf(BlockState state) {
        VoxelShape shape = core;
        for (Direction direction : Direction.values()) {
            if (state.getValue(CONNECTIONS.get(direction))) {
                shape = Shapes.or(shape, arms.get(direction));
            }
        }
        return shape;
    }
}
