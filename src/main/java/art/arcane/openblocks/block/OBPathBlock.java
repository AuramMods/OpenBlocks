package art.arcane.openblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OBPathBlock extends OBShapeBlock {

    public OBPathBlock(
            final BlockBehaviour.Properties properties,
            final VoxelShape shape,
            final VoxelShape collisionShape) {
        super(properties, shape, collisionShape);
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final BlockPos belowPos = pos.below();
        return level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP);
    }

    @Override
    public void neighborChanged(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Block neighborBlock,
            final BlockPos neighborPos,
            final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && neighborPos.equals(pos.below()) && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }
}
