package art.arcane.openblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OBPathBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.6D, 16.0D);

    public OBPathBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return true;
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final BlockPos belowPos = pos.below();
        return level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP);
    }

    @Override
    public VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return SHAPE;
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
