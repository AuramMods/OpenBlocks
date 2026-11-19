package art.arcane.openblocks.entity;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBMagnet extends Entity {

    private static final EntityDataAccessor<Integer> HOLDER = SynchedEntityData.defineId(OBMagnet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFT = SynchedEntityData.defineId(OBMagnet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DETECTS = SynchedEntityData.defineId(OBMagnet.class, EntityDataSerializers.BOOLEAN);
    private static final Map<Player, OBMagnet> ON_SERVER = new IdentityHashMap<>();
    private static final Map<Player, OBMagnet> ON_CLIENT = new IdentityHashMap<>();
    public static final double ARM_REACH = 2;
    public static final float SIZE = 0.5F;
    private static final double MIN_CABLE = 0.25;
    private static final double MAX_CABLE = 10;

    @Nullable private Player holder;
    @Nullable private UUID owner;
    @Nullable private GameProfile machineOwner;
    @Nullable private Supplier<Vec3> remoteTarget;
    @Nullable private Vec3 lastSafePosition;
    private float lastSafeYaw;
    private double cable = MIN_CABLE;
    private boolean lowering;
    private long nextToggle;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private int lerpSteps;

    public OBMagnet(final EntityType<? extends OBMagnet> type, final Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(HOLDER, -1);
        entityData.define(LIFT, 0);
        entityData.define(DETECTS, false);
    }

    public static boolean wearsBackpack(final LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(OBItems.CRANE_BACKPACK.get());
    }

    @Nullable
    public static OBMagnet forPlayer(final Player player) {
        final OBMagnet magnet = (player.level().isClientSide() ? ON_CLIENT : ON_SERVER).get(player);
        return magnet != null && !magnet.isRemoved() && magnet.level() == player.level() ? magnet : null;
    }

    @Nullable
    public static OBMagnet ensureFor(final ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !wearsBackpack(player)) return null;
        final OBMagnet existing = forPlayer(player);
        if (existing != null) return existing;

        final OBMagnet magnet = OBEntities.PLAYER_MAGNET.get().create(player.serverLevel());
        if (magnet == null) return null;
        magnet.owner = player.getUUID();
        magnet.entityData.set(HOLDER, player.getId());
        final Vec3 target = magnet.target(player);
        magnet.setPos(target.x, target.y, target.z);
        if (!player.serverLevel().addFreshEntity(magnet)) return null;
        magnet.bind(player);
        return magnet;
    }

    @Nullable
    public static OBMagnet deployRemote(final ServerLevel level, final GameProfile owner, final Vec3 initial, final Supplier<Vec3> target) {
        final OBMagnet magnet = OBEntities.MAGNET.get().create(level);
        if (magnet == null) return null;
        magnet.machineOwner = owner;
        magnet.remoteTarget = target;
        magnet.setPos(initial);
        if (!level.addFreshEntity(magnet)) return null;
        magnet.playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
        return magnet;
    }

    @Nullable
    GameProfile machineOwner() {
        return machineOwner;
    }

    private void bind(final Player player) {
        holder = player;
        (level().isClientSide() ? ON_CLIENT : ON_SERVER).put(player, this);
    }

    @Nullable
    public Player holder() {
        return holder;
    }

    public int liftState() {
        return entityData.get(LIFT);
    }

    public int magnetState() {
        return isVehicle() ? 2 : detectsEntity() ? 1 : 0;
    }

    public boolean detectsEntity() {
        return entityData.get(DETECTS);
    }

    public double cableLength() {
        return cable;
    }

    public void startWinch(final boolean sneaking) {
        lowering = OBServerConfig.CRANE_SHIFT_DIRECTION.get() ? sneaking : !lowering;
    }

    public void toggleFromControl(final ServerPlayer player) {
        final long time = level().getGameTime();
        if (time >= nextToggle) {
            nextToggle = time + 6;
            toggle(player);
        }
    }

    public boolean toggle(final ServerPlayer actor) {
        if (actor.level() != level()) return false;
        final Entity load = getFirstPassenger();
        if (load instanceof OBMountedBlock cargo) return cargo.release(actor);
        if (load != null) {
            load.stopRiding();
            return !hasPassenger(load);
        }
        if (OBServerConfig.MAGNET_PICK_ENTITIES.get()) {
            final Entity target = findEntity();
            if (target != null) {
                if (!target.startRiding(this, true)) return false;
                alignToCargo(target);
                return true;
            }
        }
        return OBServerConfig.MAGNET_PICK_BLOCKS.get() && OBMountedBlock.capture(this, actor, BlockPos.containing(getX(), getY() - 0.5, getZ()));
    }

    void alignToCargo(final Entity cargo) {
        // Begin follow movement with the cargo in its original, unobstructed space.
        setPos(cargo.getX(), cargo.getY() + cargo.getBbHeight(), cargo.getZ());
    }

    @Nullable
    private Entity findEntity() {
        final AABB scan = getBoundingBox().inflate(0.25, 0, 0.25).move(0, -1, 0);
        return level().getEntities(this, scan, target -> target != holder && target.isAlive() && !target.isPassenger() && !target.isSpectator()
                        && (target instanceof LivingEntity || target instanceof ItemEntity || target instanceof Boat || target instanceof AbstractMinecart
                        || OBServerConfig.MAGNET_ENTITY_ALLOWLIST.get().contains(ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString())))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) ensureFor(player);
    }

    @SubscribeEvent
    public static void onLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        retireFor(event.getEntity());
    }

    @SubscribeEvent
    public static void onDimensionChange(final PlayerEvent.PlayerChangedDimensionEvent event) {
        retireFor(event.getEntity());
    }

    private static void retireFor(final Player player) {
        final OBMagnet magnet = ON_SERVER.get(player);
        if (magnet != null) magnet.retire();
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            (level.isClientSide() ? ON_CLIENT : ON_SERVER).values().removeIf(magnet -> magnet.level() == level);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        if (holder != null) (level().isClientSide() ? ON_CLIENT : ON_SERVER).remove(holder, this);
        super.onRemovedFromWorld();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (holder == null && level().getEntity(entityData.get(HOLDER)) instanceof Player player) bind(player);
            if (lerpSteps > 0) {
                setPos(getX() + (lerpX - getX()) / lerpSteps, getY() + (lerpY - getY()) / lerpSteps, getZ() + (lerpZ - getZ()) / lerpSteps);
                lerpSteps--;
            }
            if (getType() == OBEntities.MAGNET.get() && random.nextInt(5) == 0) {
                level().addParticle(ParticleTypes.PORTAL, getX(), getY() + 0.2, getZ(),
                        random.nextGaussian() * 0.2, -Math.abs(random.nextGaussian()) * 0.2, random.nextGaussian() * 0.2);
            }
            return;
        }

        if (getType() == OBEntities.MAGNET.get()) {
            final Vec3 target = remoteTarget == null ? null : remoteTarget.get();
            if (target == null) retire();
            else follow(target);
            return;
        }

        if (holder == null && owner != null && level().getPlayerByUUID(owner) instanceof ServerPlayer player && forPlayer(player) == null) {
            bind(player);
            entityData.set(HOLDER, player.getId());
        }
        if (!(holder instanceof ServerPlayer player) || !player.isAlive() || !wearsBackpack(player) || player.level() != level() || player.isSpectator()) {
            retire();
            return;
        }

        limitArmCollision(player);
        final boolean operating = player.isUsingItem() && player.getUseItem().is(OBItems.CRANE_CONTROL.get());
        if (operating) cable = Mth.clamp(cable + (lowering ? 0.1 : -0.1), MIN_CABLE, MAX_CABLE);
        entityData.set(LIFT, operating ? lowering ? 2 : 1 : 0);
        follow(target(player));
    }

    private void follow(final Vec3 target) {
        moveTowards(target);
        entityData.set(DETECTS, OBServerConfig.MAGNET_PICK_ENTITIES.get() && findEntity() != null);
    }

    private Vec3 target(final Player player) {
        final float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        return player.position().add(-Mth.sin(yaw) * ARM_REACH, player.getBbHeight() - cable - getBbHeight(), Mth.cos(yaw) * ARM_REACH);
    }

    private void moveTowards(final Vec3 target) {
        final Vec3 delta = target.subtract(position());
        final double distance = delta.length();
        if (distance < 0.001) return;
        final Vec3 motion = delta.scale(distance < 0.01 ? 1 : distance > 5 ? 2.5 / distance : 0.5);
        final Entity load = getFirstPassenger();
        final AABB bounds = load == null ? getBoundingBox() : getBoundingBox().minmax(cargoBounds(load));
        final Vec3 accepted = Entity.collideBoundingBox(null, motion, bounds, level(), List.of());
        final Vec3 next = position().add(accepted);
        if (getType() != OBEntities.MAGNET.get() || level().hasChunkAt(BlockPos.containing(next))) setPos(next);
    }

    private AABB cargoBounds(final Entity cargo) {
        return cargo.getBoundingBox().move(getX() - cargo.getX(), getY() - cargo.getBbHeight() - cargo.getY(), getZ() - cargo.getZ());
    }

    @Override
    protected void positionRider(final Entity rider, final MoveFunction mover) {
        if (hasPassenger(rider)) mover.accept(rider, getX(), getY() - rider.getBbHeight(), getZ());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
        return passenger.position();
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    private void limitArmCollision(final ServerPlayer player) {
        if (!OBServerConfig.CRANE_COLLISIONS.get()) {
            lastSafePosition = null;
            return;
        }
        final double yaw = player.getYRot() * Mth.DEG_TO_RAD;
        final Vec3 base = player.position().add(0, player.getEyeHeight() + 0.2, 0);
        for (int part = 1; part <= 3; part++) {
            final Vec3 point = base.add(-Math.sin(yaw) * ARM_REACH * part / 3, 0, Math.cos(yaw) * ARM_REACH * part / 3);
            if (level().getBlockCollisions(player, AABB.ofSize(point, 0.2, 0.2, 0.2)).iterator().hasNext()) {
                if (lastSafePosition != null && player.distanceToSqr(lastSafePosition) < 16) {
                    player.setDeltaMovement(Vec3.ZERO);
                    player.connection.teleport(lastSafePosition.x, lastSafePosition.y, lastSafePosition.z, lastSafeYaw, player.getXRot());
                }
                return;
            }
        }
        lastSafePosition = player.position();
        lastSafeYaw = player.getYRot();
    }

    public void retire() {
        if (isRemoved()) return;
        if (getFirstPassenger() instanceof OBMountedBlock cargo) {
            if (holder instanceof ServerPlayer actor && actor.level() == level()) cargo.recover(actor);
            else if (machineOwner != null && level() instanceof ServerLevel server) cargo.recover(FakePlayerFactory.get(server, machineOwner));
        }
        if (remoteTarget != null) playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
        discard();
    }

    @Override
    public boolean isInvulnerableTo(final DamageSource source) {
        return true;
    }

    @Override
    public void lerpTo(final double x, final double y, final double z, final float yaw, final float pitch, final int steps, final boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpSteps = 2;
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        machineOwner = tag.contains("MachineOwner") ? NbtUtils.readGameProfile(tag.getCompound("MachineOwner")) : null;
        if (tag.contains("Cable")) cable = Mth.clamp(tag.getDouble("Cable"), MIN_CABLE, MAX_CABLE);
        lowering = tag.getBoolean("Lowering");
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        if (machineOwner != null) tag.put("MachineOwner", NbtUtils.writeGameProfile(new CompoundTag(), machineOwner));
        tag.putDouble("Cable", cable);
        tag.putBoolean("Lowering", lowering);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
