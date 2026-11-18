package art.arcane.openblocks.block;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.world.OBSpongeDrain;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class OBSpongeBlock extends Block {

    private static final int TICK_RATE = 20 * 5;
    private static final int TICK_JITTER = 5;
    private static final ThreadLocal<Set<GlobalPos>> ACTIVE_DRAINS = ThreadLocal.withInitial(HashSet::new);

    public OBSpongeBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos,
            final BlockState oldState, final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) drainAndSchedule(server, pos, level.random);
    }

    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
            final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
        if (level instanceof ServerLevel server) clearLiquids(server, pos);
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        drainAndSchedule(level, pos, random);
    }

    private void drainAndSchedule(final ServerLevel level, final BlockPos pos, final RandomSource random) {
        clearLiquids(level, pos);
        if (level.getBlockState(pos).is(this)) level.scheduleTick(pos, this, TICK_RATE + random.nextInt(TICK_JITTER));
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos,
            final BlockState newState, final boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this) && !OBServerConfig.SPONGE_BLOCK_UPDATE.get()) {
            wakeUpNearbyLiquids(server, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void clearLiquids(final ServerLevel level, final BlockPos origin) {
        final var position = GlobalPos.of(level.dimension(), origin.immutable());
        final var active = ACTIVE_DRAINS.get();
        // Optional block updates can call back into a sponge that is already absorbing.
        if (!active.add(position)) return;
        try {
            final var result = OBSpongeDrain.drain(level, origin, OBServerConfig.SPONGE_RANGE.get(),
                    OBServerConfig.SPONGE_BLOCK_UPDATE.get(), null);
            if (!result.lava() || !level.getBlockState(origin).is(this)) return;

            level.sendParticles(ParticleTypes.LARGE_SMOKE, origin.getX() + 0.5D, origin.getY() + 1.0D,
                    origin.getZ() + 0.5D, 20, 0.25D, 0.35D, 0.25D, 0.0D);
            final BlockState fire = BaseFireBlock.getState(level, origin);
            if (fire.canSurvive(level, origin)) {
                level.setBlockAndUpdate(origin, fire);
            } else {
                level.removeBlock(origin, false);
            }
        } finally {
            active.remove(position);
            if (active.isEmpty()) ACTIVE_DRAINS.remove();
        }
    }

    private static void wakeUpNearbyLiquids(final ServerLevel level, final BlockPos origin) {
        final int radius = OBServerConfig.SPONGE_RANGE.get() + 1;
        for (final BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) continue;
            final var fluid = level.getFluidState(pos);
            if (!fluid.isEmpty()) level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
        }
    }
}
