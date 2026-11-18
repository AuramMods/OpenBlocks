package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBBearTrapBlock;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBSounds;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class OBBearTrapBlockEntity extends BlockEntity {

    public static final int OPENING_TICKS = 15;
    private static final int LOAD_GRACE_TICKS = 20;
    private static final double MAX_CAPTURE_HEIGHT = 0.4;
    private static final double RELEASE_DISTANCE_SQR = 0.75 * 0.75;
    private final Vec3 anchor;
    @Nullable private UUID captive;
    private long openedAt = -1;
    private int output;
    private int loadGrace;

    public OBBearTrapBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.BEARTRAP.get(), pos, state);
        anchor = Vec3.atBottomCenterOf(pos).add(0, OBBearTrapBlock.BASE_HEIGHT, 0);
    }

    public boolean isShut() { return captive != null; }
    public int comparatorOutput() { return output; }

    public float closure(final float partialTick) {
        if (isShut()) return 1;
        if (level == null || openedAt < 0) return 0;
        return 1 - Mth.clamp((level.getGameTime() - openedAt + partialTick) / OPENING_TICKS, 0.0F, 1.0F);
    }

    public void tryCapture(final PathfinderMob mob) {
        if (!(level instanceof ServerLevel server) || isShut() || openedAt < 0 || server.getGameTime() - openedAt < OPENING_TICKS
                || !mob.isAlive() || mob.isPassenger() || !mob.blockPosition().equals(worldPosition)
                || mob.getY() - worldPosition.getY() >= MAX_CAPTURE_HEIGHT || server.hasNeighborSignal(worldPosition)) return;
        captive = mob.getUUID();
        loadGrace = 0;
        pin(mob);
        updateOutput(server, score(mob));
        server.playSound(null, worldPosition, OBSounds.BEARTRAP_CLOSE.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    public void release() {
        if (captive == null || !(level instanceof ServerLevel server)) return;
        captive = null;
        openedAt = server.getGameTime();
        loadGrace = 0;
        updateOutput(server, 0);
        server.playSound(null, worldPosition, OBSounds.BEARTRAP_OPEN.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBBearTrapBlockEntity trap) {
        if (!(level instanceof ServerLevel server)) return;
        if (trap.captive == null) {
            if (trap.openedAt < 0) {
                trap.openedAt = server.getGameTime();
                trap.sync(server);
            }
            return;
        }

        final var entity = server.getEntity(trap.captive);
        if (server.hasNeighborSignal(pos)) {
            trap.release();
        } else if (entity == null && trap.loadGrace > 0) {
            trap.loadGrace--;
        } else if (entity instanceof PathfinderMob mob && mob.isAlive() && !mob.isPassenger() && mob.distanceToSqr(trap.anchor) <= RELEASE_DISTANCE_SQR) {
            trap.loadGrace = 0;
            trap.pin(mob);
            final int score = score(mob);
            if (trap.output != score) trap.updateOutput(server, score);
        } else {
            trap.release();
        }
    }

    private static int score(final PathfinderMob mob) {
        return Mth.clamp(Mth.ceil(mob.getBoundingBox().getSize() / 16.0), 1, 15);
    }

    private void pin(final PathfinderMob mob) {
        mob.getNavigation().stop();
        if (mob.distanceToSqr(anchor) != 0 || mob.getDeltaMovement().lengthSqr() != 0) {
            mob.setPos(anchor.x, anchor.y, anchor.z);
            mob.setDeltaMovement(Vec3.ZERO);
            mob.hasImpulse = true;
        }
    }

    private void updateOutput(final ServerLevel server, final int signal) {
        output = signal;
        sync(server);
        server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private void sync(final ServerLevel server) {
        setChanged();
        server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        if (captive != null) tag.putUUID("captive", captive);
        tag.putLong("openedAt", openedAt);
        tag.putInt("output", output);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        captive = tag.hasUUID("captive") ? tag.getUUID("captive") : null;
        openedAt = tag.contains("openedAt", Tag.TAG_ANY_NUMERIC) ? tag.getLong("openedAt") : -1;
        output = captive == null ? 0 : Mth.clamp(tag.getInt("output"), 1, 15);
        loadGrace = captive == null ? 0 : LOAD_GRACE_TICKS;
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
