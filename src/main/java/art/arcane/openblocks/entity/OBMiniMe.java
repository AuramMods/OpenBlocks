package art.arcane.openblocks.entity;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBEntities;
import com.mojang.authlib.GameProfile;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OBMiniMe extends PathfinderMob {

    private static final int PICKUP_COOLDOWN = 1200;
    public static final GameProfile DEFAULT_OWNER = new GameProfile(UUID.fromString("d4d119aa-d410-488a-8734-0053577d4a1a"), null);
    private static final EntityDataAccessor<CompoundTag> APPEARANCE =
            SynchedEntityData.defineId(OBMiniMe.class, EntityDataSerializers.COMPOUND_TAG);

    @Nullable private GameProfile owner;
    @Nullable private String previousName;
    private int pickupCooldown;

    public OBMiniMe(final EntityType<? extends OBMiniMe> type, final Level level) {
        super(type, level);
        setPersistenceRequired();
        setPathfindingMalus(BlockPathTypes.WATER, -1);
        getNavigation().setCanFloat(true);
    }

    @SubscribeEvent
    public static void registerAttributes(final EntityAttributeCreationEvent event) {
        event.put(OBEntities.MINI_ME.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.3).build());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(APPEARANCE, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide && key.equals(APPEARANCE)) {
            owner = NbtUtils.readGameProfile(entityData.get(APPEARANCE));
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new PickupOwnerGoal());
        goalSelector.addGoal(3, new ForageGoal());
        goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override public boolean isBaby() { return true; }
    @Override public double getPassengersRidingOffset() { return getBbHeight() + 0.15; }

    @Override
    protected void removePassenger(final Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide) pickupCooldown = PICKUP_COOLDOWN;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (pickupCooldown > 0) pickupCooldown--;

        final Component customName = getCustomName();
        final String name = customName == null ? null : customName.getString();
        if (name != null && !name.isBlank() && !name.equals(previousName)
                && (owner == null || !name.equalsIgnoreCase(owner.getName()))) {
            setOwner(new GameProfile(null, name));
        }
        previousName = name;
    }

    @Nullable public GameProfile owner() { return owner; }
    public CompoundTag appearance() { return entityData.get(APPEARANCE); }
    public int pickupCooldown() { return pickupCooldown; }

    public void setOwner(@Nullable final GameProfile profile) {
        owner = profile;
        entityData.set(APPEARANCE, profile == null ? new CompoundTag() : NbtUtils.writeGameProfile(new CompoundTag(), profile));
        if (!(level() instanceof ServerLevel) || profile == null || profile.getProperties().containsKey("textures")) return;

        SkullBlockEntity.updateGameprofile(profile, resolved -> {
            if (owner != profile || isRemoved() || resolved == profile || resolved == null) return;
            if (profile.getId() != null && !Objects.equals(profile.getId(), resolved.getId())) return;
            owner = resolved;
            entityData.set(APPEARANCE, NbtUtils.writeGameProfile(new CompoundTag(), resolved));
        });
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.put("Owner", NbtUtils.writeGameProfile(new CompoundTag(), owner));
        tag.putInt("pickupCooldown", pickupCooldown);
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setOwner(readOwner(tag));
        final Component name = getCustomName();
        previousName = name == null ? null : name.getString();
        pickupCooldown = Math.max(0, tag.getInt("pickupCooldown"));
    }

    @Nullable
    public static GameProfile readOwner(final CompoundTag tag) {
        if (tag.contains("Owner", Tag.TAG_COMPOUND)) return NbtUtils.readGameProfile(tag.getCompound("Owner"));
        if (tag.hasUUID("Owner")) return new GameProfile(tag.getUUID("Owner"), null);
        if (tag.contains("OwnerUUID", Tag.TAG_STRING)) {
            try {
                return new GameProfile(UUID.fromString(tag.getString("OwnerUUID")), null);
            } catch (final IllegalArgumentException ignored) {
                return null;
            }
        }
        final String name = tag.getString("owner");
        return name.isBlank() ? null : new GameProfile(null, name);
    }

    private final class PickupOwnerGoal extends Goal {
        @Nullable private Player player;
        private int nextPath;

        PickupOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (pickupCooldown > 0 || isVehicle() || owner == null || owner.getId() == null) return false;
            player = level().getPlayerByUUID(owner.getId());
            return canCarryPlayer() && distanceToSqr(player) <= 100;
        }

        private boolean canCarryPlayer() {
            return player != null && player.isAlive() && !player.isSpectator() && player.isPassenger() && player.getVehicle() != OBMiniMe.this
                    && !isVehicle() && owner != null && player.getUUID().equals(owner.getId());
        }

        @Override public boolean canContinueToUse() { return canCarryPlayer() && distanceToSqr(player) <= 144; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { nextPath = 0; }

        @Override
        public void tick() {
            if (!canCarryPlayer()) return;
            if (distanceToSqr(player) < 1 && player.startRiding(OBMiniMe.this)) {
                getNavigation().stop();
            } else if (--nextPath <= 0) {
                nextPath = 10;
                getNavigation().moveTo(player, 1);
            }
        }

        @Override public void stop() {
            player = null;
            getNavigation().stop();
        }
    }

    private final class ForageGoal extends Goal {
        @Nullable private BlockPos target;
        @Nullable private Path path;
        private int nextSearch;

        ForageGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!(level() instanceof ServerLevel server) || !getNavigation().isDone() || tickCount < nextSearch) return false;
            nextSearch = tickCount + 4;
            if (!ForgeEventFactory.getMobGriefingEvent(server, OBMiniMe.this)) return false;
            for (int attempt = 0; attempt < 20; attempt++) {
                final BlockPos pos = blockPosition().offset(random.nextInt(16) - 8, random.nextInt(3) - 1, random.nextInt(16) - 8);
                if (!canHarvest(pos)) continue;
                final Path candidate = getNavigation().createPath(pos, 0);
                if (candidate == null || !candidate.canReach()) continue;
                target = pos;
                path = candidate;
                return true;
            }
            return false;
        }

        private boolean canHarvest(final BlockPos pos) {
            if (!level().hasChunkAt(pos)) return false;
            final var state = level().getBlockState(pos);
            return state.is(BlockTags.FLOWERS) || state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH);
        }

        @Override public boolean canContinueToUse() {
            return target != null && canHarvest(target) && (!getNavigation().isDone() || nearTarget());
        }

        @Override public void start() { getNavigation().moveTo(path, 1.0); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        private boolean nearTarget() {
            return distanceToSqr(Vec3.atCenterOf(target)) < 1;
        }

        @Override
        public void tick() {
            if (target == null || !nearTarget() || !(level() instanceof ServerLevel server)) return;

            if (canHarvest(target) && ForgeEventFactory.getMobGriefingEvent(server, OBMiniMe.this)) {
                final var actor = owner != null && owner.getId() != null ? FakePlayerFactory.get(server, owner) : FakePlayerFactory.getMinecraft(server);
                if (server.mayInteract(actor, target)) actor.gameMode.destroyBlock(target);
            }
            stop();
        }

        @Override public void stop() {
            target = null;
            path = null;
            getNavigation().stop();
        }
    }
}
