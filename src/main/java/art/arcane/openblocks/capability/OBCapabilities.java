package art.arcane.openblocks.capability;

import art.arcane.openblocks.OpenBlocks;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
            copy(original, clone, PEDOMETER_STATE);
            if (!event.isWasDeath()) copy(original, clone, BOWELS);
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
                    final int normalized = Math.max(0, count);
                    data.setBrickCount(normalized);
                    return normalized;
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
        private final LazyOptional<T> optional;

        private OBSerializableCapabilityProvider(final Capability<T> capability, final Supplier<T> factory) {
            this.capability = capability;
            this.instance = factory.get();
            this.optional = LazyOptional.of(() -> instance);
        }

        private void invalidate() {
            optional.invalidate();
        }

        @Override
        public <C> LazyOptional<C> getCapability(final Capability<C> capability, final Direction side) {
            if (capability == this.capability) return optional.cast();
            return LazyOptional.empty();
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
        private long startTicks;
        private double startX;
        private double startY;
        private double startZ;
        private double prevX;
        private double prevY;
        private double prevZ;
        private long prevTickTime;
        private double lastCheckX;
        private double lastCheckY;
        private double lastCheckZ;
        private long lastCheckTime;

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag tag = new CompoundTag();
            tag.putBoolean("running", running);
            tag.putDouble("total_distance", totalDistance);
            tag.putLong("start_ticks", startTicks);
            tag.putDouble("start_x", startX);
            tag.putDouble("start_y", startY);
            tag.putDouble("start_z", startZ);
            tag.putDouble("prev_x", prevX);
            tag.putDouble("prev_y", prevY);
            tag.putDouble("prev_z", prevZ);
            tag.putLong("prev_tick_time", prevTickTime);
            tag.putDouble("last_check_x", lastCheckX);
            tag.putDouble("last_check_y", lastCheckY);
            tag.putDouble("last_check_z", lastCheckZ);
            tag.putLong("last_check_time", lastCheckTime);
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundTag tag) {
            this.running = tag.getBoolean("running");
            this.totalDistance = tag.getDouble("total_distance");
            this.startTicks = tag.getLong("start_ticks");
            this.startX = tag.getDouble("start_x");
            this.startY = tag.getDouble("start_y");
            this.startZ = tag.getDouble("start_z");
            this.prevX = tag.getDouble("prev_x");
            this.prevY = tag.getDouble("prev_y");
            this.prevZ = tag.getDouble("prev_z");
            this.prevTickTime = tag.getLong("prev_tick_time");
            this.lastCheckX = tag.getDouble("last_check_x");
            this.lastCheckY = tag.getDouble("last_check_y");
            this.lastCheckZ = tag.getDouble("last_check_z");
            this.lastCheckTime = tag.getLong("last_check_time");
        }

        @Override
        public void copyFrom(final PedometerState other) {
            this.running = other.running;
            this.totalDistance = other.totalDistance;
            this.startTicks = other.startTicks;
            this.startX = other.startX;
            this.startY = other.startY;
            this.startZ = other.startZ;
            this.prevX = other.prevX;
            this.prevY = other.prevY;
            this.prevZ = other.prevZ;
            this.prevTickTime = other.prevTickTime;
            this.lastCheckX = other.lastCheckX;
            this.lastCheckY = other.lastCheckY;
            this.lastCheckZ = other.lastCheckZ;
            this.lastCheckTime = other.lastCheckTime;
        }
    }

    public static final class BowelContents implements INBTSerializable<CompoundTag>, Copyable<BowelContents> {
        private int brickCount;

        public int getBrickCount() {
            return brickCount;
        }

        public void setBrickCount(final int brickCount) {
            this.brickCount = brickCount;
        }

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag tag = new CompoundTag();
            tag.putInt("brick_count", brickCount);
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundTag tag) {
            this.brickCount = tag.getInt("brick_count");
        }

        @Override
        public void copyFrom(final BowelContents other) {
            this.brickCount = other.brickCount;
        }
    }
}
