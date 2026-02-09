package art.arcane.openblocks.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

        final CompoundTag root = new CompoundTag();
        final ListTag inventoryData = player.getInventory().save(new ListTag());
        root.put(TAG_INVENTORY, inventoryData);
        root.putLong(TAG_CREATED, System.currentTimeMillis());
        root.putString(TAG_TYPE, type);
        root.putString(TAG_PLAYER_NAME, player.getGameProfile().getName());
        root.putString(TAG_PLAYER_UUID, player.getUUID().toString());
        root.put(TAG_SUB_INVENTORIES, encodeSubInventories(player));

        Files.createDirectories(dumpPath.getParent());
        NbtIo.writeCompressed(root, dumpPath.toFile());
        return stripFilename(dumpPath.getFileName().toString());
    }

    public static boolean restoreInventory(final ServerPlayer player, final String inventoryId) throws IOException {
        final CompoundTag root = readInventoryTag(player.serverLevel(), inventoryId);
        if (root == null || !root.contains(TAG_INVENTORY, Tag.TAG_LIST)) return false;

        final ListTag inventoryData = root.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
        player.getInventory().load(inventoryData);

        if (root.contains(TAG_SUB_INVENTORIES, Tag.TAG_COMPOUND)) {
            final CompoundTag subInventories = root.getCompound(TAG_SUB_INVENTORIES);
            applyIndexedStacks(player.getInventory().armor, getSubInventory(subInventories, ID_ARMOR_INVENTORY));
            applyIndexedStacks(player.getInventory().offhand, getSubInventory(subInventories, ID_OFFHAND_INVENTORY));
            applyContainer(player.getEnderChestInventory(), getSubInventory(subInventories, ID_ENDER_CHEST_INVENTORY));
            player.getEnderChestInventory().setChanged();
        }

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

    private static CompoundTag encodeSubInventories(final ServerPlayer player) {
        final CompoundTag subInventories = new CompoundTag();
        subInventories.put(ID_ARMOR_INVENTORY, encodeIndexedStacks(player.getInventory().armor));
        subInventories.put(ID_OFFHAND_INVENTORY, encodeIndexedStacks(player.getInventory().offhand));
        subInventories.put(ID_ENDER_CHEST_INVENTORY, encodeContainer(player.getEnderChestInventory()));
        return subInventories;
    }

    private static ListTag encodeIndexedStacks(final List<ItemStack> inventory) {
        final ListTag result = new ListTag();
        for (int slot = 0; slot < inventory.size(); slot++) {
            final ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) continue;

            final CompoundTag stackTag = stack.save(new CompoundTag());
            stackTag.putInt(TAG_SLOT, slot);
            result.add(stackTag);
        }
        return result;
    }

    private static ListTag encodeContainer(final Container inventory) {
        final ListTag result = new ListTag();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;

            final CompoundTag stackTag = stack.save(new CompoundTag());
            stackTag.putInt(TAG_SLOT, slot);
            result.add(stackTag);
        }
        return result;
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
