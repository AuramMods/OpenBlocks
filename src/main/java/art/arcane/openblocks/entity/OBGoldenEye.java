package art.arcane.openblocks.entity;

import art.arcane.openblocks.registry.OBItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public final class OBGoldenEye extends Entity implements ItemSupplier {

    private static final int FLIGHT_TICKS = 60;
    private static final int RETRY_TICKS = 10;
    private static final double HORIZONTAL_RANGE = 6;
    private static final double CLOSE_RANGE = 16;
    private static final double DOWNWARD_RANGE = 4;
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(OBGoldenEye.class, EntityDataSerializers.ITEM_STACK);

    private Vec3 destination = Vec3.ZERO;
    private int age;
    private int dropRetry;
    private Vec3 lerpTarget = Vec3.ZERO;
    private int lerpSteps;

    public OBGoldenEye(final EntityType<? extends OBGoldenEye> type, final Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ITEM, ItemStack.EMPTY);
    }

    public void launch(final Player player, final BlockPos target, final ItemStack item) {
        entityData.set(ITEM, item.copyWithCount(1));
        final Vec3 eyes = player.getEyePosition();
        final double yaw = player.getYRot() * Mth.DEG_TO_RAD;
        setPos(eyes.x - 0.75 * Math.sin(yaw), eyes.y, eyes.z + 0.75 * Math.cos(yaw));

        final Vec3 direction = new Vec3(target.getX() - eyes.x, 0, target.getZ() - eyes.z);
        destination = direction.horizontalDistanceSqr() < CLOSE_RANGE * CLOSE_RANGE
                ? eyes.add(0, -DOWNWARD_RANGE, 0)
                : eyes.add(direction.normalize().scale(HORIZONTAL_RANGE));
    }

    @Override
    public ItemStack getItem() {
        return entityData.get(ITEM);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(final double distance) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            final Vec3 old = position();
            if (lerpSteps > 0) {
                final Vec3 next = old.lerp(lerpTarget, 1.0 / lerpSteps--);
                setPos(next.x, next.y, next.z);
            }
            final Vec3 movement = position().subtract(old);
            level().addParticle(ParticleTypes.PORTAL, getX() + random.nextGaussian() * 0.3 - 0.15,
                    getY() - 0.5 + random.nextGaussian() * 0.3 - 0.15, getZ() + random.nextGaussian() * 0.3 - 0.15,
                    movement.x, movement.y, movement.z);
            return;
        }

        if (getItem().isEmpty()) {
            discard();
        } else if (age < FLIGHT_TICKS) {
            ++age;
            final Vec3 remaining = destination.subtract(position());
            final Vec3 motion = remaining.lengthSqr() > 25 ? remaining.normalize().scale(2.5) : remaining.scale(0.5);
            move(MoverType.SELF, motion);
        } else if (dropRetry == 0) {
            final var drop = new ItemEntity(level(), getX(), getY(), getZ(), getItem().copy());
            if (((ServerLevel) level()).addFreshEntity(drop)) {
                entityData.set(ITEM, ItemStack.EMPTY);
                discard();
            } else {
                dropRetry = RETRY_TICKS;
            }
        } else {
            dropRetry--;
        }
    }

    @Override
    public void lerpTo(final double x, final double y, final double z, final float yaw, final float pitch, final int steps, final boolean teleport) {
        lerpTarget = new Vec3(x, y, z);
        lerpSteps = 2;
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        final ItemStack eye = ItemStack.of(tag.getCompound("SpawningItem"));
        entityData.set(ITEM, eye.is(OBItems.GOLDEN_EYE.get()) ? eye.copyWithCount(1) : ItemStack.EMPTY);
        // Legacy snapshots recorded the item and position only. Return that item after loading.
        final Vec3 savedTarget = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        destination = tag.contains("TargetX", Tag.TAG_ANY_NUMERIC) && tag.contains("TargetY", Tag.TAG_ANY_NUMERIC)
                && tag.contains("TargetZ", Tag.TAG_ANY_NUMERIC) && Double.isFinite(savedTarget.x)
                && Double.isFinite(savedTarget.y) && Double.isFinite(savedTarget.z) ? savedTarget : position();
        age = tag.contains("FlightAge", Tag.TAG_ANY_NUMERIC) ? Mth.clamp(tag.getInt("FlightAge"), 0, FLIGHT_TICKS) : FLIGHT_TICKS;
        dropRetry = Mth.clamp(tag.getInt("DropRetry"), 0, RETRY_TICKS);
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        if (!getItem().isEmpty()) tag.put("SpawningItem", getItem().save(new CompoundTag()));
        tag.putDouble("TargetX", destination.x);
        tag.putDouble("TargetY", destination.y);
        tag.putDouble("TargetZ", destination.z);
        tag.putInt("FlightAge", age);
        tag.putInt("DropRetry", dropRetry);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
