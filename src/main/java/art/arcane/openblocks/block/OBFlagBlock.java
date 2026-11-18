package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBFlagBlockEntity;
import art.arcane.openblocks.item.OBFlagItem;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBFlagBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", direction -> direction != Direction.DOWN);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final DyeColor DEFAULT_COLOR = DyeColor.LIME;
    private static final VoxelShape POLE = box(7, 0, 7, 9, 16, 9);
    private static final VoxelShape NORTH_SOUTH = box(7, 0, 3, 9, 16, 13);
    private static final VoxelShape EAST_WEST = box(3, 0, 7, 13, 16, 9);

    public OBFlagBlock(final Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(COLOR, DEFAULT_COLOR));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLOR);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBFlagBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction facing = context.getClickedFace();
        if (facing == Direction.DOWN) return null;
        final BlockState state = defaultBlockState().setValue(FACING, facing)
                .setValue(COLOR, OBFlagItem.getColor(context.getItemInHand()));
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final Direction facing = state.getValue(FACING);
        final BlockPos supportPos = pos.relative(facing.getOpposite());
        final BlockState support = level.getBlockState(supportPos);
        return support.isFaceSturdy(level, supportPos, facing) || (facing == Direction.UP
                && (support.getBlock() instanceof FenceBlock || (support.is(this) && support.getValue(FACING) == Direction.UP)));
    }

    @Override
    public BlockState updateShape(final BlockState state, final Direction side, final BlockState neighbor,
            final LevelAccessor level, final BlockPos pos, final BlockPos neighborPos) {
        if (side == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> EAST_WEST;
            case Z -> NORTH_SOUTH;
            case Y -> POLE;
        };
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            @Nullable final LivingEntity placer, final ItemStack stack) {
        if (!level.isClientSide && placer != null && state.getValue(FACING) == Direction.UP
                && level.getBlockEntity(pos) instanceof OBFlagBlockEntity flag) {
            flag.setYaw(Mth.floor(placer.getYHeadRot() / 10) * 10);
        }
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (state.getValue(FACING) != Direction.UP || hand != InteractionHand.MAIN_HAND
                || player.getMainHandItem().is(asItem()) || !(level.getBlockEntity(pos) instanceof OBFlagBlockEntity flag)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) flag.setYaw(flag.yaw() + (player.isShiftKeyDown() ? -10 : 10));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final BlockGetter level,
            final BlockPos pos, final Player player) {
        return OBFlagItem.create(state.getValue(COLOR));
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return state.getValue(FACING) == Direction.UP ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }
}
