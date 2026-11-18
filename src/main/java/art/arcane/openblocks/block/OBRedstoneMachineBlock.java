package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBRedstoneMachineBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class OBRedstoneMachineBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final int WORK_EVENT = 3;

    public enum Kind {
        BREAKER, PLACER, DROPPER;

        public BlockEntityType<OBRedstoneMachineBlockEntity> type() {
            return switch (this) {
                case BREAKER -> OBBlockEntities.BLOCK_BREAKER.get();
                case PLACER -> OBBlockEntities.BLOCK_PLACER.get();
                case DROPPER -> OBBlockEntities.ITEM_DROPPER.get();
            };
        }
    }

    private final Kind kind;

    public OBRedstoneMachineBlock(final Properties properties, final Kind kind) {
        super(properties.strength(1.0F));
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, kind == Kind.DROPPER ? Direction.DOWN : Direction.UP)
                .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockPos pos = context.getClickedPos();
        final Player player = context.getPlayer();
        Direction towardBlock = context.getClickedFace().getOpposite();
        if (player != null) {
            towardBlock = player.getDirection();
            if (Math.abs(player.getX() - pos.getX()) < 2.0 && Math.abs(player.getZ() - pos.getZ()) < 2.0) {
                final double height = player.getEyeY() - pos.getY();
                if (height > 2.0) towardBlock = Direction.DOWN;
                else if (height < 0.0) towardBlock = Direction.UP;
            }
        }
        return defaultBlockState().setValue(FACING, kind == Kind.DROPPER ? towardBlock : towardBlock.getOpposite());
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return kind.type().create(pos, state);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighbor,
            final BlockPos neighborPos, final boolean moved) {
        if (level instanceof ServerLevel server) updatePower(server, pos, state);
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        updatePower(level, pos, state);
    }

    private void updatePower(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof OBRedstoneMachineBlockEntity machine)) return;
        final int signal = level.getBestNeighborSignal(pos);
        final boolean powered = signal > 0;
        final boolean changed = state.getValue(POWERED) != powered;
        if (changed) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            if (kind != Kind.DROPPER) {
                level.playSound(null, pos, powered ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                        SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.15F + 0.6F);
            }
        }
        if (!powered) return;
        if (kind != Kind.DROPPER) machine.requestWork(level, state.getValue(FACING));
        else if (changed) machine.eject(level, state.getValue(FACING), signal);
    }

    @Override
    public boolean triggerEvent(final BlockState state, final Level level, final BlockPos pos, final int id, final int parameter) {
        if (id != WORK_EVENT) return super.triggerEvent(state, level, pos, id, parameter);
        if (level instanceof ServerLevel server && kind != Kind.DROPPER
                && level.getBlockEntity(pos) instanceof OBRedstoneMachineBlockEntity machine) {
            machine.manipulate(server, state.getValue(FACING));
        }
        return false;
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (kind == Kind.BREAKER) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OBRedstoneMachineBlockEntity machine) player.openMenu(machine);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState replacement, final boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof OBRedstoneMachineBlockEntity machine) machine.dropInventory();
        super.onRemove(state, level, pos, replacement, moving);
    }
}
