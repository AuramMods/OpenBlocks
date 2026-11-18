package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBVacuumHopperBlock extends BaseEntityBlock {

    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    private static final VoxelShape SELECTION = box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2);
    private static final VoxelShape COLLISION = box(0.16, 0.16, 0.16, 15.84, 15.84, 15.84);

    public OBVacuumHopperBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ENABLED, true));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENABLED);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SELECTION;
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return COLLISION;
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBVacuumHopperBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return createTickerHelper(type, OBBlockEntities.VACUUM_HOPPER.get(), level.isClientSide
                ? OBVacuumHopperBlockEntity::clientTick : OBVacuumHopperBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (player.isSecondaryUseActive() && hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()) {
            if (!level.isClientSide) level.setBlockAndUpdate(pos, state.cycle(ENABLED));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OBVacuumHopperBlockEntity hopper) player.openMenu(hopper);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OBVacuumHopperBlockEntity hopper) hopper.collectContact(entity);
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof OBVacuumHopperBlockEntity hopper) {
            hopper.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
