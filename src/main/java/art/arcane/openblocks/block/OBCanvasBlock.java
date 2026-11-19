package art.arcane.openblocks.block;

import art.arcane.openblocks.api.OBPaintableBlock;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.ForgeRegistries;

public final class OBCanvasBlock extends BaseEntityBlock implements SimpleWaterloggedBlock, OBPaintableBlock {

    private final boolean glass;

    public OBCanvasBlock(final Properties properties, final boolean glass) {
        super(properties.dynamicShape());
        this.glass = glass;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBCanvasBlockEntity((glass ? OBBlockEntities.CANVAS_GLASS : OBBlockEntities.CANVAS).get(), pos, state);
    }

    @Override
    public boolean applyPaint(final Level level, final BlockPos pos, final int rgb, final Direction... faces) {
        return level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas && canvas.paint(rgb, faces);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        final ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || held.is(OBItems.PAINTBRUSH.get()) || held.is(OBItems.SQUEEGEE.get())
                || held.is(OBItems.STENCIL.get()) || !level.mayInteract(player, pos)
                || !(level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas)
                || canvas.face(hit.getDirection()).cover() == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) canvas.recoverStencil(player, hit.getDirection());
            else canvas.rotateStencil(hit.getDirection());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState replacement, final boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide() && !level.restoringBlockSnapshots && !level.captureBlockSnapshots
                && level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas) canvas.dropStencils();
        super.onRemove(state, level, pos, replacement, moving);
    }

    public static boolean canWrap(final Level level, final BlockPos pos, final BlockState state) {
        final Block block = state.getBlock();
        if (state.isAir() || state.hasBlockEntity() || state.getRenderShape() != RenderShape.MODEL
                || state.getDestroySpeed(level, pos) < 0 || block instanceof BedBlock || block instanceof DoorBlock
                || block instanceof DoublePlantBlock || block instanceof PistonHeadBlock
                || block instanceof PistonBaseBlock && state.getValue(BlockStateProperties.EXTENDED)
                || !state.getFluidState().isEmpty() && !state.hasProperty(BlockStateProperties.WATERLOGGED)) return false;
        final ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null && (!OBServerConfig.SPEC.isLoaded() || !OBServerConfig.CANVAS_BLACKLIST.get().contains(id.toString()));
    }

    @Nullable
    public static OBCanvasBlockEntity wrap(final Level level, final BlockPos pos, final BlockState original) {
        final Block block = (original.isSolidRender(level, pos) ? OBBlocks.CANVAS : OBBlocks.CANVAS_GLASS).get();
        final BlockState wrapper = block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, original.getFluidState().is(Fluids.WATER));
        if (!level.setBlock(pos, wrapper, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE)) return null;
        if (!(level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas)) {
            level.setBlockAndUpdate(pos, original);
            return null;
        }
        canvas.remember(original);
        return canvas;
    }

    @Nullable
    private static BlockState underlying(final BlockGetter level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas && !canvas.original().isAir()) return canvas.original();
        return null;
    }

    @Override
    public boolean supportsExternalFaceHiding(final BlockState state) {
        return true;
    }

    @Override
    public boolean hidesNeighborFace(final BlockGetter level, final BlockPos pos, final BlockState state,
            final BlockState adjacent, final Direction side) {
        final BlockState original = underlying(level, pos);
        if (original == null) return false;
        final BlockState neighbor = adjacent.getBlock() instanceof OBCanvasBlock ? underlying(level, pos.relative(side)) : adjacent;
        return neighbor != null && (neighbor.skipRendering(original, side.getOpposite())
                || neighbor.supportsExternalFaceHiding() && original.hidesNeighborFace(level, pos, neighbor, side));
    }

    @Override
    public FluidState getFluidState(final BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlaceLiquid(final BlockGetter level, final BlockPos pos, final BlockState state, final Fluid fluid) {
        final BlockState original = underlying(level, pos);
        return original != null && original.hasProperty(BlockStateProperties.WATERLOGGED)
                && SimpleWaterloggedBlock.super.canPlaceLiquid(level, pos, state, fluid);
    }

    @Override
    public BlockState updateShape(final BlockState state, final Direction side, final BlockState neighbor, final LevelAccessor level,
            final BlockPos pos, final BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        final BlockState original = underlying(level, pos);
        return original == null ? super.getShape(state, level, pos, context) : original.getShape(level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        final BlockState original = underlying(level, pos);
        return original == null ? super.getCollisionShape(state, level, pos, context) : original.getCollisionShape(level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        final BlockState original = underlying(level, pos);
        return original == null ? super.getOcclusionShape(state, level, pos) : original.getOcclusionShape(level, pos);
    }

    @Override
    public int getLightEmission(final BlockState state, final BlockGetter level, final BlockPos pos) {
        final BlockState original = underlying(level, pos);
        return original == null ? super.getLightEmission(state, level, pos) : original.getLightEmission(level, pos);
    }

    @Override
    public int getLightBlock(final BlockState state, final BlockGetter level, final BlockPos pos) {
        final BlockState original = underlying(level, pos);
        return original == null ? super.getLightBlock(state, level, pos) : original.getLightBlock(level, pos);
    }

    @Override
    public float getDestroyProgress(final BlockState state, final Player player, final BlockGetter level, final BlockPos pos) {
        final BlockState original = underlying(level, pos);
        return original == null ? super.getDestroyProgress(state, player, level, pos) : original.getDestroyProgress(player, level, pos);
    }

    @Override
    public float getExplosionResistance(final BlockState state, final BlockGetter level, final BlockPos pos, final Explosion explosion) {
        final BlockState original = underlying(level, pos);
        return original == null ? super.getExplosionResistance(state, level, pos, explosion) : original.getExplosionResistance(level, pos, explosion);
    }

    @Override
    public SoundType getSoundType(final BlockState state, final LevelReader level, final BlockPos pos, @Nullable final Entity entity) {
        final BlockState original = underlying(level, pos);
        return original == null ? super.getSoundType(state, level, pos, entity) : original.getSoundType(level, pos, entity);
    }

    @Override
    public MapColor getMapColor(final BlockState state, final BlockGetter level, final BlockPos pos, final MapColor fallback) {
        final BlockState original = underlying(level, pos);
        return original == null ? fallback : original.getMapColor(level, pos);
    }

    @Override
    public boolean canHarvestBlock(final BlockState state, final BlockGetter level, final BlockPos pos, final Player player) {
        final BlockState original = underlying(level, pos);
        return original == null ? ForgeHooks.isCorrectToolForDrops(state, player) : original.canHarvestBlock(level, pos, player);
    }

    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder loot) {
        if (loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof OBCanvasBlockEntity canvas && !canvas.original().isAir()) {
            final BlockState original = canvas.original();
            return original.getDrops(loot.withOptionalParameter(LootContextParams.BLOCK_ENTITY, null));
        }
        return super.getDrops(state, loot);
    }

    @Override
    public void playerDestroy(final Level level, final Player player, final BlockPos pos, final BlockState state,
            @Nullable final BlockEntity tile, final ItemStack tool) {
        if (tile instanceof OBCanvasBlockEntity canvas && !canvas.original().isAir()) {
            final BlockState original = canvas.original();
            original.getBlock().playerDestroy(level, player, pos, original, null, tool);
        } else {
            super.playerDestroy(level, player, pos, state, tile, tool);
        }
    }

    @Override
    public void spawnAfterBreak(final BlockState state, final ServerLevel level, final BlockPos pos, final ItemStack tool, final boolean dropXp) {
        final BlockState original = underlying(level, pos);
        if (original != null) original.spawnAfterBreak(level, pos, tool, dropXp);
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final BlockGetter level, final BlockPos pos, final Player player) {
        final BlockState original = underlying(level, pos);
        return original == null ? super.getCloneItemStack(state, target, level, pos, player)
                : original.getCloneItemStack(target, level, pos, player);
    }

    @Override
    public boolean isSignalSource(final BlockState state) { return true; }

    @Override
    public int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        final BlockState original = underlying(level, pos);
        return original == null ? 0 : original.getSignal(level, pos, side);
    }

    @Override
    public int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction side) {
        final BlockState original = underlying(level, pos);
        return original == null ? 0 : original.getDirectSignal(level, pos, side);
    }

    @Override
    public boolean hasAnalogOutputSignal(final BlockState state) { return true; }

    @Override
    public int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        final BlockState original = underlying(level, pos);
        return original == null || !original.hasAnalogOutputSignal() ? 0 : original.getAnalogOutputSignal(level, pos);
    }

    @Override
    public void stepOn(final Level level, final BlockPos pos, final BlockState state, final Entity entity) {
        final BlockState original = underlying(level, pos);
        if (original != null) original.getBlock().stepOn(level, pos, original, entity);
    }
}
