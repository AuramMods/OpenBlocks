package art.arcane.openblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OBLadderBlock extends OBShapeBlock {

    public OBLadderBlock(
            final BlockBehaviour.Properties properties,
            final VoxelShape shape,
            final VoxelShape collisionShape) {
        super(properties, shape, collisionShape);
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return hasNorthSupport(level, pos);
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
        if (!level.isClientSide && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    protected static boolean hasNorthSupport(final LevelReader level, final BlockPos pos) {
        final BlockPos supportPos = pos.north();
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.SOUTH);
    }
}
