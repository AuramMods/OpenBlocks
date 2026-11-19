package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBVillageHighlighterBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBVillageHighlighterBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape NORTH_SOUTH = Shapes.or(box(0, 0, 1, 16, 14, 15), box(2, 14, 3, 14, 16, 13));
    private static final VoxelShape EAST_WEST = Shapes.or(box(1, 0, 0, 15, 14, 16), box(3, 14, 2, 13, 16, 14));

    public OBVillageHighlighterBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, POWERED); }
    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) { return new OBVillageHighlighterBlockEntity(pos, state); }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override public BlockState rotate(final BlockState state, final Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(final BlockState state, final Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, OBBlockEntities.VILLAGE_HIGHLIGHTER.get(), OBVillageHighlighterBlockEntity::serverTick);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block,
            final BlockPos neighbor, final boolean moving) {
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof OBVillageHighlighterBlockEntity highlighter) {
            highlighter.refreshInput(server);
        }
    }

    @Override public boolean isSignalSource(final BlockState state) { return true; }

    @Override
    public int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        return level.getBlockEntity(pos) instanceof OBVillageHighlighterBlockEntity highlighter ? highlighter.signal() : 0;
    }

    @Override
    public int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        return getSignal(state, level, pos, side);
    }
}
