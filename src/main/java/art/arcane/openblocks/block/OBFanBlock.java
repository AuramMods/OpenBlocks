package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBFanBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBFanBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = box(3.2, 0, 3.2, 12.8, 16, 12.8);

    public OBFanBlock(final Properties properties) { super(properties); }

    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) { return new OBFanBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return createTickerHelper(type, OBBlockEntities.FAN.get(), OBFanBlockEntity::tick);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final var state = defaultBlockState();
        return context.getClickedFace() == Direction.UP && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
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

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
        if (!level.isClientSide && placer != null && level.getBlockEntity(pos) instanceof OBFanBlockEntity fan) fan.setYaw(placer.getYHeadRot());
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OBFanBlockEntity fan) {
            fan.setYaw(fan.yaw() + (player.isSecondaryUseActive() ? -10 : 10));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) { return SHAPE; }
}
