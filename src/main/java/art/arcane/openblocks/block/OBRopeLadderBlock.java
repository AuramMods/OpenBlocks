package art.arcane.openblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OBRopeLadderBlock extends OBLadderBlock {

    public OBRopeLadderBlock(
            final BlockBehaviour.Properties properties,
            final VoxelShape shape,
            final VoxelShape collisionShape) {
        super(properties, shape, collisionShape);
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return hasNorthSupport(level, pos) || level.getBlockState(pos.above()).is(this);
    }

    @Override
    public void setPlacedBy(
            final Level level,
            final BlockPos pos,
            final BlockState state,
            final LivingEntity placer,
            final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        if (!(placer instanceof Player player) || player.isCreative()) return;

        BlockPos placePos = pos.below();
        while (placePos.getY() > level.getMinBuildHeight() && stack.getCount() > 1) {
            if (!level.isEmptyBlock(placePos)) return;
            if (!canSurvive(state, level, placePos)) return;

            level.setBlock(placePos, state, Block.UPDATE_ALL);
            stack.shrink(1);
            placePos = placePos.below();
        }
    }

    @Override
    public void onRemove(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final BlockState newState,
            final boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            final BlockPos belowPos = pos.below();
            if (level.getBlockState(belowPos).is(this)) {
                level.destroyBlock(belowPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
