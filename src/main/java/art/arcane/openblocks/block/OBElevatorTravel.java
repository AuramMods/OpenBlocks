package art.arcane.openblocks.block;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.player.OBExperience;
import art.arcane.openblocks.registry.OBSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public final class OBElevatorTravel {

    private OBElevatorTravel() {}

    public static boolean use(final Player player, final boolean descend) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || !player.onGround()
                || player.isPassenger() || player.isSpectator() || player.getAbilities().flying || player.isSleeping()) {
            return false;
        }

        final BlockPos origin = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.01D, player.getZ());
        final OBElevatorRules rules = OBElevatorRules.configured();
        final var source = rules.identify(level.getBlockState(origin));
        if (source == null) return false;

        final Direction direction = descend ? Direction.DOWN : Direction.UP;
        final int distance = OBServerConfig.ELEVATOR_DISTANCE.get();
        final boolean ignoreBlockers = OBServerConfig.ELEVATOR_IGNORE_BLOCKERS.get();
        final boolean ignorePartial = OBServerConfig.ELEVATOR_IGNORE_PARTIAL_BLOCKS.get();
        final int maxBlockers = OBServerConfig.ELEVATOR_MAX_BLOCKERS.get();
        int blockers = 0;

        BlockPos pos = origin;
        for (int i = 0; i < distance; i++) {
            pos = pos.relative(direction);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) break;

            final var state = level.getBlockState(pos);
            if (state.isAir()) continue;
            final var landing = rules.identify(state);
            if (landing != null && landing.color() == source.color()) {
                final boolean center = OBServerConfig.ELEVATOR_CENTER.get();
                final double x = center ? pos.getX() + 0.5D : player.getX();
                final double y = pos.getY() + 1.0D;
                final double z = center ? pos.getZ() + 0.5D : player.getZ();
                if (canLand(level, player, x, y, z)) {
                    return move(level, player, pos, landing, x, y, z, i + 1);
                }
            }

            if (ignoreBlockers) continue;
            final var rule = rules.blockers().get(state.getBlock());
            if (rule == OBElevatorRules.Action.ABORT) break;
            if (rule == OBElevatorRules.Action.IGNORE || (rule == null && ignorePartial
                    && !Block.isShapeFullBlock(state.getCollisionShape(level, pos)))) continue;
            if (++blockers > maxBlockers) break;
        }
        return false;
    }

    private static boolean canLand(final ServerLevel level, final Player player, final double x, final double y, final double z) {
        final AABB bounds = player.getDimensions(Pose.STANDING).makeBoundingBox(x, y, z);
        if (bounds.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(bounds.minX, bounds.minZ)
                || !level.getWorldBorder().isWithinBounds(bounds.maxX, bounds.maxZ)
                || !level.noCollision(player, bounds)
                || level.containsAnyLiquid(bounds)) return false;

        if (OBServerConfig.ELEVATOR_ALLOW_NON_COLLIDING_BLOCKS.get()) return true;
        return BlockPos.betweenClosedStream(bounds.deflate(1.0E-6D)).allMatch(level::isEmptyBlock);
    }

    private static boolean move(final ServerLevel level, final Player player, final BlockPos pos,
            final OBElevatorRules.Landing landing, final double x, final double y, final double z, final int distance) {
        if (!player.isCreative()) {
            final int cost = (int) Math.ceil(OBServerConfig.ELEVATOR_XP_PER_BLOCK.get() * distance);
            if (cost > 0) {
                if (OBExperience.points(player) < cost) return false;
                player.giveExperiencePoints(-cost);
            }
        }

        final float yaw = landing.yaw() != null ? landing.yaw() : player.getYRot();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.teleport(x, y, z, yaw, player.getXRot());
        } else {
            player.teleportTo(x, y, z);
            player.setYRot(yaw);
        }
        player.setOnGround(false);
        player.resetFallDistance();
        level.playSound(null, pos, OBSounds.ELEVATOR_ACTIVATE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
}
