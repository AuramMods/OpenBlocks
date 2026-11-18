package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.entity.OBItemProjectile;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBSounds;
import art.arcane.openblocks.world.OBMachineItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OBCannonBlockEntity extends BlockEntity {

    private static final int RECOIL_EVENT = 1;
    private static final int SHOT_INTERVAL = 20;
    private static final int MAX_STACK_PER_SHOT = 64;
    private static final double DEFAULT_PITCH = 45;
    private static final double DEFAULT_SPEED = 1.4;
    private static final double MAX_SPEED = 3.9;

    private double targetYaw;
    private double targetPitch = DEFAULT_PITCH;
    private double targetSpeed = DEFAULT_SPEED;
    private double yaw;
    private double pitch = DEFAULT_PITCH;
    private double speed = DEFAULT_SPEED;
    private double previousYaw;
    private double previousPitch = DEFAULT_PITCH;
    private int recoilTicks = 12;
    private ItemStack pending = ItemStack.EMPTY;

    public OBCannonBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.CANNON.get(), pos, state);
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final OBCannonBlockEntity cannon) {
        cannon.updateAim();
        if (level instanceof ServerLevel server) {
            if (level.getGameTime() % SHOT_INTERVAL == 0 && level.hasNeighborSignal(pos)) cannon.fireNextStack(server);
        } else if (cannon.recoilTicks < 12) {
            cannon.recoilTicks++;
        }
    }

    private void updateAim() {
        previousYaw = yaw;
        previousPitch = pitch;
        yaw = Mth.wrapDegrees(yaw + Mth.clamp(Mth.wrapDegrees(targetYaw - yaw), -3.0D, 3.0D));
        pitch = Mth.lerp(0.05, pitch, targetPitch);
        speed = Mth.lerp(0.05, speed, targetSpeed);
    }

    private void fireNextStack(final ServerLevel server) {
        if (!pending.isEmpty()) {
            final ItemStack retry = pending;
            pending = ItemStack.EMPTY;
            if (!fire(server, retry)) keepPending(server, retry);
            setChanged();
            return;
        }

        for (final Direction face : Direction.values()) {
            final var inventory = OBMachineItems.adjacent(server, worldPosition, face);
            if (inventory == null) continue;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                final ItemStack stack = inventory.extractItem(slot, MAX_STACK_PER_SHOT, false);
                if (stack.isEmpty()) continue;
                if (!fire(server, stack)) keepPending(server, inventory.insertItem(slot, stack, false));
                return;
            }
        }
    }

    private boolean fire(final ServerLevel server, final ItemStack stack) {
        final Vec3 origin = origin();
        final Vec3 velocity = motion();
        if (!server.addFreshEntity(new OBItemProjectile(server, origin, velocity, stack.copy()))) return false;

        server.blockEvent(worldPosition, getBlockState().getBlock(), RECOIL_EVENT, 0);
        server.playSound(null, worldPosition, OBSounds.CANNON_ACTIVATE.get(), SoundSource.BLOCKS, 0.2F, 1.0F);
        final Vec3 outlet = origin.add(velocity.normalize().scale(0.5));
        server.sendParticles(ParticleTypes.LARGE_SMOKE, outlet.x, outlet.y, outlet.z, 4, .2, .05, .2, 0.01);
        server.sendParticles(ParticleTypes.SMOKE, outlet.x, outlet.y, outlet.z, 16, .2, .05, .2, 0.01);
        return true;
    }

    private void keepPending(final ServerLevel server, final ItemStack stack) {
        if (stack.isEmpty()) return;
        if (isRemoved()) {
            Block.popResource(server, worldPosition, stack);
        } else {
            pending = stack;
            setChanged();
        }
    }

    public void dropPending() {
        final ItemStack stack = pending;
        pending = ItemStack.EMPTY;
        if (level != null) Block.popResource(level, worldPosition, stack);
    }

    @Override
    public boolean triggerEvent(final int id, final int param) {
        if (id != RECOIL_EVENT) return super.triggerEvent(id, param);
        recoilTicks = 0;
        return true;
    }

    public int recoilTicks() { return recoilTicks; }
    public double yaw(final float partialTick) { return previousYaw + Mth.wrapDegrees(yaw - previousYaw) * partialTick; }
    public double pitch(final float partialTick) { return Mth.lerp(partialTick, previousPitch, pitch); }
    public Vec3 origin() { return Vec3.atCenterOf(worldPosition); }

    public Vec3 motion() {
        final double heading = Math.toRadians(yaw);
        final double rise = Math.toRadians(pitch);
        final double horizontalSpeed = Math.cos(rise) * speed;
        return new Vec3(-Math.sin(heading) * horizontalSpeed, Math.sin(rise) * speed, Math.cos(heading) * horizontalSpeed);
    }

    public boolean aimAt(final BlockPos target) {
        final Vec3 offset = Vec3.atBottomCenterOf(target.above()).subtract(origin());
        final double lob = Math.sqrt(offset.horizontalDistanceSqr()) + Math.max(0.0, offset.y * 4.0) + 5.0;
        final int flightTicks = (int) Math.round(Mth.clamp(lob, 20.0, 75.0));
        // Gravity is applied before each movement, so the vertical loss sums to g * n * (n + 1) / 2.
        final Vec3 velocity = offset.scale(1.0 / flightTicks).add(0, OBItemProjectile.GRAVITY * (flightTicks + 1) / 2.0, 0);
        final double power = velocity.length();
        if (power > MAX_SPEED) return false;

        previousYaw = yaw = targetYaw = Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
        previousPitch = pitch = targetPitch = Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance()));
        speed = targetSpeed = power;
        syncAim();
        return true;
    }

    public void setYaw(final double value) {
        if (!Double.isFinite(value)) return;
        targetYaw = Mth.wrapDegrees(value);
        syncAim();
    }

    public void setPitch(final double value) {
        if (!Double.isFinite(value)) return;
        targetPitch = Mth.clamp(value, -90, 90);
        syncAim();
    }

    public void setSpeed(final double value) {
        if (!Double.isFinite(value)) return;
        targetSpeed = Mth.clamp(value, 0, MAX_SPEED);
        syncAim();
    }

    private void syncAim() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(32);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        writeAim(tag);
        if (!pending.isEmpty()) tag.put("pending", pending.save(new CompoundTag()));
    }

    private void writeAim(final CompoundTag tag) {
        tag.putDouble("targetYaw", targetYaw);
        tag.putDouble("targetPitch", targetPitch);
        tag.putDouble("targetSpeed", targetSpeed);
        tag.putDouble("yaw", yaw);
        tag.putDouble("pitch", pitch);
        tag.putDouble("speed", speed);
    }

    private static double readNumber(final CompoundTag tag, final String key, final double fallback) {
        final double value = tag.getDouble(key);
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) && Double.isFinite(value) ? value : fallback;
    }

    private void readAim(final CompoundTag tag) {
        targetYaw = Mth.wrapDegrees(readNumber(tag, "targetYaw", 0));
        targetPitch = Mth.clamp(readNumber(tag, "targetPitch", DEFAULT_PITCH), -90, 90);
        targetSpeed = Mth.clamp(readNumber(tag, "targetSpeed", DEFAULT_SPEED), 0, MAX_SPEED);
        previousYaw = yaw = Mth.wrapDegrees(readNumber(tag, "yaw", targetYaw));
        previousPitch = pitch = Mth.clamp(readNumber(tag, "pitch", targetPitch), -90, 90);
        speed = Mth.clamp(readNumber(tag, "speed", targetSpeed), 0, MAX_SPEED);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        readAim(tag);
        pending = ItemStack.of(tag.getCompound("pending"));
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = new CompoundTag();
        writeAim(tag);
        return tag;
    }

    @Override public void handleUpdateTag(final CompoundTag tag) { readAim(tag); }
}
