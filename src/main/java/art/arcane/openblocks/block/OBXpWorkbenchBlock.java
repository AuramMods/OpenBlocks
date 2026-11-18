package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBXpWorkbenchBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OBXpWorkbenchBlock extends BaseEntityBlock {

    private static final VoxelShape TABLE_SHAPE = box(0, 0, 0, 16, 12, 16);
    private final boolean enchanting;

    public static OBXpWorkbenchBlock enchantmentTable(final Properties properties) {
        return new OBXpWorkbenchBlock(properties, true);
    }

    private OBXpWorkbenchBlock(final Properties properties, final boolean enchanting) {
        super(properties.strength(1.0F).noOcclusion());
        this.enchanting = enchanting;
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return TABLE_SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBXpWorkbenchBlockEntity(enchanting, pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (enchanting && level.isClientSide) {
            return createTickerHelper(type, OBBlockEntities.AUTO_ENCHANTMENT_TABLE.get(), OBXpWorkbenchBlockEntity::clientTick);
        }
        return level.isClientSide ? null : createTickerHelper(type,
                (enchanting ? OBBlockEntities.AUTO_ENCHANTMENT_TABLE : OBBlockEntities.AUTO_ANVIL).get(), OBXpWorkbenchBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OBXpWorkbenchBlockEntity workbench) player.openMenu(workbench);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState replacement, final boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof OBXpWorkbenchBlockEntity workbench) {
            workbench.dropItems(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (!enchanting) return;
        for (final BlockPos offset : EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
            if (random.nextInt(16) == 0 && EnchantmentTableBlock.isValidBookShelf(level, pos, offset)) {
                level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                        offset.getX() + random.nextFloat() - 0.5, offset.getY() - random.nextFloat() - 1, offset.getZ() + random.nextFloat() - 0.5);
            }
        }
    }

    public static final class Anvil extends OBXpWorkbenchBlock {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
        private static final VoxelShape BASE = box(2, 0, 2, 14, 4, 14);
        private static final VoxelShape LONG_Z = Shapes.or(BASE, box(4, 4, 3, 12, 5, 13), box(6, 5, 4, 10, 10, 12), box(3, 10, 0, 13, 16, 16));
        private static final VoxelShape LONG_X = Shapes.or(BASE, box(3, 4, 4, 13, 5, 12), box(4, 5, 6, 12, 10, 10), box(0, 10, 3, 16, 16, 13));

        public Anvil(final Properties properties) {
            super(properties.sound(SoundType.ANVIL), false);
            registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
        }

        @Override
        protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS); }

        @Override
        public BlockState getStateForPlacement(final BlockPlaceContext context) {
            return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
        }

        @Override
        public BlockState rotate(final BlockState state, final Rotation rotation) {
            return state.setValue(AXIS, rotation.rotate(Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS))).getAxis());
        }

        @Override
        public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
            return state.getValue(AXIS) == Direction.Axis.X ? LONG_X : LONG_Z;
        }
    }
}
