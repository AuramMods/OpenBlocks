package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBImaginaryBlockEntity;
import art.arcane.openblocks.item.OBImaginaryItem;
import art.arcane.openblocks.registry.OBSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.ForgeSoundType;

public final class OBImaginaryBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final SoundType SOUND = new ForgeSoundType(0.5F, 1.0F,
            OBSounds.CRAYON_PLACE, OBSounds.CRAYON_PLACE, OBSounds.CRAYON_PLACE, OBSounds.CRAYON_PLACE, OBSounds.CRAYON_PLACE);

    public OBImaginaryBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public enum Shape {
        BLOCK(Shapes.block()),
        PANEL(box(0, 14.4, 0, 16, 16, 16)),
        HALF_PANEL(box(0, 6.4, 0, 16, 8, 16)),
        STAIRS(
                Shapes.or(box(0, 6.4, 0, 16, 8, 8), box(0, 14.4, 8, 16, 16, 16)),
                Shapes.or(box(8, 6.4, 0, 16, 8, 16), box(0, 14.4, 0, 8, 16, 16)),
                Shapes.or(box(0, 6.4, 8, 16, 8, 16), box(0, 14.4, 0, 16, 16, 8)),
                Shapes.or(box(0, 6.4, 0, 8, 8, 16), box(8, 14.4, 0, 16, 16, 16)));

        private static final Shape[] VALUES = values();
        private final VoxelShape[] shapes;

        Shape(final VoxelShape... shapes) {
            this.shapes = shapes;
        }

        public VoxelShape bounds(final Direction facing) {
            return shapes.length == 1 ? shapes[0] : shapes[(facing.get2DDataValue() + 2) % 4];
        }

        public static Shape fromId(final int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : BLOCK;
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public BlockState mirror(final BlockState state, final Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.INVISIBLE; }
    @Override public SoundType getSoundType(final BlockState state) { return SOUND; }
    @Override public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBImaginaryBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        final var player = player(context);
        if (player != null && level.getBlockEntity(pos) instanceof OBImaginaryBlockEntity block && block.selectableBy(player)) {
            return block.shape().bounds(state.getValue(FACING));
        }
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        final var player = player(context);
        if (player != null && level.getBlockEntity(pos) instanceof OBImaginaryBlockEntity block && block.solidFor(player)) {
            return block.shape().bounds(state.getValue(FACING));
        }
        return Shapes.empty();
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final BlockGetter level,
            final BlockPos pos, final Player player) {
        return level.getBlockEntity(pos) instanceof OBImaginaryBlockEntity block
                ? OBImaginaryItem.create(block.color(), OBImaginaryItem.Mode.of(block.shape(), block.inverted()), OBImaginaryItem.defaultUses())
                : super.getCloneItemStack(state, target, level, pos, player);
    }

    @Nullable
    private static Player player(final CollisionContext context) {
        return context instanceof EntityCollisionContext entities && entities.getEntity() instanceof Player player ? player : null;
    }
}
