package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBGuideBlockEntity;
import art.arcane.openblocks.guide.OBGuideControls;
import art.arcane.openblocks.guide.OBGuideOrientation;
import art.arcane.openblocks.guide.OBGuideShape;
import art.arcane.openblocks.guide.OBGuideShape.Bounds;
import art.arcane.openblocks.item.OBPaintColors;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class OBGuideBlock extends BaseEntityBlock {

    public static final EnumProperty<OBGuideOrientation> ORIENTATION = EnumProperty.create("orientation", OBGuideOrientation.class);
    private final boolean builder;

    public OBGuideBlock(final Properties properties, final boolean builder) {
        super(properties.lightLevel(state -> 9));
        this.builder = builder;
        registerDefaultState(stateDefinition.any().setValue(ORIENTATION, OBGuideOrientation.XP_YP).setValue(BlockStateProperties.POWERED, false));
    }

    public boolean builder() { return builder; }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> definition) {
        definition.add(ORIENTATION, BlockStateProperties.POWERED);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(ORIENTATION, OBGuideOrientation.forSurface(context.getClickedFace()))
                .setValue(BlockStateProperties.POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(ORIENTATION, state.getValue(ORIENTATION).rotate(rotation));
    }

    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return (builder ? OBBlockEntities.BUILDER_GUIDE : OBBlockEntities.GUIDE).get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return builder && !level.isClientSide() ? createTickerHelper(type, OBBlockEntities.BUILDER_GUIDE.get(), OBGuideBlockEntity::tick) : null;
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState previous, final boolean moving) {
        if (!state.is(previous.getBlock())) updatePower(state, level, pos);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighbor,
            final BlockPos neighborPos, final boolean moving) {
        updatePower(state, level, pos);
    }

    private static void updatePower(final BlockState state, final Level level, final BlockPos pos) {
        if (level.isClientSide()) return;
        final boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(BlockStateProperties.POWERED) != powered) {
            level.setBlock(pos, state.setValue(BlockStateProperties.POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof OBGuideBlockEntity guide)) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos) || !player.mayBuild()) return InteractionResult.FAIL;
        final ItemStack held = player.getItemInHand(hand);
        final DyeColor dye = DyeColor.getColor(held);
        if (dye != null) {
            if (!level.isClientSide()) guide.setColor(OBPaintColors.palette(dye));
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (held.is(OBItems.WRENCH.get())) {
            if (!level.isClientSide()) {
                final OBGuideOrientation orientation = guide.orientation();
                final Direction face = hit.getDirection();
                final OBGuideOrientation next = face.getAxis() == orientation.y().getAxis()
                        ? orientation.quarterTurn(face == orientation.y()) : OBGuideOrientation.forSurface(face);
                level.setBlockAndUpdate(pos, state.setValue(ORIENTATION, next));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (builder && held.getItem() instanceof BlockItem) {
            if (player instanceof ServerPlayer server) guide.build(server, hit);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        final OBGuideControls.Button button = OBGuideControls.find(guide.orientation(), hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
        if (button == null) return InteractionResult.PASS;
        if (!level.isClientSide()) press(guide, button.name(), player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void press(final OBGuideBlockEntity guide, final String name, final Player player) {
        switch (name) {
            case "rotate_cw", "rotate_ccw" -> {
                final BlockState state = guide.getBlockState();
                guide.getLevel().setBlockAndUpdate(guide.getBlockPos(), state.setValue(ORIENTATION, guide.orientation().quarterTurn(name.equals("rotate_cw"))));
            }
            case "inc_mode", "dec_mode" -> {
                guide.setShape(OBGuideShape.byIndex(guide.shape().ordinal() + (name.equals("inc_mode") ? 1 : -1)));
                player.displayClientMessage(Component.translatable("guide.open_blocks.mode", guide.shape().label()), false);
                count(guide, player);
            }
            default -> resize(guide, name, player);
        }
    }

    private static void resize(final OBGuideBlockEntity guide, final String name, final Player player) {
        final Bounds b = guide.bounds();
        final int[] extents = {b.negX(), b.negY(), b.negZ(), b.posX(), b.posY(), b.posZ()};
        final boolean positive = name.charAt(name.length() - 5) == 'p';
        final int index = (positive ? 3 : 0) + name.charAt(name.length() - 1) - 'x';
        if (name.startsWith("copy")) extents[(index + 3) % 6] = extents[index];
        else extents[index] += name.startsWith("inc") ? 1 : -1;
        guide.setBounds(new Bounds(extents[0], extents[1], extents[2], extents[3], extents[4], extents[5]));
        final Bounds next = guide.bounds();
        player.displayClientMessage(Component.translatable("guide.open_blocks.bounds",
                -next.negX(), -next.negY(), -next.negZ(), next.posX(), next.posY(), next.posZ()), false);
        count(guide, player);
    }

    private static void count(final OBGuideBlockEntity guide, final Player player) {
        player.displayClientMessage(Component.translatable("guide.open_blocks.count", guide.points().size()), false);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack stack) {
        final var tag = stack.getTag();
        if (!level.isClientSide() && tag != null && (tag.contains("Mode") || tag.contains("PosX", Tag.TAG_ANY_NUMERIC) || tag.contains("Width", Tag.TAG_ANY_NUMERIC))
                && level.getBlockEntity(pos) instanceof OBGuideBlockEntity guide) guide.applyItem(tag);
    }

    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder loot) {
        final var drops = super.getDrops(state, loot);
        if (loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof OBGuideBlockEntity guide) {
            for (final ItemStack stack : drops) if (stack.is(asItem())) stack.getOrCreateTag().merge(guide.settings());
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final BlockGetter level, final BlockPos pos, final Player player) {
        final ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        if (level.getBlockEntity(pos) instanceof OBGuideBlockEntity guide) stack.getOrCreateTag().merge(guide.settings());
        return stack;
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.7;
        final double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        level.addParticle(builder ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, x, y, z, 0, 0, 0);
    }
}
