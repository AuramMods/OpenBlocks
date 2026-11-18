package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBSkyBlockEntity;
import art.arcane.openblocks.item.OBSkyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.HitResult;

public final class OBSkyBlock extends Block implements EntityBlock {

    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public OBSkyBlock(final Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(INVERTED, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INVERTED, POWERED);
    }

    public static boolean isActive(final BlockState state) {
        return state.getValue(INVERTED) ^ state.getValue(POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBSkyBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return isActive(state) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState previous,
            final boolean moved) {
        if (!previous.is(this)) checkPower(state, level, pos);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighbor,
            final BlockPos neighborPos, final boolean moved) {
        checkPower(state, level, pos);
    }

    private void checkPower(final BlockState state, final Level level, final BlockPos pos) {
        if (!level.isClientSide && level.hasNeighborSignal(pos) != state.getValue(POWERED)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        final boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) level.setBlockAndUpdate(pos, state.setValue(POWERED, powered));
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final BlockGetter level,
            final BlockPos pos, final Player player) {
        return OBSkyItem.create(state.getValue(INVERTED));
    }
}
