package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.command.OBInventoryStore;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBSounds;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

public class OBGraveBlockEntity extends BlockEntity {

    private static final String TAG_INVENTORY_ID = "InventoryId";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_OWNER_UUID = "OwnerUUID";
    private static final String TAG_EPITAPH = "Epitaph";
    private static final String TAG_OCCUPIED = "Occupied";
    private static final String TAG_DEATH_MESSAGE = "DeathMessage";
    private static final String TAG_DEATH_COMPONENT = "DeathMessageComponent";
    private static final String TAG_LOOT = "Loot";
    private static final String TAG_CREATED = "Created";

    private String legacyDumpId = "";
    private List<ItemStack> loot = List.of();
    private String ownerName = "";
    private String ownerUuid = "";
    private String epitaph = "";
    private Component deathMessage;
    private long created;
    private boolean claiming;
    private boolean clientOccupied;

    public OBGraveBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.GRAVE.get(), pos, state);
    }

    public void initializeFromDeath(final ServerPlayer player, final List<ItemStack> stacks, final String epitaph, final Component deathMessage) {
        this.legacyDumpId = "";
        this.loot = stacks.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
        this.deathMessage = deathMessage.copy();
        nameMemorial(player, epitaph);
    }

    public void nameMemorial(final ServerPlayer player, final String epitaph) {
        this.ownerName = player.getGameProfile().getName();
        this.ownerUuid = player.getUUID().toString();
        this.epitaph = epitaph;
        this.created = System.currentTimeMillis();
        sync();
    }

    public String epitaph() { return epitaph; }

    private void sync() {
        setChanged();
        if (level instanceof ServerLevel server) {
            final BlockState state = getBlockState();
            server.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public void sendDeathMessage(final ServerPlayer player) {
        if (deathMessage != null) {
            player.sendSystemMessage(deathMessage);
            return;
        }

        if (!ownerName.isBlank()) {
            player.sendSystemMessage(Component.translatableWithFallback("grave.open_blocks.owner", "Here lies %s.", ownerName));
            return;
        }

        player.sendSystemMessage(Component.translatableWithFallback("grave.open_blocks.memorial", "A weathered gravestone."));
    }

    public boolean hasLoot() {
        if (level != null && level.isClientSide()) return clientOccupied;
        return !loot.isEmpty() || !legacyDumpId.isBlank();
    }

    public boolean claimLoot(final ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel) || player.serverLevel() != serverLevel || claiming || player.isSpectator()
                || !serverLevel.mayInteract(player, worldPosition)) return false;
        claiming = true;
        try {
            final String dumpId = legacyDumpId;
            final List<ItemStack> stacks = dumpId.isBlank() ? loot : OBInventoryStore.readDroppedItems(serverLevel, dumpId);
            if (stacks.isEmpty()) {
                player.sendSystemMessage(dumpId.isBlank()
                        ? Component.translatableWithFallback("grave.open_blocks.empty", "This grave is empty.")
                        : Component.translatableWithFallback("grave.open_blocks.backup_missing", "Grave backup '%s' is missing or empty.", dumpId));
                return false;
            }
            if (!releaseLoot(serverLevel, stacks, dumpId)) {
                player.sendSystemMessage(Component.translatableWithFallback("grave.open_blocks.release_denied", "The grave kept its items. Try digging it again where drops are allowed."));
                return false;
            }
            serverLevel.levelEvent(2001, worldPosition, Block.getId(Blocks.DIRT.defaultBlockState()));
            if (serverLevel.random.nextDouble() < OBServerConfig.GRAVE_SPECIAL_ACTION.get()) {
                serverLevel.playSound(null, player.blockPosition(), OBSounds.GRAVE_ROB.get(), SoundSource.BLOCKS, 1, 1);
                serverLevel.setWeatherParameters(0, 35 * 20, true, true);
            }
            return true;
        } catch (final IOException e) {
            player.sendSystemMessage(Component.translatableWithFallback("grave.open_blocks.backup_failed",
                    "Could not access grave backup '%s'. %s", legacyDumpId, e.getMessage()));
            return false;
        } finally {
            claiming = false;
        }
    }

    private boolean releaseLoot(final ServerLevel serverLevel, final List<ItemStack> stacks, final String dumpId) throws IOException {
        final Vec3 center = Vec3.atCenterOf(worldPosition);
        final List<ItemEntity> spawned = new ArrayList<>(stacks.size());
        boolean released = false;
        try {
            for (final ItemStack stack : stacks) {
                final var drop = new ItemEntity(serverLevel, center.x, center.y, center.z, stack.copy());
                drop.setDefaultPickUpDelay();
                if (!serverLevel.addFreshEntity(drop)) return false;
                spawned.add(drop);
            }
            if (serverLevel.getBlockEntity(worldPosition) != this || !dumpId.isBlank() && !OBInventoryStore.deleteDump(serverLevel, dumpId)) {
                return false;
            }
            loot = List.of();
            legacyDumpId = "";
            sync();
            released = true;
            return true;
        } finally {
            if (!released) spawned.forEach(Entity::discard);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        if (!legacyDumpId.isBlank()) tag.putString(TAG_INVENTORY_ID, legacyDumpId);
        final var savedLoot = new ListTag();
        for (final ItemStack stack : loot) savedLoot.add(stack.save(new CompoundTag()));
        tag.put(TAG_LOOT, savedLoot);
        tag.putString(TAG_OWNER_NAME, ownerName);
        tag.putString(TAG_OWNER_UUID, ownerUuid);
        tag.putString(TAG_EPITAPH, epitaph);
        if (deathMessage != null) tag.putString(TAG_DEATH_COMPONENT, Component.Serializer.toJson(deathMessage));
        tag.putLong(TAG_CREATED, created);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        this.legacyDumpId = tag.getString(TAG_INVENTORY_ID);
        final ListTag savedLoot = tag.getList(TAG_LOOT, Tag.TAG_COMPOUND);
        final List<ItemStack> stacks = new ArrayList<>(savedLoot.size());
        for (int i = 0; i < savedLoot.size(); i++) {
            final ItemStack stack = ItemStack.of(savedLoot.getCompound(i));
            if (!stack.isEmpty()) stacks.add(stack);
        }
        this.loot = List.copyOf(stacks);
        this.ownerName = tag.getString(TAG_OWNER_NAME);
        this.ownerUuid = tag.getString(TAG_OWNER_UUID);
        this.epitaph = tag.contains(TAG_EPITAPH, Tag.TAG_STRING) ? tag.getString(TAG_EPITAPH) : ownerName;
        this.deathMessage = readDeathMessage(tag);
        this.created = tag.getLong(TAG_CREATED);
        this.clientOccupied = tag.getBoolean(TAG_OCCUPIED);
    }

    @Override
    public CompoundTag getUpdateTag() {
        final var update = new CompoundTag();
        update.putString(TAG_EPITAPH, epitaph);
        update.putBoolean(TAG_OCCUPIED, hasLoot());
        return update;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBGraveBlockEntity grave) {
        if (!(level instanceof ServerLevel server) || !OBServerConfig.GRAVE_AMBIENT_MOBS.get()
                || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || level.random.nextDouble() >= OBServerConfig.GRAVE_AMBIENT_RATE.get()) return;
        if (server.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(7), mob -> mob instanceof Enemy && mob.isAlive()).size() >= 5) return;

        final EntityType<? extends Mob> type = server.random.nextBoolean() ? EntityType.SKELETON : EntityType.BAT;
        final MobSpawnType reason = MobSpawnType.MOB_SUMMONED;
        if (!SpawnPlacements.checkSpawnRules(type, server, reason, pos, server.random)) return;
        final Mob mob = type.create(server);
        if (mob == null) return;
        final Vec3 center = Vec3.atCenterOf(pos);
        mob.moveTo(center.x, center.y, center.z, server.random.nextFloat() * 360, 0);
        if (!ForgeEventFactory.checkSpawnPosition(mob, server, reason)) return;
        ForgeEventFactory.onFinalizeSpawn(mob, server, server.getCurrentDifficultyAt(pos), reason, null, null);
        if (!mob.isSpawnCancelled()) server.addFreshEntity(mob);
    }

    private static Component readDeathMessage(final CompoundTag tag) {
        if (tag.contains(TAG_DEATH_COMPONENT, Tag.TAG_STRING)) {
            try {
                return Component.Serializer.fromJson(tag.getString(TAG_DEATH_COMPONENT));
            } catch (final JsonParseException ignored) {
                // Older saves store the message as plain text.
            }
        }
        final String text = tag.getString(TAG_DEATH_MESSAGE);
        return text.isBlank() ? null : Component.literal(text);
    }
}
