package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBGoldenEggBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBGoldenEggBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);

    public OBGoldenEggBlock(final Properties properties) {
        super(properties);
    }

    @Override public RenderShape getRenderShape(final BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) { return new OBGoldenEggBlockEntity(pos, state); }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer, final ItemStack item) {
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof OBGoldenEggBlockEntity egg) {
            egg.setOwner(player.getGameProfile());
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return createTickerHelper(type, OBBlockEntities.GOLDEN_EGG.get(), OBGoldenEggBlockEntity::tick);
    }
}
