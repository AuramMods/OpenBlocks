package art.arcane.openblocks.command;

import art.arcane.openblocks.api.OBInventoryEvent;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraftforge.common.MinecraftForge;

public final class OBInventoryStore {

    private static final String PREFIX = "inventory-";
    private static final String EXT = ".dat";
    private static final String ID_MAIN_INVENTORY = "main";
    private static final String ID_ARMOR_INVENTORY = "armor";
    private static final String ID_OFFHAND_INVENTORY = "offhand";
    private static final String ID_ENDER_CHEST_INVENTORY = "ender_chest";

    private static final String TAG_CREATED = "Created";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_PLAYER_NAME = "PlayerName";
    private static final String TAG_PLAYER_UUID = "PlayerUUID";
    private static final String TAG_LOCATION = "Location";
    private static final String TAG_LOCATION_X = "X";
    private static final String TAG_LOCATION_Y = "Y";
    private static final String TAG_LOCATION_Z = "Z";
    private static final String TAG_LOCATION_DIMENSION = "Dimension";
    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_SUB_INVENTORIES = "SubInventories";
    private static final String TAG_SLOT = "Slot";
    private static final String TYPE_GRAVE = "grave";

    private static final int MAIN_SLOTS = 36;
    private static final int ARMOR_SLOT_BASE = 100;
    private static final int ARMOR_SLOT_COUNT = 4;
    private static final int OFFHAND_SLOT = 150;
    private static final int PLAYER_SLOTS = MAIN_SLOTS + ARMOR_SLOT_COUNT + 1;
    private static final int LEGACY_DATA_VERSION = 1343;

