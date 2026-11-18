package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBFoldingTargetBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBFoldingTargetBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape FOLDED = box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape RAILS_Z = Shapes.or(box(0, 0, 0, 1, 1, 16), box(15, 0, 0, 16, 1, 16));
    private static final VoxelShape RAILS_X = Shapes.or(box(0, 0, 0, 16, 1, 1), box(0, 0, 15, 16, 1, 16));
    private static final VoxelShape NORTH = Shapes.or(RAILS_Z, box(0, 1, 0, 16, 16, 1));
    private static final VoxelShape SOUTH = Shapes.or(RAILS_Z, box(0, 1, 15, 16, 16, 16));
    private static final VoxelShape WEST = Shapes.or(RAILS_X, box(0, 1, 0, 1, 16, 16));
    private static final VoxelShape EAST = Shapes.or(RAILS_X, box(15, 1, 0, 16, 16, 16));

    public OBFoldingTargetBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, POWERED); }
    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) { return new OBFoldingTargetBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, OBBlockEntities.TARGET.get(), OBFoldingTargetBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final var state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
        return context.getClickedFace() == Direction.UP && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final BlockPos support = pos.below();
        return level.getBlockState(support).isFaceSturdy(level, support, Direction.UP);
    }

    @Override
    public BlockState updateShape(final BlockState state, final Direction side, final BlockState neighbor,
            final LevelAccessor level, final BlockPos pos, final BlockPos neighborPos) {
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public BlockState rotate(final BlockState state, final Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(final BlockState state, final Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        if (!state.getValue(POWERED)) return FOLDED;
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override public VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) { return Shapes.empty(); }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        refreshDeployment(level, pos);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block,
            final BlockPos neighbor, final boolean moving) {
        if (level instanceof ServerLevel server) refreshDeployment(server, pos);
    }

    private void refreshDeployment(final ServerLevel level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (!state.is(this) || !(level.getBlockEntity(pos) instanceof OBFoldingTargetBlockEntity target)) return;
        final boolean raised = target.hasInputPower(level);
        if (raised == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, raised), UPDATE_ALL);
        if (!raised) target.releaseArrows(level);
        level.playSound(null, pos, (raised ? OBSounds.TARGET_OPEN : OBSounds.TARGET_CLOSE).get(), SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    @Override
    public void onProjectileHit(final Level level, final BlockState state, final BlockHitResult hit, final Projectile projectile) {
        if (level.isClientSide || !state.getValue(POWERED) || !(projectile instanceof AbstractArrow arrow) || arrow instanceof ThrownTrident) return;
        final BlockPos pos = hit.getBlockPos();
        if (hit.getLocation().y <= pos.getY() + 1.0 / 16.0 || !(level.getBlockEntity(pos) instanceof OBFoldingTargetBlockEntity target)) return;
        final Vec3 front = Vec3.atLowerCornerOf(state.getValue(FACING).getNormal()).scale(0.5);
        final Vec3 bullseye = Vec3.atCenterOf(pos).add(front).add(0, 0.05, 0);
        final int score = 15 - Mth.clamp(Mth.floor(hit.getLocation().distanceTo(bullseye) * 32), 0, 15);
        target.recordHit(arrow, score);
    }

    @Override public boolean isSignalSource(final BlockState state) { return true; }
    @Override public int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        return level.getBlockEntity(pos) instanceof OBFoldingTargetBlockEntity target ? target.outputPower() : 0;
    }

    @Override public int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        return getSignal(state, level, pos, side);
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState replacement, final boolean moving) {
        if (!state.is(replacement.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof OBFoldingTargetBlockEntity target) target.releaseArrows(server);
        super.onRemove(state, level, pos, replacement, moving);
    }
}
