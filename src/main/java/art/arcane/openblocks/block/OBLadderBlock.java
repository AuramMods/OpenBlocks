package art.arcane.openblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public final class OBLadderBlock extends TrapDoorBlock {

    public OBLadderBlock(final BlockBehaviour.Properties properties) {
        super(properties, BlockSetType.OAK);
        // Existing saves from the earlier port have a vertical ladder along the north edge.
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(OPEN, true));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockState placement = super.getStateForPlacement(context);
        return placement.setValue(OPEN, placement.getValue(POWERED));
    }

    @Override
    public boolean isLadder(final BlockState state, final LevelReader level, final BlockPos pos, final LivingEntity entity) {
        return state.getValue(OPEN);
    }
}
