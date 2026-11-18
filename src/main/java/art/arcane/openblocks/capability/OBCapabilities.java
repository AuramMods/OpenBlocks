package art.arcane.openblocks.capability;

import art.arcane.openblocks.OpenBlocks;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBCapabilities {

    public static final ResourceLocation LUCK_ID = ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "luck");
    public static final ResourceLocation PEDOMETER_STATE_ID = ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "pedometer_state");
    public static final ResourceLocation BOWELS_ID = ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "bowels");

    public static final Capability<LuckData> LUCK = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<PedometerState> PEDOMETER_STATE = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<BowelContents> BOWELS = CapabilityManager.get(new CapabilityToken<>() {});

    private OBCapabilities() {}

    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.register(LuckData.class);
        event.register(PedometerState.class);
        event.register(BowelContents.class);
    }

    @SubscribeEvent
    public static void onAttachEntityCapabilities(final AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;

        attach(event, LUCK_ID, LUCK, LuckData::new);
        attach(event, PEDOMETER_STATE_ID, PEDOMETER_STATE, PedometerState::new);
        attach(event, BOWELS_ID, BOWELS, BowelContents::new);
    }

    @SubscribeEvent
    public static void onPlayerClone(final PlayerEvent.Clone event) {
        final Player original = event.getOriginal();
        final Player clone = event.getEntity();

        original.reviveCaps();
        try {
            copy(original, clone, LUCK);
            if (!event.isWasDeath()) {
                copy(original, clone, BOWELS);
                if (original.level().dimension().equals(clone.level().dimension())) copy(original, clone, PEDOMETER_STATE);
            }
        } finally {
            original.invalidateCaps();
        }
    }

    public static int getLuck(final Player player) {
        return player.getCapability(LUCK)
                .map(LuckData::getLuck)
                .orElse(0);
    }

    public static int addLuck(final Player player, final int amount) {
        return player.getCapability(LUCK)
                .map((data) -> data.addLuck(amount))
                .orElse(0);
    }

    public static OptionalInt getBowelBrickCount(final Player player) {
        return player.getCapability(BOWELS)
                .map((data) -> OptionalInt.of(data.getBrickCount()))
                .orElseGet(OptionalInt::empty);
    }

    public static int setBowelBrickCount(final Player player, final int count) {
        return player.getCapability(BOWELS)
                .map((data) -> {
                    data.setBrickCount(count);
                    return data.getBrickCount();
                })
                .orElse(0);
    }

    private static <T extends INBTSerializable<CompoundTag> & Copyable<T>> void copy(
            final Player original,
            final Player clone,
            final Capability<T> capability) {
        original.getCapability(capability).ifPresent((source) ->
                clone.getCapability(capability).ifPresent((target) -> target.copyFrom(source)));
    }

    private static <T extends INBTSerializable<CompoundTag>> void attach(
            final AttachCapabilitiesEvent<Entity> event,
            final ResourceLocation id,
            final Capability<T> capability,
            final Supplier<T> factory) {
        final OBSerializableCapabilityProvider<T> provider = new OBSerializableCapabilityProvider<>(capability, factory);
        event.addCapability(id, provider);
        event.addListener(provider::invalidate);
    }

    private interface Copyable<T> {
        void copyFrom(T other);
    }

    private static final class OBSerializableCapabilityProvider<T extends INBTSerializable<CompoundTag>>
            implements ICapabilitySerializable<CompoundTag> {

        private final Capability<T> capability;
        private final T instance;
        private LazyOptional<T> optional;

        private OBSerializableCapabilityProvider(final Capability<T> capability, final Supplier<T> factory) {
            this.capability = capability;
            this.instance = factory.get();
        }

        private void invalidate() {
            if (optional == null) return;
            final LazyOptional<T> invalidated = optional;
            optional = null;
            invalidated.invalidate();
        }

        @Override
        public <C> LazyOptional<C> getCapability(final Capability<C> capability, final Direction side) {
            if (capability != this.capability) return LazyOptional.empty();
            // Forge can revive the same player on a dimension change or while copying respawn data.
            if (optional == null) optional = LazyOptional.of(() -> instance);
            return optional.cast();
        }

        @Override
        public CompoundTag serializeNBT() {
            return instance.serializeNBT();
        }

        @Override
        public void deserializeNBT(final CompoundTag tag) {
            instance.deserializeNBT(tag);
        }
    }

    public static final class LuckData implements INBTSerializable<CompoundTag>, Copyable<LuckData> {
        private int luck;
        private int cooldown;
        private boolean forceNext;

        public int getLuck() {
            return luck;
        }

        public int addLuck(final int delta) {
            this.luck += delta;
            return this.luck;
        }

        public int getCooldown() {
            return cooldown;
        }

        public void setCooldown(final int cooldown) {
            this.cooldown = cooldown;
        }

        public boolean isForceNext() {
            return forceNext;
        }

        public void setForceNext(final boolean forceNext) {
            this.forceNext = forceNext;
        }

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag tag = new CompoundTag();
            tag.putInt("luck", luck);
            tag.putInt("cooldown", cooldown);
            tag.putBoolean("force_next", forceNext);
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundTag tag) {
            this.luck = tag.getInt("luck");
            this.cooldown = tag.getInt("cooldown");
            this.forceNext = tag.getBoolean("force_next");
        }

        @Override
        public void copyFrom(final LuckData other) {
            this.luck = other.luck;
            this.cooldown = other.cooldown;
            this.forceNext = other.forceNext;
        }
    }

    public static final class PedometerState implements INBTSerializable<CompoundTag>, Copyable<PedometerState> {
        private boolean running;
        private double totalDistance;
        private double currentSpeed;
        private long startTicks;
        private Vec3 start = Vec3.ZERO;
        private Vec3 lastSample = Vec3.ZERO;
        private long lastSampleTicks;
        private Vec3 lastCheck = Vec3.ZERO;
        private long lastCheckTicks;

        public boolean isRunning() {
            return running;
        }

        public double getTotalDistance() {
            return totalDistance;
        }

        public void stop() {
            running = false;
        }

        public void reset() {
            running = false;
            totalDistance = 0;
            currentSpeed = 0;
            startTicks = 0;
            start = Vec3.ZERO;
            lastSample = Vec3.ZERO;
            lastSampleTicks = 0;
            lastCheck = Vec3.ZERO;
            lastCheckTicks = 0;
        }

        public void start(final Player player) {
            start = lastSample = lastCheck = player.position();
            startTicks = lastSampleTicks = lastCheckTicks = player.level().getGameTime();
            totalDistance = 0;
            currentSpeed = 0;
            running = true;
        }

        public void tick(final Player player) {
            final long now = player.level().getGameTime();
            final long elapsed = now - lastSampleTicks;
            if (!running || elapsed <= 0) return;

            final Vec3 position = player.position();
            final double distance = position.distanceTo(lastSample);
            totalDistance += distance;
            currentSpeed = distance / elapsed;
            lastSample = position;
            lastSampleTicks = now;
        }

        public PedometerReport createReport(final Player player) {
            // Always report the sampled route, even if the player moved after the last sample.
            if (!running || lastSampleTicks <= startTicks) return null;

            final var report = new PedometerReport(start.x, start.y, start.z, lastSampleTicks - startTicks, totalDistance,
                    start.distanceTo(lastSample), lastCheck.distanceTo(lastSample), lastSampleTicks - lastCheckTicks, currentSpeed);
            lastCheck = lastSample;
            lastCheckTicks = lastSampleTicks;
            return report;
        }

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag tag = new CompoundTag();
            tag.putBoolean("running", running);
            tag.putDouble("total_distance", totalDistance);
            tag.putDouble("current_speed", currentSpeed);
            tag.putLong("start_ticks", startTicks);
            writePosition(tag, "start", start);
            writePosition(tag, "prev", lastSample);
            tag.putLong("prev_tick_time", lastSampleTicks);
            writePosition(tag, "last_check", lastCheck);
            tag.putLong("last_check_time", lastCheckTicks);
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundTag tag) {
            running = tag.getBoolean("running");
            totalDistance = tag.getDouble("total_distance");
            currentSpeed = tag.getDouble("current_speed");
            startTicks = tag.getLong("start_ticks");
            start = readPosition(tag, "start");
            lastSample = readPosition(tag, "prev");
            lastSampleTicks = tag.getLong("prev_tick_time");
            lastCheck = readPosition(tag, "last_check");
            lastCheckTicks = tag.getLong("last_check_time");
        }

        private static void writePosition(final CompoundTag tag, final String prefix, final Vec3 position) {
            tag.putDouble(prefix + "_x", position.x);
            tag.putDouble(prefix + "_y", position.y);
            tag.putDouble(prefix + "_z", position.z);
        }

        private static Vec3 readPosition(final CompoundTag tag, final String prefix) {
            return new Vec3(tag.getDouble(prefix + "_x"), tag.getDouble(prefix + "_y"), tag.getDouble(prefix + "_z"));
        }

        @Override
        public void copyFrom(final PedometerState other) {
            running = other.running;
            totalDistance = other.totalDistance;
            currentSpeed = other.currentSpeed;
            startTicks = other.startTicks;
            start = other.start;
            lastSample = other.lastSample;
            lastSampleTicks = other.lastSampleTicks;
            lastCheck = other.lastCheck;
            lastCheckTicks = other.lastCheckTicks;
        }
    }

    public record PedometerReport(double startX, double startY, double startZ, long totalTime, double totalDistance,
            double straightLineDistance, double lastCheckDistance, long lastCheckTime, double currentSpeed) {

        public double averageSpeed() {
            return totalTime > 0 ? totalDistance / totalTime : 0;
        }

        public double straightLineSpeed() {
            return totalTime > 0 ? straightLineDistance / totalTime : 0;
        }

        public double lastCheckSpeed() {
            return lastCheckTime > 0 ? lastCheckDistance / lastCheckTime : 0;
        }
    }

    public static final class BowelContents implements INBTSerializable<CompoundTag>, Copyable<BowelContents> {
        private int brickCount;

        public int getBrickCount() {
            return brickCount;
        }

        public void setBrickCount(final int brickCount) {
            this.brickCount = Math.max(0, brickCount);
        }

        public void addBrick() {
            if (brickCount < Integer.MAX_VALUE) brickCount++;
        }

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag tag = new CompoundTag();
            tag.putInt("brick_count", brickCount);
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundTag tag) {
            setBrickCount(tag.getInt("brick_count"));
        }

        @Override
        public void copyFrom(final BowelContents other) {
            this.brickCount = other.brickCount;
        }
    }
}