    private static final Pattern SAFE_CHARS = Pattern.compile("[^A-Za-z0-9_-]");
    private static final Pattern SAFE_DUMP_ID_CHARS = Pattern.compile("[^A-Za-z0-9_.-]");
    private static final ThreadLocal<SimpleDateFormat> FILE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.ROOT));

    private OBInventoryStore() {}

    public static String storePlayerInventory(final ServerPlayer player, final String type) throws IOException {
        final Map<String, OBInventoryEvent.SubInventory> subInventories = collectSubInventories(player);
        final CompoundTag root = dumpMetadata(player, type);
        root.put(TAG_INVENTORY, player.getInventory().save(new ListTag()));
        root.put(TAG_SUB_INVENTORIES, encodeSubInventories(subInventories));
        return writeDump(player, type, root);
    }

    public static String storeGraveBackup(final ServerPlayer player, final BlockPos gravePos, final List<ItemStack> drops)
            throws IOException {
        final CompoundTag root = dumpMetadata(player, TYPE_GRAVE);
        root.put(TAG_INVENTORY, encodeMainInventoryList(drops));
        root.putBoolean("Placed", gravePos != null);
        if (gravePos != null) {
            final var location = new CompoundTag();
            location.putInt(TAG_LOCATION_X, gravePos.getX());
            location.putInt(TAG_LOCATION_Y, gravePos.getY());
            location.putInt(TAG_LOCATION_Z, gravePos.getZ());
            location.putString(TAG_LOCATION_DIMENSION, player.level().dimension().location().toString());
            root.put("GraveLocation", location);
        }
        root.put(TAG_SUB_INVENTORIES, new CompoundTag());
        return writeDump(player, TYPE_GRAVE, root);
    }

    private static CompoundTag dumpMetadata(final ServerPlayer player, final String type) {
        final var root = new CompoundTag();
        root.putLong(TAG_CREATED, System.currentTimeMillis());
        root.putString(TAG_TYPE, type);
        root.putString(TAG_PLAYER_NAME, player.getGameProfile().getName());
        root.putString(TAG_PLAYER_UUID, player.getUUID().toString());
        root.put(TAG_LOCATION, encodePlayerLocation(player));
        return root;
    }

    public static boolean restoreInventory(final ServerPlayer player, final String inventoryId) throws IOException {
        final Snapshot backup = readSnapshot(player.serverLevel(), inventoryId);
        if (backup == null || !restoreMain(player, backup.main())) return false;

        final Map<String, OBInventoryEvent.SubInventory> subInventories = backup.subInventories();
        final var armorSubInventory = findSubInventory(subInventories, ID_ARMOR_INVENTORY);
        if (armorSubInventory != null) applyIndexedStacks(player.getInventory().armor, armorSubInventory);

        final var offhandSubInventory = findSubInventory(subInventories, ID_OFFHAND_INVENTORY);
        if (offhandSubInventory != null) applyIndexedStacks(player.getInventory().offhand, offhandSubInventory);

        final var enderChestSubInventory = findSubInventory(subInventories, ID_ENDER_CHEST_INVENTORY);
        if (enderChestSubInventory != null) {
            applyContainer(player.getEnderChestInventory(), enderChestSubInventory);
            player.getEnderChestInventory().setChanged();
        }

        MinecraftForge.EVENT_BUS.post(new OBInventoryEvent.Load(player, subInventories));

        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        return true;
    }

    private static boolean restoreMain(final ServerPlayer player, final IndexedInventory saved) {
        final var inventory = player.getInventory();
        final int slots = (int) Math.min(saved.size(), inventory.getContainerSize());
        if (!spawnStacks(player.serverLevel(), player.position(), saved.stacks().tailMap(slots, true).values())) return false;
        for (int i = 0; i < slots; i++) inventory.setItem(i, saved.stacks().getOrDefault(i, ItemStack.EMPTY));
        inventory.setChanged();
        return true;
    }

    public static List<ItemStack> getSpawnStacks(
            final ServerLevel level,
            final String inventoryId,
            final String target,
            final Integer slotIndex) throws IOException {
        final Snapshot backup = readSnapshot(level, inventoryId);
        if (backup == null) return List.of();
        if (ID_MAIN_INVENTORY.equalsIgnoreCase(target)) return selectStacks(backup.main(), slotIndex);

        final var subInventory = findSubInventory(backup.subInventories(), target);
        if (subInventory != null) {
            final var slots = new TreeMap<>(subInventory.asMap());
            final long size = switch (target.toLowerCase(Locale.ROOT)) {
                case ID_ARMOR_INVENTORY -> ARMOR_SLOT_COUNT;
                case ID_OFFHAND_INVENTORY -> 1;
                case ID_ENDER_CHEST_INVENTORY -> 27;
                default -> slots.isEmpty() ? 0 : (long) slots.lastKey() + 1;
            };
            return selectStacks(new IndexedInventory(size, slots), slotIndex);
        }
        if (!backup.grave() && ID_ARMOR_INVENTORY.equalsIgnoreCase(target)) {
            return selectStacks(backup.main().section(MAIN_SLOTS, ARMOR_SLOT_COUNT), slotIndex);
        }
        if (!backup.grave() && ID_OFFHAND_INVENTORY.equalsIgnoreCase(target)) {
            return selectStacks(backup.main().section(PLAYER_SLOTS - 1, 1), slotIndex);
        }
        throw new IllegalArgumentException("Unsupported sub inventory: " + target);
    }

    private static List<ItemStack> selectStacks(final IndexedInventory inventory, final Integer slotIndex) {
        if (slotIndex == null) return inventory.stacks().values().stream().map(ItemStack::copy).toList();
        if (slotIndex < 0 || slotIndex >= inventory.size()) throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        final ItemStack stack = inventory.stacks().get(slotIndex);
        return stack == null ? List.of() : List.of(stack.copy());
    }

    public static boolean spawnStacks(final ServerLevel level, final Vec3 pos, final Iterable<ItemStack> stacks) {
        final List<ItemEntity> spawned = new ArrayList<>();
        boolean complete = false;
        try {
            for (final ItemStack stack : stacks) {
                final var drop = new ItemEntity(level, pos.x, pos.y, pos.z, stack.copy());
                drop.setDefaultPickUpDelay();
                if (!level.addFreshEntity(drop)) return false;
                spawned.add(drop);
            }
            complete = true;
            return true;
        } finally {
            if (!complete) spawned.forEach(Entity::discard);
        }
    }

    public static List<String> getAvailableTargets(final ServerLevel level, final String inventoryId) {
        try {
            final Snapshot backup = readSnapshot(level, inventoryId);
            if (backup == null) return List.of();

            final Set<String> targets = new LinkedHashSet<>();
            targets.add(ID_MAIN_INVENTORY);
            if (!backup.grave()) {
                targets.add(ID_ARMOR_INVENTORY);
                targets.add(ID_OFFHAND_INVENTORY);
            }
            targets.addAll(backup.subInventories().keySet());
            return List.copyOf(targets);
        } catch (final IOException ignored) {
            // Suggestion path should stay quiet.
        }

        return List.of();
    }

    public static List<String> getMatchedDumps(final ServerLevel level, final String prefix) {
        final Path folder = getSaveFolder(level);
        if (!Files.isDirectory(folder)) return List.of();

        final String normalizedPrefix = prefix == null ? "" : prefix;
        final String actualPrefix = normalizedPrefix.startsWith(PREFIX) ? normalizedPrefix : PREFIX + normalizedPrefix;

        try (var files = Files.list(folder)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith(actualPrefix))
                    .filter(name -> name.endsWith(EXT))
                    .map(OBInventoryStore::stripFilename)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (final IOException ignored) {
            return List.of();
        }
    }

    public static Path resolveDumpPath(final ServerLevel level, final String inventoryId) {
        final String id = SAFE_DUMP_ID_CHARS.matcher(stripFilename(inventoryId)).replaceAll("_");
        return getSaveFolder(level).resolve(PREFIX + id + EXT);
    }

    public static List<ItemStack> readDroppedItems(final ServerLevel level, final String inventoryId) throws IOException {
        final Snapshot backup = readSnapshot(level, inventoryId);
        return backup == null ? List.of() : selectStacks(backup.main(), null);
    }

    public static boolean deleteDump(final ServerLevel level, final String inventoryId) throws IOException {
        return Files.deleteIfExists(resolveDumpPath(level, inventoryId));
    }

    private static String writeDump(final ServerPlayer player, final String type, final CompoundTag root) throws IOException {
        final ServerLevel level = player.serverLevel();
        final Path folder = Files.createDirectories(getSaveFolder(level));
        final Path pending = Files.createTempFile(folder, ".inventory-", ".tmp");
        try {
            NbtIo.writeCompressed(root, pending.toFile());
            synchronized (OBInventoryStore.class) {
                final Path dest = createDumpPath(level, sanitize(player.getGameProfile().getName()), sanitize(type));
                try {
                    Files.move(pending, dest, StandardCopyOption.ATOMIC_MOVE);
                } catch (final AtomicMoveNotSupportedException e) {
                    Files.move(pending, dest);
                }
                return stripFilename(dest.getFileName().toString());
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private static Path createDumpPath(final ServerLevel level, final String playerName, final String type) {
        final String timestamp = FILE_FORMAT.get().format(new Date());
        int index = 0;

        while (true) {
            final String fileName = PREFIX + playerName + "-" + timestamp + "-" + type + "-" + index + EXT;
            final Path path = getSaveFolder(level).resolve(fileName);
            if (!Files.exists(path)) return path;
            index++;
        }
    }

    private static CompoundTag readInventoryTag(final ServerLevel level, final String inventoryId) throws IOException {
        final Path path = resolveDumpPath(level, inventoryId);
        if (!Files.isRegularFile(path)) return null;
        return NbtIo.readCompressed(path.toFile());
    }

    private static Snapshot readSnapshot(final ServerLevel level, final String inventoryId) throws IOException {
        final CompoundTag root = readInventoryTag(level, inventoryId);
        if (root == null) return null;
        final Tag inventory = root.get(TAG_INVENTORY);
        final boolean legacy = inventory instanceof CompoundTag;
        final boolean grave = TYPE_GRAVE.equals(root.getString(TAG_TYPE));
        final IndexedInventory main;
        if (inventory instanceof CompoundTag legacyInventory) {
            if (!legacyInventory.contains("size", Tag.TAG_INT) || legacyInventory.getInt("size") < 0) {
                throw new IOException("Invalid 1.12.2 inventory size");
            }
            final int size = legacyInventory.getInt("size");
            main = new IndexedInventory(size, decodeStacks(requireItemList(legacyInventory.get("Items")), true, false, size));
        } else if (grave) {
            final var slots = decodeStacks(requireItemList(inventory), false, false, (long) Integer.MAX_VALUE + 1);
            main = new IndexedInventory(slots.isEmpty() ? 0 : (long) slots.lastKey() + 1, slots);
        } else {
            main = new IndexedInventory(PLAYER_SLOTS, decodeStacks(requireItemList(inventory), false, true, PLAYER_SLOTS));
        }

        final Map<String, OBInventoryEvent.SubInventory> subs = new LinkedHashMap<>();
        if (root.get(TAG_SUB_INVENTORIES) instanceof CompoundTag subTag) {
            for (final String key : subTag.getAllKeys()) {
                if (key.isBlank() || ID_MAIN_INVENTORY.equalsIgnoreCase(key)) continue;
                final long size = switch (key.toLowerCase(Locale.ROOT)) {
                    case ID_ARMOR_INVENTORY -> ARMOR_SLOT_COUNT;
                    case ID_OFFHAND_INVENTORY -> 1;
                    case ID_ENDER_CHEST_INVENTORY -> 27;
                    default -> (long) Integer.MAX_VALUE + 1;
                };
                final var sub = new OBInventoryEvent.SubInventory();
                decodeStacks(requireItemList(subTag.get(key)), legacy, false, size).forEach(sub::addItemStack);
                subs.put(key, sub);
            }
        }
        return new Snapshot(main, subs, grave);
    }

    private static ListTag requireItemList(final Tag tag) throws IOException {
        if (!(tag instanceof ListTag list) || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IOException("Inventory must contain a list of item stacks");
        }
        return list;
    }

    private static NavigableMap<Integer, ItemStack> decodeStacks(final ListTag list, final boolean legacy, final boolean playerSlots,
            final long size) throws IOException {
        final var result = new TreeMap<Integer, ItemStack>();
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag tag = list.getCompound(i);
            if (tag.isEmpty() || (tag.contains("Count", Tag.TAG_ANY_NUMERIC) && tag.getInt("Count") <= 0)) continue;
            final int savedSlot = readSlotIndex(tag);
            final int slot = playerSlots ? playerSlot(savedSlot) : savedSlot;
            if (slot < 0 || slot >= size || result.containsKey(slot)) throw new IOException("Invalid or duplicate inventory slot " + savedSlot);
            final ItemStack stack = ItemStack.of(legacy ? updateLegacyItem(tag) : tag);
            if (stack.isEmpty()) throw new IOException("Could not read item '" + tag.getString("id") + "' in slot " + savedSlot);
            result.put(slot, stack);
        }
        return result;
    }

    private static CompoundTag updateLegacyItem(final CompoundTag stack) throws IOException {
        requireVanillaLegacyItems(stack);
        final Tag updated = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, stack.copy()),
                LEGACY_DATA_VERSION, SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        if (updated instanceof CompoundTag tag) return tag;
        throw new IOException("Could not convert legacy item '" + stack.getString("id") + "'");
    }

    private static void requireVanillaLegacyItems(final Tag data) throws IOException {
        if (data instanceof CompoundTag tag) {
            if (tag.contains("id", Tag.TAG_STRING) && tag.contains("Count", Tag.TAG_ANY_NUMERIC) && tag.getInt("Count") > 0) {
                final ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
                if (id == null || !"minecraft".equals(id.getNamespace())) {
                    throw new IOException("Legacy modded item '" + tag.getString("id") + "' needs its own migration");
                }
            }
            for (final String key : tag.getAllKeys()) requireVanillaLegacyItems(tag.get(key));
        } else if (data instanceof ListTag list) {
            for (final Tag entry : list) requireVanillaLegacyItems(entry);
        }
    }

    private static int playerSlot(final int savedSlot) {
        if (savedSlot >= 0 && savedSlot < MAIN_SLOTS) return savedSlot;
        if (savedSlot >= ARMOR_SLOT_BASE && savedSlot < ARMOR_SLOT_BASE + ARMOR_SLOT_COUNT) return MAIN_SLOTS + savedSlot - ARMOR_SLOT_BASE;
        return savedSlot == OFFHAND_SLOT ? PLAYER_SLOTS - 1 : -1;
    }

    private static Path getSaveFolder(final ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
    }

    private static CompoundTag encodePlayerLocation(final ServerPlayer player) {
        final CompoundTag location = new CompoundTag();
        location.putDouble(TAG_LOCATION_X, player.getX());
        location.putDouble(TAG_LOCATION_Y, player.getY());
        location.putDouble(TAG_LOCATION_Z, player.getZ());
        location.putString(TAG_LOCATION_DIMENSION, player.level().dimension().location().toString());
        return location;
    }

    private static ListTag encodeMainInventoryList(final List<ItemStack> stacks) {
        final ListTag inventoryData = new ListTag();
        for (int slot = 0; slot < stacks.size(); slot++) {
            final ItemStack stack = stacks.get(slot);
            if (stack == null || stack.isEmpty()) continue;

            final CompoundTag stackTag = stack.save(new CompoundTag());
            stackTag.putInt(TAG_SLOT, slot);
            inventoryData.add(stackTag);
        }

        return inventoryData;
    }

    private static CompoundTag encodeSubInventories(final Map<String, OBInventoryEvent.SubInventory> subInventories) {
        final CompoundTag encoded = new CompoundTag();
        for (final Map.Entry<String, OBInventoryEvent.SubInventory> entry : subInventories.entrySet()) {
            final String id = entry.getKey();
            final OBInventoryEvent.SubInventory subInventory = entry.getValue();
            if (id == null || id.isBlank() || subInventory == null) continue;
            encoded.put(id, encodeSubInventory(subInventory));
        }
        return encoded;
    }

    private static Map<String, OBInventoryEvent.SubInventory> collectSubInventories(final ServerPlayer player) {
        final Map<String, OBInventoryEvent.SubInventory> result = new LinkedHashMap<>();
        result.put(ID_ARMOR_INVENTORY, createSubInventory(player.getInventory().armor));
        result.put(ID_OFFHAND_INVENTORY, createSubInventory(player.getInventory().offhand));
        result.put(ID_ENDER_CHEST_INVENTORY, createSubInventory(player.getEnderChestInventory()));

        final OBInventoryEvent.Store event = new OBInventoryEvent.Store(player);
        MinecraftForge.EVENT_BUS.post(event);
        for (final Map.Entry<String, OBInventoryEvent.SubInventory> entry : event.getSubInventories().entrySet()) {
            final String id = entry.getKey();
            if (!ID_MAIN_INVENTORY.equalsIgnoreCase(id) && findSubInventory(result, id) == null) result.put(id, entry.getValue());
        }

        return result;
    }

    private static OBInventoryEvent.SubInventory createSubInventory(final List<ItemStack> inventory) {
        final OBInventoryEvent.SubInventory subInventory = new OBInventoryEvent.SubInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            final ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) continue;
            subInventory.addItemStack(slot, stack);
        }

        return subInventory;
    }

    private static OBInventoryEvent.SubInventory createSubInventory(final Container inventory) {
        final OBInventoryEvent.SubInventory subInventory = new OBInventoryEvent.SubInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            subInventory.addItemStack(slot, stack);
        }
        return subInventory;
    }

    private static ListTag encodeSubInventory(final OBInventoryEvent.SubInventory subInventory) {
        final ListTag encoded = new ListTag();
        for (final Map.Entry<Integer, ItemStack> entry : subInventory.asMap().entrySet()) {
            final int slot = entry.getKey();
            if (slot < 0) continue;

            final ItemStack stack = entry.getValue();
            if (stack == null || stack.isEmpty()) continue;

            final CompoundTag stackTag = stack.save(new CompoundTag());
            stackTag.putInt(TAG_SLOT, slot);
            encoded.add(stackTag);
        }
        return encoded;
    }

    private static OBInventoryEvent.SubInventory findSubInventory(
            final Map<String, OBInventoryEvent.SubInventory> inventories, final String requested) {
        final var exact = inventories.get(requested);
        if (exact != null) return exact;
        for (final var entry : inventories.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(requested)) return entry.getValue();
        }
        return null;
    }

    private static void applyIndexedStacks(final List<ItemStack> target, final OBInventoryEvent.SubInventory saved) {
        for (int i = 0; i < target.size(); i++) target.set(i, saved.getItemStack(i));
    }

    private static void applyContainer(final Container target, final OBInventoryEvent.SubInventory saved) {
        for (int i = 0; i < target.getContainerSize(); i++) target.setItem(i, saved.getItemStack(i));
    }

    private static int readSlotIndex(final CompoundTag stackTag) {
        if (stackTag.contains(TAG_SLOT, Tag.TAG_INT)) {
            return stackTag.getInt(TAG_SLOT);
        }
        if (stackTag.contains(TAG_SLOT, Tag.TAG_SHORT)) {
            return stackTag.getShort(TAG_SLOT);
        }
        if (stackTag.contains(TAG_SLOT, Tag.TAG_BYTE)) {
            return stackTag.getByte(TAG_SLOT) & 255;
        }
        return -1;
    }

    private static String stripFilename(final String name) {
        String value = name;
        if (value.startsWith(PREFIX)) value = value.substring(PREFIX.length());
        if (value.endsWith(EXT)) value = value.substring(0, value.length() - EXT.length());
        return value;
    }

    private static String sanitize(final String input) {
        return SAFE_CHARS.matcher(input).replaceAll("_");
    }

    private record IndexedInventory(long size, NavigableMap<Integer, ItemStack> stacks) {
        IndexedInventory section(final int start, final int length) {
            final var section = new TreeMap<Integer, ItemStack>();
            stacks.subMap(start, true, start + length, false).forEach((slot, stack) -> section.put(slot - start, stack));
            return new IndexedInventory(length, section);
        }
    }

    private record Snapshot(IndexedInventory main, Map<String, OBInventoryEvent.SubInventory> subInventories, boolean grave) {}
}
