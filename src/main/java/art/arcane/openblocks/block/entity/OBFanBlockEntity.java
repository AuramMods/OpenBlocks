package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.integration.OBWind;
import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OBFanBlockEntity extends BlockEntity {

    private static final double CONE_RADIUS_COS = Math.cos(0.6);
    private static final float BLADE_SPEED = 45.0F;
    private static final float FULL_WIND = 0.30F;
    private static final int WIND_REFRESH_TICKS = 40;
    private float yaw;
    private int power;
    private float bladeRotation;
    private int windPower;
    private float windYaw;
    private long nextWindRefresh;

    public OBFanBlockEntity(final BlockPos pos, final BlockState state) { super(OBBlockEntities.FAN.get(), pos, state); }

    public float yaw() { return yaw; }
    public int power() { return power; }

    public void setYaw(final float angle) {
        if (!Float.isFinite(angle)) return;
        final float next = Mth.wrapDegrees(angle);
        if (next != yaw) {
            yaw = next;
            sync();
        }
    }

    public float bladeRotation(final float partialTick) {
        return bladeRotation + BLADE_SPEED * power / 15.0F * partialTick;
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final OBFanBlockEntity fan) {
        if (level.isClientSide) {
            fan.bladeRotation = fan.bladeRotation(1.0F) % 360.0F;
            return;
        }
        final int signal = OBServerConfig.FAN_REDSTONE.get() ? level.getBestNeighborSignal(pos) : 15;
        if (fan.power != signal) {
            fan.power = signal;
            fan.sync();
        }
        final double force = OBServerConfig.FAN_FORCE.get() * signal / 15.0;
        final double range = OBServerConfig.FAN_RANGE.get();
        if (force > 0 && range > 0) {
            final Vec3 forward = fan.forward();
            if (level instanceof ServerLevel server) fan.publishWind(server, pos, forward, signal);
            fan.pushEntities(level, pos, forward, range, force);
        } else {
            fan.clearWind();
        }
    }

    private Vec3 forward() {
        final double heading = Math.toRadians(yaw);
        return new Vec3(Math.sin(heading), 0, -Math.cos(heading));
    }

    private void publishWind(final ServerLevel level, final BlockPos pos, final Vec3 forward, final int signal) {
        long now = level.getGameTime();
        if (windPower == signal && windYaw == yaw && now < nextWindRefresh) return;
        OBWind.setFan(level, pos, forward, FULL_WIND * signal / 15.0F);
        windPower = signal;
        windYaw = yaw;
        nextWindRefresh = now + WIND_REFRESH_TICKS;
    }

    private void clearWind() {
        if (windPower != 0 && level instanceof ServerLevel server) {
            OBWind.stopFan(server, worldPosition);
            windPower = 0;
        }
    }

    @Override
    public void setRemoved() {
        clearWind();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        clearWind();
        super.onChunkUnloaded();
    }

    private void pushEntities(final Level level, final BlockPos pos, final Vec3 forward, final double range,
                              final double maxForce) {
        final Vec3 apex = Vec3.atCenterOf(pos).subtract(forward.scale(1.1));
        final AABB bounds = new AABB(apex, apex).inflate(range);
        for (final Entity entity : level.getEntitiesOfClass(Entity.class, bounds, OBFanBlockEntity::canPush)) {
            final Vec3 offset = entity.position().subtract(apex);
            final double distance = offset.length();
            if (distance >= range || offset.dot(forward) <= distance * CONE_RADIUS_COS) continue;
            final double scaledForce = maxForce * (1.0 - distance / range) / distance;
            entity.push(offset.x * scaledForce, 0, offset.z * scaledForce);
            if (entity instanceof ServerPlayer) entity.hurtMarked = true;
        }
    }

    private static boolean canPush(final Entity entity) {
        return entity.isAlive() && !entity.isSpectator() && !(entity instanceof Player player && player.getAbilities().instabuild);
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("yaw", yaw);
        tag.putInt("power", power);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        final float angle = tag.getFloat("yaw");
        yaw = Float.isFinite(angle) ? Mth.wrapDegrees(angle) : 0;
        power = Mth.clamp(tag.getInt("power"), 0, 15);
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
