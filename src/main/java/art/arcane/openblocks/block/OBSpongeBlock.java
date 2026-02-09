package art.arcane.openblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class OBSpongeBlock extends Block {

    private static final int RANGE = 3;
    private static final int TICK_RATE = 20 * 5;
    private static final int TICK_JITTER = 5;

    public OBSpongeBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final BlockState oldState,
            final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            clearLiquidsAndBurnIfNeeded((ServerLevel) level, pos);
            level.scheduleTick(pos, this, TICK_RATE + level.random.nextInt(TICK_JITTER));
        }
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
        if (!level.isClientSide) clearLiquidsAndBurnIfNeeded((ServerLevel) level, pos);
    }

    @Override
    public void tick(
            final BlockState state,
            final ServerLevel level,
            final BlockPos pos,
            final RandomSource random) {
        clearLiquidsAndBurnIfNeeded(level, pos);
        if (level.getBlockState(pos).is(this)) {
            level.scheduleTick(pos, this, TICK_RATE + random.nextInt(TICK_JITTER));
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
            wakeUpBorderLiquids((ServerLevel) level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void clearLiquidsAndBurnIfNeeded(final ServerLevel level, final BlockPos origin) {
        boolean touchedLava = false;
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dy = -RANGE; dy <= RANGE; dy++) {
                for (int dz = -RANGE; dz <= RANGE; dz++) {
                    final BlockPos workPos = origin.offset(dx, dy, dz);
                    if (!level.hasChunkAt(workPos)) continue;

                    final FluidState fluidState = level.getBlockState(workPos).getFluidState();
                    if (fluidState.isEmpty()) continue;

                    touchedLava |= fluidState.is(FluidTags.LAVA);
                    level.setBlock(workPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }

        if (touchedLava && level.getBlockState(origin).is(this)) {
            level.levelEvent(2004, origin, 0);
            final BlockState fireState = BaseFireBlock.getState(level, origin);
            if (fireState.canSurvive(level, origin)) {
                level.setBlockAndUpdate(origin, fireState);
            } else {
                level.removeBlock(origin, false);
            }
        }
    }

    private void wakeUpBorderLiquids(final ServerLevel level, final BlockPos origin) {
        final int borderRange = RANGE + 1;
        for (int dx = -borderRange; dx <= borderRange; dx++) {
            for (int dy = -borderRange; dy <= borderRange; dy++) {
                for (int dz = -borderRange; dz <= borderRange; dz++) {
                    final BlockPos workPos = origin.offset(dx, dy, dz);
                    if (!level.hasChunkAt(workPos)) continue;

                    final FluidState fluidState = level.getBlockState(workPos).getFluidState();
                    if (fluidState.isEmpty()) continue;

                    level.scheduleTick(workPos, fluidState.getType(), fluidState.getType().getTickDelay(level));
                }
            }
        }
    }
}
