package art.arcane.openblocks.command;

import art.arcane.openblocks.api.OBInventoryEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
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

    private static final int MAIN_SLOTS = 36;
    private static final int ARMOR_SLOT_BASE = 100;
    private static final int ARMOR_SLOT_COUNT = 4;
    private static final int OFFHAND_SLOT = 150;
    private static final int OFFHAND_SLOT_COUNT = 1;

    private static final Pattern SAFE_CHARS = Pattern.compile("[^A-Za-z0-9_-]");
    private static final ThreadLocal<SimpleDateFormat> FILE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.ROOT));

    private OBInventoryStore() {}

    public static String storePlayerInventory(final ServerPlayer player, final String type) throws IOException {
        final String safePlayerName = sanitize(player.getGameProfile().getName());
        final Path dumpPath = createDumpPath(player.serverLevel(), safePlayerName, sanitize(type));
        final Map<String, OBInventoryEvent.SubInventory> subInventories = collectSubInventories(player);

        final CompoundTag root = new CompoundTag();
        final ListTag inventoryData = player.getInventory().save(new ListTag());
        root.put(TAG_INVENTORY, inventoryData);
        root.putLong(TAG_CREATED, System.currentTimeMillis());
        root.putString(TAG_TYPE, type);
        root.putString(TAG_PLAYER_NAME, player.getGameProfile().getName());
        root.putString(TAG_PLAYER_UUID, player.getUUID().toString());
        root.put(TAG_LOCATION, encodePlayerLocation(player));
        root.put(TAG_SUB_INVENTORIES, encodeSubInventories(subInventories));

        Files.createDirectories(dumpPath.getParent());
        NbtIo.writeCompressed(root, dumpPath.toFile());
        return stripFilename(dumpPath.getFileName().toString());
    }

    public static String storeDroppedItems(final ServerPlayer player, final String type, final List<ItemStack> drops)
            throws IOException {
        final String safePlayerName = sanitize(player.getGameProfile().getName());
        final Path dumpPath = createDumpPath(player.serverLevel(), safePlayerName, sanitize(type));

        final CompoundTag root = new CompoundTag();
        root.put(TAG_INVENTORY, encodeMainInventoryList(drops));
        root.putLong(TAG_CREATED, System.currentTimeMillis());
        root.putString(TAG_TYPE, type);
        root.putString(TAG_PLAYER_NAME, player.getGameProfile().getName());
        root.putString(TAG_PLAYER_UUID, player.getUUID().toString());
        root.put(TAG_LOCATION, encodePlayerLocation(player));
        root.put(TAG_SUB_INVENTORIES, new CompoundTag());

        Files.createDirectories(dumpPath.getParent());
        NbtIo.writeCompressed(root, dumpPath.toFile());
        return stripFilename(dumpPath.getFileName().toString());
    }

    public static boolean restoreInventory(final ServerPlayer player, final String inventoryId) throws IOException {
        final CompoundTag root = readInventoryTag(player.serverLevel(), inventoryId);
        if (root == null || !root.contains(TAG_INVENTORY, Tag.TAG_LIST)) return false;

        final ListTag inventoryData = root.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
        player.getInventory().load(inventoryData);
        final CompoundTag encodedSubInventories = root.contains(TAG_SUB_INVENTORIES, Tag.TAG_COMPOUND)
                ? root.getCompound(TAG_SUB_INVENTORIES)
                : new CompoundTag();
        final Map<String, OBInventoryEvent.SubInventory> subInventories = decodeSubInventories(encodedSubInventories);

        final ListTag armorSubInventory = findSubInventory(encodedSubInventories, ID_ARMOR_INVENTORY);
        if (armorSubInventory != null) applyIndexedStacks(player.getInventory().armor, armorSubInventory);

        final ListTag offhandSubInventory = findSubInventory(encodedSubInventories, ID_OFFHAND_INVENTORY);
        if (offhandSubInventory != null) applyIndexedStacks(player.getInventory().offhand, offhandSubInventory);

        final ListTag enderChestSubInventory = findSubInventory(encodedSubInventories, ID_ENDER_CHEST_INVENTORY);
        if (enderChestSubInventory != null) {
            applyContainer(player.getEnderChestInventory(), enderChestSubInventory);
            player.getEnderChestInventory().setChanged();
        }

        MinecraftForge.EVENT_BUS.post(new OBInventoryEvent.Load(player, subInventories));

        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        return true;
    }

    public static List<ItemStack> getSpawnStacks(
            final ServerLevel level,
            final String inventoryId,
            final String target,
            final Integer slotIndex) throws IOException {
        final CompoundTag root = readInventoryTag(level, inventoryId);
        if (root == null || !root.contains(TAG_INVENTORY, Tag.TAG_LIST)) return List.of();

        final ListTag inventoryData = root.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
        final MainInventorySections mainSections = decodeMainInventorySections(inventoryData);
        final CompoundTag subInventories = root.contains(TAG_SUB_INVENTORIES, Tag.TAG_COMPOUND)
                ? root.getCompound(TAG_SUB_INVENTORIES)
                : new CompoundTag();
        final String normalizedTarget = target.toLowerCase(Locale.ROOT);

        final List<ItemStack> inventory = switch (normalizedTarget) {
            case ID_MAIN_INVENTORY -> mainSections.main();
            case ID_ARMOR_INVENTORY -> {
                final String key = resolveSubInventoryKey(subInventories, ID_ARMOR_INVENTORY);
                if (key != null) yield decodeIndexedStacks(getSubInventory(subInventories, key), ARMOR_SLOT_COUNT);
                yield mainSections.armor();
            }
            case ID_OFFHAND_INVENTORY -> {
                final String key = resolveSubInventoryKey(subInventories, ID_OFFHAND_INVENTORY);
                if (key != null) yield decodeIndexedStacks(getSubInventory(subInventories, key), OFFHAND_SLOT_COUNT);
                yield mainSections.offhand();
            }
            case ID_ENDER_CHEST_INVENTORY -> {
                final String key = resolveSubInventoryKey(subInventories, ID_ENDER_CHEST_INVENTORY);
                if (key == null) throw new IllegalArgumentException("Unsupported sub inventory: " + target);
                yield decodeIndexedStacks(getSubInventory(subInventories, key), 27);
            }
            default -> {
                final String key = resolveSubInventoryKey(subInventories, normalizedTarget);
                if (key == null) throw new IllegalArgumentException("Unsupported sub inventory: " + target);
                yield decodeIndexedStacks(getSubInventory(subInventories, key), 0);
            }
        };

        if (slotIndex != null) {
            if (slotIndex < 0 || slotIndex >= inventory.size()) throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
            final ItemStack stack = inventory.get(slotIndex);
            if (stack.isEmpty()) return List.of();
            return List.of(stack.copy());
        }

        final List<ItemStack> result = new ArrayList<>();
        for (final ItemStack stack : inventory) {
            if (!stack.isEmpty()) result.add(stack.copy());
        }

        return result;
    }

    public static List<String> getAvailableTargets(final ServerLevel level, final String inventoryId) {
        try {
            final CompoundTag root = readInventoryTag(level, inventoryId);
            if (root == null || !root.contains(TAG_INVENTORY, Tag.TAG_LIST)) return List.of();

            final Set<String> targets = new LinkedHashSet<>();
            targets.add(ID_MAIN_INVENTORY);
            targets.add(ID_ARMOR_INVENTORY);
            targets.add(ID_OFFHAND_INVENTORY);
            targets.add(ID_ENDER_CHEST_INVENTORY);

            if (root.contains(TAG_SUB_INVENTORIES, Tag.TAG_COMPOUND)) {
                final CompoundTag subInventories = root.getCompound(TAG_SUB_INVENTORIES);
                targets.addAll(subInventories.getAllKeys());
            }

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
        final String id = sanitize(stripFilename(inventoryId));
        return getSaveFolder(level).resolve(PREFIX + id + EXT);
    }

    public static List<ItemStack> readDroppedItems(final ServerLevel level, final String inventoryId) throws IOException {
        final CompoundTag root = readInventoryTag(level, inventoryId);
        if (root == null || !root.contains(TAG_INVENTORY, Tag.TAG_LIST)) return List.of();

        final ListTag inventoryData = root.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
        final List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < inventoryData.size(); i++) {
            final ItemStack stack = ItemStack.of(inventoryData.getCompound(i));
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    public static boolean deleteDump(final ServerLevel level, final String inventoryId) throws IOException {
        return Files.deleteIfExists(resolveDumpPath(level, inventoryId));
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

    private static Map<String, OBInventoryEvent.SubInventory> decodeSubInventories(final CompoundTag subInventoriesTag) {
        final Map<String, OBInventoryEvent.SubInventory> subInventories = new LinkedHashMap<>();
        for (final String key : subInventoriesTag.getAllKeys()) {
            if (!subInventoriesTag.contains(key, Tag.TAG_LIST)) continue;
            final ListTag encoded = subInventoriesTag.getList(key, Tag.TAG_COMPOUND);
            subInventories.put(key, decodeSubInventory(encoded));
        }
        return subInventories;
    }

    private static Map<String, OBInventoryEvent.SubInventory> collectSubInventories(final ServerPlayer player) {
        final Map<String, OBInventoryEvent.SubInventory> result = new LinkedHashMap<>();
        result.put(ID_ARMOR_INVENTORY, createSubInventory(player.getInventory().armor));
        result.put(ID_OFFHAND_INVENTORY, createSubInventory(player.getInventory().offhand));
        result.put(ID_ENDER_CHEST_INVENTORY, createSubInventory(player.getEnderChestInventory()));

        final OBInventoryEvent.Store event = new OBInventoryEvent.Store(player);
        MinecraftForge.EVENT_BUS.post(event);
        for (final Map.Entry<String, OBInventoryEvent.SubInventory> entry : event.getSubInventories().entrySet()) {
            result.putIfAbsent(entry.getKey(), entry.getValue());
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

    private static OBInventoryEvent.SubInventory decodeSubInventory(final ListTag encodedSubInventory) {
        final OBInventoryEvent.SubInventory subInventory = new OBInventoryEvent.SubInventory();
        for (int i = 0; i < encodedSubInventory.size(); i++) {
            final CompoundTag stackTag = encodedSubInventory.getCompound(i);
            final int slot = readSlotIndex(stackTag);
            if (slot < 0) continue;

            final ItemStack stack = ItemStack.of(stackTag);
            if (stack.isEmpty()) continue;

            subInventory.addItemStack(slot, stack);
        }
        return subInventory;
    }

    private static ListTag findSubInventory(final CompoundTag subInventories, final String key) {
        final String resolved = resolveSubInventoryKey(subInventories, key);
        if (resolved == null) return null;
        return subInventories.getList(resolved, Tag.TAG_COMPOUND);
    }

    private static void applyIndexedStacks(final List<ItemStack> target, final ListTag inventoryData) {
        for (int i = 0; i < target.size(); i++) {
            target.set(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < inventoryData.size(); i++) {
            final CompoundTag stackTag = inventoryData.getCompound(i);
            final int slot = readSlotIndex(stackTag);
            if (slot < 0 || slot >= target.size()) continue;

            final ItemStack stack = ItemStack.of(stackTag);
            if (!stack.isEmpty()) target.set(slot, stack);
        }
    }

    private static void applyContainer(final Container target, final ListTag inventoryData) {
        for (int i = 0; i < target.getContainerSize(); i++) {
            target.setItem(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < inventoryData.size(); i++) {
            final CompoundTag stackTag = inventoryData.getCompound(i);
            final int slot = readSlotIndex(stackTag);
            if (slot < 0 || slot >= target.getContainerSize()) continue;

            final ItemStack stack = ItemStack.of(stackTag);
            if (!stack.isEmpty()) target.setItem(slot, stack);
        }
    }

    private static ListTag getSubInventory(final CompoundTag subInventories, final String key) {
        if (!subInventories.contains(key, Tag.TAG_LIST)) return new ListTag();
        return subInventories.getList(key, Tag.TAG_COMPOUND);
    }

    private static String resolveSubInventoryKey(final CompoundTag subInventories, final String requested) {
        for (final String key : subInventories.getAllKeys()) {
            if (key.equalsIgnoreCase(requested)) return key;
        }
        return null;
    }

    private static MainInventorySections decodeMainInventorySections(final ListTag inventoryData) {
        final List<ItemStack> main = emptySlots(MAIN_SLOTS);
        final List<ItemStack> armor = emptySlots(ARMOR_SLOT_COUNT);
        final List<ItemStack> offhand = emptySlots(OFFHAND_SLOT_COUNT);

        for (int i = 0; i < inventoryData.size(); i++) {
            final CompoundTag stackTag = inventoryData.getCompound(i);
            final ItemStack stack = ItemStack.of(stackTag);
            if (stack.isEmpty()) continue;

            final int encodedSlot = stackTag.getByte(TAG_SLOT) & 255;
            if (encodedSlot >= 0 && encodedSlot < MAIN_SLOTS) {
                main.set(encodedSlot, stack);
                continue;
            }

            if (encodedSlot >= ARMOR_SLOT_BASE && encodedSlot < ARMOR_SLOT_BASE + ARMOR_SLOT_COUNT) {
                armor.set(encodedSlot - ARMOR_SLOT_BASE, stack);
                continue;
            }

            if (encodedSlot == OFFHAND_SLOT) offhand.set(0, stack);
        }

        return new MainInventorySections(main, armor, offhand);
    }

    private static List<ItemStack> decodeIndexedStacks(final ListTag inventoryData, final int minimumSize) {
        int size = Math.max(minimumSize, 0);
        for (int i = 0; i < inventoryData.size(); i++) {
            final int slot = readSlotIndex(inventoryData.getCompound(i));
            if (slot >= 0) size = Math.max(size, slot + 1);
        }

        final List<ItemStack> inventory = emptySlots(size);
        for (int i = 0; i < inventoryData.size(); i++) {
            final CompoundTag stackTag = inventoryData.getCompound(i);
            final ItemStack stack = ItemStack.of(stackTag);
            if (stack.isEmpty()) continue;

            final int slot = readSlotIndex(stackTag);
            if (slot >= 0 && slot < inventory.size()) inventory.set(slot, stack);
        }

        return inventory;
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

    private static List<ItemStack> emptySlots(final int size) {
        final List<ItemStack> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(ItemStack.EMPTY);
        }
        return result;
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

    private record MainInventorySections(List<ItemStack> main, List<ItemStack> armor, List<ItemStack> offhand) {}
}
