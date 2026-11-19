package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBProjectorBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public final class OBProjectorBlock extends BaseEntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape BASE = box(0, 0, 0, 16, 8, 16);

    public OBProjectorBlock(final Properties properties) {
        super(properties.lightLevel(state -> state.getValue(ACTIVE) ? 10 : 0));
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return BASE;
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) { return RenderShape.MODEL; }

    @Override
    public int getLightEmission(final BlockState state, final BlockGetter level, final BlockPos pos) {
        if (!state.getValue(ACTIVE)) return 0;
        if (!OBServerConfig.SPEC.isLoaded()) return 10;
        return OBServerConfig.PROJECTOR_LIGHT_ENABLED.get() ? OBServerConfig.PROJECTOR_LIGHT_LEVEL.get() : 0;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBProjectorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return createTickerHelper(type, OBBlockEntities.PROJECTOR.get(),
                level.isClientSide() ? OBProjectorBlockEntity::clientTick : OBProjectorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos, final Player player,
            final InteractionHand hand, final BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof OBProjectorBlockEntity projector) {
            NetworkHooks.openScreen(server, projector, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState replacement, final boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof OBProjectorBlockEntity projector) projector.dropMap();
        super.onRemove(state, level, pos, replacement, moving);
    }
}
