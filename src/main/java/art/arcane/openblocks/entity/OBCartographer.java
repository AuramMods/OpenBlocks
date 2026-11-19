package art.arcane.openblocks.entity;

import art.arcane.openblocks.item.OBEmptyMapItem;
import art.arcane.openblocks.item.OBHeightMapItem;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBHeightMapData;
import art.arcane.openblocks.world.OBHeightMapSurvey;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public final class OBCartographer extends Entity {

    private static final EntityDataAccessor<CompoundTag> PROGRESS =
            SynchedEntityData.defineId(OBCartographer.class, EntityDataSerializers.COMPOUND_TAG);
    private static final String MAP_ITEM = "MapItem";
    private static final String SURVEY = "ActiveSurvey";
    private static final int SCAN_DELAY = 5;

    @Nullable private UUID owner;
    private ItemStack mapItem = ItemStack.EMPTY;
    private CompoundTag itemTag = new CompoundTag();
    private long survey;
    private int nextScan;
    private float orbitAngle;
    private Vec3 clientTarget = Vec3.ZERO;
    private int interpolation;

    public OBCartographer(final EntityType<? extends OBCartographer> type, final Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(PROGRESS, new CompoundTag());
    }

    public void deploy(final Player player, final ItemStack stack) {
        owner = player.getUUID();
        final Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
        setPos(player.getX() + look.x, player.getEyeY(), player.getZ() + look.z);
        if (stack.hasTag()) {
            itemTag = stack.getTag().copy();
            readMap(itemTag);
            itemTag.remove(MAP_ITEM);
            itemTag.remove(SURVEY);
        }
        if (stack.hasCustomHoverName()) setCustomName(stack.getHoverName());
    }

    public CompoundTag progress() {
        return entityData.get(PROGRESS);
    }

    public int centerX() { return blockPosition().getX() & ~15; }
    public int centerZ() { return blockPosition().getZ() & ~15; }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            followOwner(server);
            if (!mapItem.isEmpty() && --nextScan <= 0) updateSurvey(server);
        } else if (interpolation > 0) {
            setPos(position().lerp(clientTarget, 1.0 / interpolation));
            interpolation--;
        }
    }

    private void followOwner(final ServerLevel level) {
        if (owner == null) return;
        final Player player = level.getPlayerByUUID(owner);
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        final boolean surveying = !mapItem.isEmpty();
        if (surveying && tickCount % 35 == 0) orbitAngle = random.nextFloat() * Mth.TWO_PI;
        final float yaw = surveying ? orbitAngle : player.getYRot() * Mth.DEG_TO_RAD;
        final Vec3 target = new Vec3(player.getX() - Mth.sin(yaw), player.getEyeY(), player.getZ() + Mth.cos(yaw));
        final Vec3 delta = target.subtract(position());
        if (delta.lengthSqr() > 256 && level.hasChunkAt(BlockPos.containing(target)) && level.noCollision(this, getBoundingBox().move(delta))) {
            setPos(target);
        } else if (delta.lengthSqr() > 1.0E-6) {
            move(MoverType.SELF, delta.scale(Math.min(0.5, 1.5 / delta.length())));
        }
    }

    private void updateSurvey(final ServerLevel server) {
        final OBHeightMapData map = OBHeightMapData.get(server, OBHeightMapItem.mapId(mapItem));
        nextScan = 20;
        if (map == null) {
            entityData.set(PROGRESS, new CompoundTag());
            return;
        }
        if (survey == map.survey() && OBHeightMapSurvey.scanNext(map, server, getX(), getZ())) nextScan = SCAN_DELAY;
        entityData.set(PROGRESS, map.progress());
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || distanceToSqr(player) >= 9
                || player.isSpectator()) return InteractionResult.PASS;

        if (level() instanceof ServerLevel server) {
            final ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty() && !mapItem.isEmpty()) {
                final OBHeightMapData map = OBHeightMapData.get(server, OBHeightMapItem.mapId(mapItem));
                if (map != null) OBHeightMapItem.updateMetadata(mapItem, map);
                player.setItemInHand(hand, mapItem);
                mapItem = ItemStack.EMPTY;
                survey = 0;
                entityData.set(PROGRESS, new CompoundTag());
            } else if (mapItem.isEmpty() && (held.is(OBItems.EMPTY_MAP.get()) || held.is(OBItems.HEIGHT_MAP.get()))) {
                beginSurvey(server, held);
                held.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    private void beginSurvey(final ServerLevel server, final ItemStack source) {
        int id = OBHeightMapItem.mapId(source);
        OBHeightMapData map = OBHeightMapData.get(server, id);
        if (map == null) {
            id = OBHeightMapData.create(server, blockPosition(), OBEmptyMapItem.scale(source));
            map = OBHeightMapData.get(server, id);
        } else {
            map.reset(server, blockPosition());
        }
        mapItem = OBHeightMapItem.create(source, id, map);
        survey = map.survey();
        nextScan = SCAN_DELAY;
        entityData.set(PROGRESS, map.progress());
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (isRemoved() || isInvulnerableTo(source)) return false;
        if (!level().isClientSide()) {
            final ItemEntity drop = new ItemEntity(level(), getX(), getY(), getZ(), pack());
            drop.setDefaultPickUpDelay();
            drop.setInvulnerable(true);
            drop.setUnlimitedLifetime();
            if (!level().addFreshEntity(drop)) return false;
            discard();
        }
        return true;
    }

    private ItemStack pack() {
        final ItemStack stack = new ItemStack(OBItems.CARTOGRAPHER.get());
        if (!itemTag.isEmpty()) stack.setTag(itemTag.copy());
        if (hasCustomName()) stack.setHoverName(getCustomName());
        if (!mapItem.isEmpty()) writeMap(stack.getOrCreateTag());
        return stack;
    }

    @Override
    public void lerpTo(final double x, final double y, final double z, final float yaw, final float pitch, final int steps, final boolean teleport) {
        clientTarget = new Vec3(x, y, z);
        interpolation = teleport ? 1 : 3;
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        itemTag = tag.getCompound("ItemTag").copy();
        readMap(tag);
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        if (!itemTag.isEmpty()) tag.put("ItemTag", itemTag.copy());
        writeMap(tag);
    }

    private void readMap(final CompoundTag tag) {
        mapItem = tag.contains(MAP_ITEM, Tag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound(MAP_ITEM)) : ItemStack.EMPTY;
        survey = tag.getLong(SURVEY);
    }

    private void writeMap(final CompoundTag tag) {
        if (!mapItem.isEmpty()) {
            tag.put(MAP_ITEM, mapItem.save(new CompoundTag()));
            tag.putLong(SURVEY, survey);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
