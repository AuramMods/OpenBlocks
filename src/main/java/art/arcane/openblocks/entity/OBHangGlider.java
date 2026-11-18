package art.arcane.openblocks.entity;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import java.time.LocalDate;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBHangGlider extends Entity {

    private static final EntityDataAccessor<Integer> HOLDER = SynchedEntityData.defineId(OBHangGlider.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OFF_HAND = SynchedEntityData.defineId(OBHangGlider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> THERMAL_DAY = SynchedEntityData.defineId(OBHangGlider.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(OBHangGlider.class, EntityDataSerializers.BOOLEAN);
    private static final Map<Player, OBHangGlider> ON_SERVER = new IdentityHashMap<>();
    private static final Map<Player, OBHangGlider> ON_CLIENT = new IdentityHashMap<>();

    public static final double FASTEST_DESCENT = -0.32D;
    public static final double MAX_THERMAL_LIFT = 0.4D;
    public static final double HEIGHT_ABOVE_FEET = 1.2D;

    @Nullable private Player holder;
    @Nullable private PerlinSimplexNoise thermalNoise;
    private int noiseDay;
    private double lastVerticalSpeed;
    private double glideRate = Double.NaN;

    public OBHangGlider(final EntityType<? extends OBHangGlider> type, final Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(HOLDER, -1);
        entityData.define(OFF_HAND, false);
        entityData.define(THERMAL_DAY, 0);
        entityData.define(FLYING, false);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public static void toggle(final ServerPlayer player, final InteractionHand hand) {
        final OBHangGlider equipped = activeFor(player);
        if (equipped != null) {
            if (equipped.isUsableBy(player)) {
                if (equipped.hand() == hand) equipped.discard();
                return;
            }
            equipped.discard();
        }
        if (!canUseGlider(player) || !player.getItemInHand(hand).is(OBItems.HANG_GLIDER.get())) return;

        final OBHangGlider glider = OBEntities.HANG_GLIDER.get().create(player.serverLevel());
        if (glider == null) return;
        glider.entityData.set(HOLDER, player.getId());
        glider.entityData.set(OFF_HAND, hand == InteractionHand.OFF_HAND);
        glider.entityData.set(THERMAL_DAY, LocalDate.now().getDayOfYear());
        glider.setPos(player.getX(), player.getY() + HEIGHT_ABOVE_FEET, player.getZ());
        if (player.serverLevel().addFreshEntity(glider)) glider.bind(player);
    }

    @Nullable
    public static OBHangGlider activeFor(final Player player) {
        final OBHangGlider glider = (player.level().isClientSide() ? ON_CLIENT : ON_SERVER).get(player);
        return glider != null && !glider.isRemoved() && glider.level() == player.level() ? glider : null;
    }

    public static boolean isHeldGlider(final Player player, final ItemStack stack) {
        final OBHangGlider glider = activeFor(player);
        return glider != null && glider.isUsableBy(player) && player.getItemInHand(glider.hand()) == stack;
    }

    public InteractionHand hand() {
        return entityData.get(OFF_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Nullable
    public Player holder() {
        return holder;
    }

    public boolean isFlying() {
        return holder != null && isUsableBy(holder)
                && (holder.isLocalPlayer() ? canGlide(holder) : entityData.get(FLYING));
    }

    public double glideRate() {
        return glideRate;
    }

    private void bind(final Player player) {
        holder = player;
        (level().isClientSide() ? ON_CLIENT : ON_SERVER).put(player, this);
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
        } else if (holder == null || !isUsableBy(holder)) {
            discard();
            return;
        }
        if (holder != null) setPos(holder.getX(), holder.getY() + HEIGHT_ABOVE_FEET, holder.getZ());
    }

    private boolean isUsableBy(final Player player) {
        return player.level() == level() && canUseGlider(player) && player.getItemInHand(hand()).is(OBItems.HANG_GLIDER.get());
    }

    private static boolean canUseGlider(final Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isFallFlying();
    }

    private static boolean canGlide(final Player player) {
        return !player.onGround() && !player.isInWater() && !player.isSleeping() && !player.isPassenger() && !player.getAbilities().flying;
    }

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || (event.player.level().isClientSide() && !event.player.isLocalPlayer())) return;
        final OBHangGlider glider = activeFor(event.player);
        if (glider != null) glider.updateFlight(event.player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        foldGlider(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        foldGlider(event.getEntity());
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            (level.isClientSide() ? ON_CLIENT : ON_SERVER).values().removeIf(glider -> glider.level() == level);
        }
    }

    private static void foldGlider(final Player player) {
        final OBHangGlider glider = ON_SERVER.get(player);
        if (glider != null) glider.discard();
    }

    private void updateFlight(final Player player) {
        if (!isUsableBy(player)) {
            glideRate = Double.NaN;
            if (!level().isClientSide()) discard();
            return;
        }
        final boolean airborne = canGlide(player);
        if (!level().isClientSide()) entityData.set(FLYING, airborne);
        if (!airborne) {
            lastVerticalSpeed = 0;
            glideRate = Double.NaN;
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            // Vanilla checks the residual after collision resolution and can flag even a normal glide descent.
            serverPlayer.connection.aboveGroundTickCount = 0;
        }

        final var velocity = player.getDeltaMovement();
        if (velocity.y >= lastVerticalSpeed) return;
        final boolean diving = player.isShiftKeyDown();
        final double vertical = verticalSpeed(diving, thermalsAt(player));
        final double yaw = player.getYRot() * Mth.DEG_TO_RAD;
        final double thrust = diving ? 0.1D : 0.03D;
        player.setDeltaMovement(velocity.x - Math.sin(yaw) * thrust, vertical, velocity.z + Math.cos(yaw) * thrust);
        player.fallDistance = 0;
        lastVerticalSpeed = vertical;
        glideRate = vertical;
    }

    private double thermalsAt(final Player player) {
        if (!OBServerConfig.HANG_GLIDER_THERMALS.get() || !level().dimension().equals(Level.OVERWORLD)) return 0;
        final int day = entityData.get(THERMAL_DAY);
        if (thermalNoise == null || noiseDay != day) {
            noiseDay = day;
            thermalNoise = new PerlinSimplexNoise(RandomSource.create(day), List.of(-1, 0));
        }
        // The 1.12 generator sums octaves with weights 1 and 2, then divides by 4.
        final double noise = 0.75D * thermalNoise.getValue(player.getX() / 20D, player.getZ() / 20D, false);
        final boolean wetBiome = level().getBiome(player.blockPosition()).value().getModifiedClimateSettings().downfall() > 0;
        return thermalStrength(noise, player.getY(), level().getDayTime(), level().isRaining(), wetBiome);
    }

    public static double thermalStrength(final double noise, final double altitude, final long time, final boolean raining, final boolean wetBiome) {
        final boolean strong = noise > 0.7D;
        if (raining && !strong) return wetBiome ? -0.5D : 0;
        final int bonus = strong ? 100 : 0;
        final long dayTime = Math.floorMod(time, 24_000L);
        return noise * Mth.clamp((altitude - 70D) / 40D, 0D, 1D) * Mth.clamp((136D + bonus - altitude) / (26D + bonus / 4D), 0D, 1D)
                * Math.min(dayTime / 1_000D, 1D) * Mth.clamp((12_000D - dayTime) / 1_000D, 0D, 1D);
    }

    public static double verticalSpeed(final boolean diving, final double thermal) {
        final double liftScale = thermal >= 0 ? MAX_THERMAL_LIFT : -FASTEST_DESCENT;
        return Math.max((diving ? -0.176D : -0.052D) + thermal * liftScale, FASTEST_DESCENT);
    }
}
