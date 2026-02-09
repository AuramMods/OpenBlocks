package art.arcane.openblocks.block;

import art.arcane.openblocks.registry.OBSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class OBElevatorBlock extends Block {

    private static final int MAX_TRAVEL_DISTANCE = 20;
    private static final int MAX_BLOCKERS = 4;

    public OBElevatorBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public InteractionResult use(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final net.minecraft.world.phys.BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player.isPassenger()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        final Direction direction = player.isShiftKeyDown() ? Direction.DOWN : Direction.UP;
        final SearchResult destination = findDestination(level, pos, direction, player);
        if (destination == null) return InteractionResult.PASS;

        teleportPlayer(player, destination);
        level.playSound(
                null,
                destination.pos,
                OBSounds.ELEVATOR_ACTIVATE.get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        return InteractionResult.CONSUME;
    }

    @Nullable
    private SearchResult findDestination(
            final Level level,
            final BlockPos origin,
            final Direction direction,
            final Player player) {
        int blockers = 0;
        BlockPos searchPos = origin;

        for (int i = 0; i < MAX_TRAVEL_DISTANCE; i++) {
            searchPos = searchPos.relative(direction);
            if (!level.isInWorldBounds(searchPos)) break;

            final BlockState state = level.getBlockState(searchPos);
            if (state.isAir()) continue;

            if (state.getBlock() instanceof OBElevatorBlock && canTeleportTo(level, searchPos)) {
                return new SearchResult(searchPos, state);
            }

            if (++blockers > MAX_BLOCKERS) break;
        }

        return null;
    }

    private boolean canTeleportTo(final Level level, final BlockPos elevatorPos) {
        final BlockPos feetPos = elevatorPos.above();
        final BlockPos headPos = feetPos.above();

        if (!level.isInWorldBounds(feetPos) || !level.isInWorldBounds(headPos)) return false;
        if (!isPassableForTeleport(level, feetPos)) return false;
        if (!isPassableForTeleport(level, headPos)) return false;

        return true;
    }

    private static boolean isPassableForTeleport(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    private void teleportPlayer(final Player player, final SearchResult destination) {
        final double targetX = destination.pos.getX() + 0.5D;
        final double targetY = destination.pos.getY() + 1.1D;
        final double targetZ = destination.pos.getZ() + 0.5D;
        final Float targetYaw = getTargetYaw(destination.state);
        final float currentPitch = player.getXRot();

        if (player instanceof ServerPlayer serverPlayer) {
            final float yaw = targetYaw != null ? targetYaw : serverPlayer.getYRot();
            serverPlayer.connection.teleport(targetX, targetY, targetZ, yaw, currentPitch);
        } else {
            player.teleportTo(targetX, targetY, targetZ);
            if (targetYaw != null) player.setYRot(targetYaw);
        }

        player.fallDistance = 0.0F;
    }

    @Nullable
    protected Float getTargetYaw(final BlockState state) {
        return null;
    }

    private record SearchResult(BlockPos pos, BlockState state) {}
}
