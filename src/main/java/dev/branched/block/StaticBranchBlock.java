package dev.branched.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Static, non-simulated 6-way connected 3D branch block.
 *
 * <p>It owns no BlockEntity, overrides no {@code randomTick}, schedules no ticks
 * and keeps no per-tree state. Every directional combination maps to one of 64
 * precomputed {@link VoxelShape}s baked once in an immutable static list.</p>
 */
public final class StaticBranchBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final IntegerProperty THICKNESS = IntegerProperty.create("thickness", 1, 3);

    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = buildPropertyMap();
    private static final List<VoxelShape> SHAPES = buildShapes();

    public StaticBranchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(THICKNESS, 2));
    }

    private static Map<Direction, BooleanProperty> buildPropertyMap() {
        EnumMap<Direction, BooleanProperty> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, NORTH);
        map.put(Direction.SOUTH, SOUTH);
        map.put(Direction.EAST, EAST);
        map.put(Direction.WEST, WEST);
        map.put(Direction.UP, UP);
        map.put(Direction.DOWN, DOWN);
        return Collections.unmodifiableMap(map);
    }

    private static List<VoxelShape> buildShapes() {
        double[][] bounds = {
                {5.0D, 11.0D}, // thickness 1: 6x6px thin
                {4.0D, 12.0D}, // thickness 2: 8x8px medium
                {2.0D, 14.0D}  // thickness 3: 12x12px thick (75% trunk width)
        };
        VoxelShape[] table = new VoxelShape[192];
        for (int tier = 0; tier < 3; tier++) {
            double min = bounds[tier][0];
            double max = bounds[tier][1];
            VoxelShape core = Block.box(min, min, min, max, max, max);
            VoxelShape[] sleeves = {
                    Block.box(min, min, 0.0D, max, max, min),   // 1: NORTH
                    Block.box(min, min, max, max, max, 16.0D),  // 2: SOUTH
                    Block.box(max, min, min, 16.0D, max, max),  // 4: EAST
                    Block.box(0.0D, min, min, min, max, max),   // 8: WEST
                    Block.box(min, max, min, max, 16.0D, max),  // 16: UP
                    Block.box(min, 0.0D, min, max, min, max)    // 32: DOWN
            };
            int baseIdx = tier << 6;
            for (int mask = 0; mask < 64; mask++) {
                VoxelShape shape = core;
                for (int bit = 0; bit < 6; bit++) {
                    if ((mask & (1 << bit)) != 0) {
                        shape = Shapes.or(shape, sleeves[bit]);
                    }
                }
                table[baseIdx | mask] = shape;
            }
        }
        return List.of(table);
    }

    private static int shapeIndex(BlockState state) {
        int mask = 0;
        if (state.getValue(NORTH)) mask |= 1;
        if (state.getValue(SOUTH)) mask |= 2;
        if (state.getValue(EAST)) mask |= 4;
        if (state.getValue(WEST)) mask |= 8;
        if (state.getValue(UP)) mask |= 16;
        if (state.getValue(DOWN)) mask |= 32;
        int tier = Math.max(0, Math.min(2, state.getValue(THICKNESS) - 1));
        return (tier << 6) | mask;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, THICKNESS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPES.get(shapeIndex(state));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return SHAPES.get(shapeIndex(state));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH, canConnectTo(level.getBlockState(pos.north())))
                .setValue(SOUTH, canConnectTo(level.getBlockState(pos.south())))
                .setValue(EAST, canConnectTo(level.getBlockState(pos.east())))
                .setValue(WEST, canConnectTo(level.getBlockState(pos.west())))
                .setValue(UP, canConnectTo(level.getBlockState(pos.above())))
                .setValue(DOWN, canConnectTo(level.getBlockState(pos.below())));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction), canConnectTo(neighborState));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            rotated = rotated.setValue(propertyFor(rotation.rotate(dir)), state.getValue(propertyFor(dir)));
        }
        return rotated;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mirrored = mirrored.setValue(propertyFor(mirror.mirror(dir)), state.getValue(propertyFor(dir)));
        }
        return mirrored;
    }

    public static boolean canConnectTo(BlockState neighborState) {
        return neighborState.getBlock() instanceof StaticBranchBlock
                || neighborState.is(BlockTags.LOGS);
    }

    public static BooleanProperty propertyFor(Direction direction) {
        return PROPERTY_BY_DIRECTION.get(direction);
    }

    public static boolean isConnected(BlockState state, Direction direction) {
        BooleanProperty prop = propertyFor(direction);
        return prop != null && state.hasProperty(prop) && state.getValue(prop);
    }

    public static BlockState withConnection(BlockState state, Direction direction, boolean connected) {
        BooleanProperty prop = propertyFor(direction);
        return prop == null ? state : state.setValue(prop, connected);
    }

    public static BlockState withConnections(BlockState state,
                                            boolean north, boolean south,
                                            boolean east, boolean west,
                                            boolean up, boolean down) {
        return state
                .setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(EAST, east)
                .setValue(WEST, west)
                .setValue(UP, up)
                .setValue(DOWN, down);
    }

    /** Clamps gauge to [1..3] (1=thin 6px, 2=medium 8px, 3=thick 10px) and updates {@link #THICKNESS}. */
    public static BlockState withThickness(BlockState state, int thickness) {
        int clamped = Math.max(1, Math.min(3, thickness));
        return state.hasProperty(THICKNESS) ? state.setValue(THICKNESS, clamped) : state;
    }

    /** Compatibility bridge mapping a legacy single-direction {@link BranchShape} to the 6-way properties. */
    public static BlockState withShape(BlockState state, BranchShape shape) {
        BlockState base = withConnections(state, false, false, false, false, false, false);
        if (shape == null || shape == BranchShape.CORE) {
            return base;
        }
        Direction direction = shape.direction();
        return direction == null ? base : base.setValue(propertyFor(direction), true);
    }
}
