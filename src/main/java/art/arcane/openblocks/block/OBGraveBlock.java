package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBGraveBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayer;

public final class OBGraveBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty BASE = BooleanProperty.create("base");
    private static final Map<Direction, VoxelShape> WITH_BASE = shapes(true);
    private static final Map<Direction, VoxelShape> WITHOUT_BASE = shapes(false);

    public OBGraveBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BASE, true));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BASE);
    }

    public static boolean hasBase(final BlockState below) {
        return below.is(BlockTags.DIRT) || below.is(Blocks.FARMLAND) || below.is(Blocks.DIRT_PATH);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(BASE, hasBase(context.getLevel().getBlockState(context.getClickedPos().below())));
    }

    @Override
    public BlockState updateShape(final BlockState state, final Direction side, final BlockState neighbor,
            final LevelAccessor level, final BlockPos pos, final BlockPos neighborPos) {
        return side == Direction.DOWN ? state.setValue(BASE, hasBase(neighbor)) : state;
    }

    @Override public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public BlockState mirror(final BlockState state, final Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return (state.getValue(BASE) ? WITH_BASE : WITHOUT_BASE).get(state.getValue(FACING));
    }

    private static Map<Direction, VoxelShape> shapes(final boolean base) {
        final int y = base ? 1 : 0;
        final var shapes = new EnumMap<Direction, VoxelShape>(Direction.class);
        shapes.put(Direction.NORTH, box(2, y, 14, 14, 15 + y, 16));
        shapes.put(Direction.EAST, box(0, y, 2, 2, 15 + y, 14));
        shapes.put(Direction.SOUTH, box(2, y, 0, 14, 15 + y, 2));
        shapes.put(Direction.WEST, box(14, y, 2, 16, 15 + y, 14));
        if (base) {
            final VoxelShape slab = box(0, 0, 0, 16, 1, 16);
            shapes.replaceAll((direction, board) -> Shapes.or(slab, board));
        }
        return Map.copyOf(shapes);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBGraveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, OBBlockEntities.GRAVE.get(), OBGraveBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
            @Nullable final LivingEntity placer, final ItemStack stack) {
        if (placer instanceof ServerPlayer player && !(player instanceof FakePlayer)
                && level.getBlockEntity(pos) instanceof OBGraveBlockEntity grave) {
            grave.nameMemorial(player, stack.hasCustomHoverName() ? stack.getHoverName().getString() : player.getScoreboardName());
        }
    }

    @Override
    public InteractionResult use(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof OBGraveBlockEntity graveBlockEntity)) return InteractionResult.PASS;

        if (player.getItemInHand(hand).canPerformAction(ToolActions.SHOVEL_DIG)) {
            if (graveBlockEntity.claimLoot(serverPlayer)) {
                player.getItemInHand(hand).hurtAndBreak(2, player, owner -> owner.broadcastBreakEvent(hand));
            }
        } else {
            graveBlockEntity.sendDeathMessage(serverPlayer);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onDestroyedByPlayer(final BlockState state, final Level level, final BlockPos pos,
            final Player player, final boolean willHarvest, final FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof OBGraveBlockEntity grave && grave.hasLoot()) return false;
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override public boolean canEntityDestroy(final BlockState state, final BlockGetter level, final BlockPos pos, final Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(final BlockState state, final Level level, final BlockPos pos, final Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof OBGraveBlockEntity grave && grave.hasLoot()) return;
        super.onBlockExploded(state, level, pos, explosion);
    }
}
