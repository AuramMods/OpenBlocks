package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBPaintMixerBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public final class OBPaintMixerBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);

    public OBPaintMixerBlock(final Properties properties) {
        super(properties);
    }

    @Override protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) { return new OBPaintMixerBlockEntity(pos, state); }

    @Override public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, OBBlockEntities.PAINT_MIXER.get(), OBPaintMixerBlockEntity::tick);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof OBPaintMixerBlockEntity mixer) {
            NetworkHooks.openScreen(server, mixer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState replacement, final boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof OBPaintMixerBlockEntity mixer) mixer.dropInventory();
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder loot) {
        final var drops = super.getDrops(state, loot);
        if (loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof OBPaintMixerBlockEntity mixer) {
            for (final ItemStack stack : drops) {
                if (stack.is(asItem())) BlockItem.setBlockEntityData(stack, OBBlockEntities.PAINT_MIXER.get(), mixer.recipeSettings());
            }
        }
        return drops;
    }
}
