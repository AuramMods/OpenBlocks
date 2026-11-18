package art.arcane.openblocks.block;

import art.arcane.openblocks.config.OBServerConfig;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBRopeLadderBlock extends HorizontalDirectionalBlock {

    public static final BooleanProperty RECOVERABLE = BooleanProperty.create("recoverable");

    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(15, 0, 0, 16, 16, 16);

    public OBRopeLadderBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(RECOVERABLE, true));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RECOVERABLE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final Direction clickedFace = context.getClickedFace();
        final BlockState above = level.getBlockState(pos.above());

        if (clickedFace == Direction.DOWN) {
            return above.is(this) ? defaultBlockState().setValue(FACING, above.getValue(FACING)) : null;
        }
        if (clickedFace.getAxis().isHorizontal() && hasSupport(level, pos, clickedFace)) {
            return defaultBlockState().setValue(FACING, clickedFace);
        }
        for (final Direction direction : context.getNearestLookingDirections()) {
            final Direction facing = direction.getOpposite();
            if (facing.getAxis().isHorizontal() && hasSupport(level, pos, facing)) {
                return defaultBlockState().setValue(FACING, facing);
            }
        }
        return null;
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return hasSupport(level, pos, state.getValue(FACING)) || level.getBlockState(pos.above()).is(this);
    }

    private static boolean hasSupport(final BlockGetter level, final BlockPos pos, final Direction facing) {
        return !level.getBlockState(pos.relative(facing.getOpposite())).isAir();
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> SOUTH_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LivingEntity entity
                && entity.getDirection().getOpposite() != state.getValue(FACING)) return Shapes.empty();
        return getShape(state, level, pos, context);
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
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
            final Block neighbor, final BlockPos neighborPos, final boolean moving) {
        final BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        if (!level.isClientSide && (neighborPos.equals(support) || neighborPos.equals(pos.above()))
                && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            @Nullable final LivingEntity placer, final ItemStack stack) {
        if (level.isClientSide || !(placer instanceof Player player)) return;
        final boolean infinite = OBServerConfig.ROPE_LADDER_INFINITE.get();
        final BlockState rung = state.setValue(RECOVERABLE, !infinite);
        if (rung != state && !level.setBlock(pos, rung, Block.UPDATE_CLIENTS)) return;

        int remaining = infinite ? pos.getY() - level.getMinBuildHeight() : stack.getCount() - 1;
        final CollisionContext collision = CollisionContext.of(player);
        BlockPos next = pos.below();
        while (remaining > 0 && next.getY() >= level.getMinBuildHeight()) {
            if (!level.isEmptyBlock(next) || !level.mayInteract(player, next) || !player.mayUseItemAt(next, Direction.DOWN, stack)
                    || !level.isUnobstructed(rung, next, collision) || !level.setBlock(next, rung, Block.UPDATE_ALL)) return;
            if (!infinite && !player.getAbilities().instabuild) stack.shrink(1);
            remaining--;
            next = next.below();
        }
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos,
            final BlockState replacement, final boolean moving) {
        if (!level.isClientSide && !replacement.is(this) && !level.restoringBlockSnapshots) {
            // Break from the bottom up to keep long ropes from recursing once per rung.
            BlockPos bottom = pos.below();
            while (bottom.getY() >= level.getMinBuildHeight() && level.getBlockState(bottom).is(this)) {
                bottom = bottom.below();
            }
            for (BlockPos rung = bottom.above(); rung.getY() < pos.getY(); rung = rung.above()) {
                if (level.getBlockState(rung).is(this)) level.destroyBlock(rung, true);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
