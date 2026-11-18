package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBBigButtonBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBBigButtonBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape[] RELEASED = faces(2);
    private static final VoxelShape[] PRESSED = faces(1);
    private final boolean wooden;

    public OBBigButtonBlock(final boolean wooden, final Properties properties) {
        super(properties);
        this.wooden = wooden;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false));
    }

    private static VoxelShape[] faces(final double depth) {
        final var faces = new VoxelShape[Direction.values().length];
        faces[Direction.UP.ordinal()] = box(1, 0, 1, 15, depth, 15);
        faces[Direction.DOWN.ordinal()] = box(1, 16 - depth, 1, 15, 16, 15);
        faces[Direction.NORTH.ordinal()] = box(1, 1, 16 - depth, 15, 15, 16);
        faces[Direction.SOUTH.ordinal()] = box(1, 1, 0, 15, 15, depth);
        faces[Direction.WEST.ordinal()] = box(16 - depth, 1, 1, 16, 15, 15);
        faces[Direction.EAST.ordinal()] = box(0, 1, 1, depth, 15, 15);
        return faces;
    }

    @Override protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, POWERED); }
    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) { return new OBBigButtonBlockEntity(wooden, pos, state); }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final var state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final Direction face = state.getValue(FACING);
        final BlockPos support = pos.relative(face.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, face);
    }

    @Override
    public BlockState updateShape(final BlockState state, final Direction side, final BlockState neighbor,
            final LevelAccessor level, final BlockPos pos, final BlockPos neighborPos) {
        return side == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public BlockState rotate(final BlockState state, final Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(final BlockState state, final Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return (state.getValue(POWERED) ? PRESSED : RELEASED)[state.getValue(FACING).ordinal()];
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        // Keep arrows embedded when the wooden plate moves inward.
        return wooden ? RELEASED[state.getValue(FACING).ordinal()] : getShape(state, level, pos, context);
    }

    @Override public VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) { return Shapes.empty(); }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OBBigButtonBlockEntity button) {
            if (player.isSecondaryUseActive() && player.getMainHandItem().isEmpty()) player.openMenu(button);
            else if (!state.getValue(POWERED)) press(state, level, pos, button.pulseLength());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onProjectileHit(final Level level, final BlockState state, final BlockHitResult hit, final Projectile projectile) {
        if (wooden && !level.isClientSide && !state.getValue(POWERED) && projectile instanceof AbstractArrow
                && level.getBlockEntity(hit.getBlockPos()) instanceof OBBigButtonBlockEntity button) {
            press(state, level, hit.getBlockPos(), button.pulseLength());
        }
    }

    private void press(final BlockState state, final Level level, final BlockPos pos, final int ticks) {
        setPowered(state, level, pos, true);
        level.scheduleTick(pos, this, ticks);
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!state.getValue(POWERED)) return;
        final var arrowArea = RELEASED[state.getValue(FACING).ordinal()].bounds().inflate(0.07).move(pos);
        if (wooden && level.getBlockEntity(pos) instanceof OBBigButtonBlockEntity button
                && !level.getEntitiesOfClass(AbstractArrow.class, arrowArea, AbstractArrow::isAlive).isEmpty()) {
            level.scheduleTick(pos, this, button.pulseLength());
        } else {
            setPowered(state, level, pos, false);
        }
    }

    private void setPowered(final BlockState state, final Level level, final BlockPos pos, final boolean powered) {
        level.setBlock(pos, state.setValue(POWERED, powered), UPDATE_ALL);
        notifySupport(state, level, pos);
        level.playSound(null, pos, powered ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
                SoundSource.BLOCKS, 0.3F, powered ? 0.6F : 0.5F);
    }

    private void notifySupport(final BlockState state, final Level level, final BlockPos pos) {
        level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), this);
    }

    @Override public boolean isSignalSource(final BlockState state) { return true; }

    @Override
    public int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        return side == state.getValue(FACING) ? getSignal(state, level, pos, side) : 0;
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState next, final boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof OBBigButtonBlockEntity button) button.dropItems(level);
            if (state.getValue(POWERED)) notifySupport(state, level, pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
