package art.arcane.openblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OBShapeBlock extends Block {

    private final VoxelShape shape;
    private final VoxelShape collisionShape;

    public OBShapeBlock(final BlockBehaviour.Properties properties, final VoxelShape shape) {
        this(properties, shape, shape);
    }

    public OBShapeBlock(final BlockBehaviour.Properties properties, final VoxelShape shape, final VoxelShape collisionShape) {
        super(properties);
        this.shape = shape;
        this.collisionShape = collisionShape;
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return collisionShape;
    }

    @Override
    public VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return collisionShape;
    }

    @Override
    public VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return true;
    }
}
