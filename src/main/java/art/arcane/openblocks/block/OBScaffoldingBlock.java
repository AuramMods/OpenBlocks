package art.arcane.openblocks.block;

import art.arcane.openblocks.config.OBServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class OBScaffoldingBlock extends Block {

    public OBScaffoldingBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        final int rate = OBServerConfig.SCAFFOLDING_DESPAWN_RATE.get();
        if (rate <= 1 || random.nextInt(rate) == 0) level.destroyBlock(pos, true);
    }

    @Override
    public boolean skipRendering(final BlockState state, final BlockState adjacent, final Direction side) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }
}
