package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBBearTrapBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBBearTrapBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final double BASE_HEIGHT = 1.0 / 16.0;
    private static final VoxelShape OUTLINE = box(0.5, 0, 0.5, 15.5, 8, 15.5);
    private static final VoxelShape BASE = box(1.6, 0, 1.6, 14.4, BASE_HEIGHT * 16, 14.4);

    public OBBearTrapBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
    }

    @Override protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS); }
    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) { return new OBBearTrapBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, OBBlockEntities.BEARTRAP.get(), OBBearTrapBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final var state = defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
        return context.getClickedFace() == Direction.UP && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(AXIS, rotation.rotate(Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS))).getAxis());
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final var support = pos.below();
        return level.getBlockState(support).isFaceSturdy(level, support, Direction.UP);
    }

    @Override
    public BlockState updateShape(final BlockState state, final Direction side, final BlockState neighbor,
            final LevelAccessor level, final BlockPos pos, final BlockPos neighborPos) {
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) { return OUTLINE; }
    @Override public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) { return BASE; }
    @Override public VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) { return Shapes.empty(); }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OBBearTrapBlockEntity trap) trap.release();
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighbor, final BlockPos neighborPos, final boolean moving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OBBearTrapBlockEntity trap && trap.isShut() && level.hasNeighborSignal(pos)) {
            trap.release();
        }
    }

    @Override
    public void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (!level.isClientSide && entity instanceof PathfinderMob mob && level.getBlockEntity(pos) instanceof OBBearTrapBlockEntity trap) {
            trap.tryCapture(mob);
        }
    }

    @Override public boolean hasAnalogOutputSignal(final BlockState state) { return true; }

    @Override
    public int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof OBBearTrapBlockEntity trap ? trap.comparatorOutput() : 0;
    }
}
