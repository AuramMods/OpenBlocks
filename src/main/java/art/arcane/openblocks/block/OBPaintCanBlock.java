package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBPaintCanBlockEntity;
import art.arcane.openblocks.item.OBPaintBrushItem;
import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBPaintCanBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 11, 12);

    public OBPaintCanBlock(final Properties properties) {
        super(properties);
    }

    @Override protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) { return new OBPaintCanBlockEntity(pos, state); }

    @Override public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof OBPaintCanBlockEntity can) can.fillFrom(stack);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        final ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !stack.is(OBItems.PAINTBRUSH.get())
                || !(level.getBlockEntity(pos) instanceof OBPaintCanBlockEntity can)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (can.amount() == 0) return InteractionResult.FAIL;
            if (stack.getCount() == 1) {
                OBPaintBrushItem.loadPaint(stack, can.color());
            } else {
                // Early 1.20.1 builds allowed brushes to stack.
                final ItemStack brush = stack.split(1);
                OBPaintBrushItem.loadPaint(brush, can.color());
                player.setItemInHand(hand, brush);
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
            can.takeCharge();
            if (can.amount() == 0) {
                level.removeBlock(pos, false);
                popResource(level, pos, new ItemStack(Items.BUCKET));
            }
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.1F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder loot) {
        final var drops = super.getDrops(state, loot);
        if (!(loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof OBPaintCanBlockEntity can)) return drops;
        return drops.stream().map(stack -> stack.is(asItem()) ? can.createStack() : stack).toList();
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final BlockGetter level, final BlockPos pos, final Player player) {
        return level.getBlockEntity(pos) instanceof OBPaintCanBlockEntity can ? can.createStack() : super.getCloneItemStack(state, target, level, pos, player);
    }
}
