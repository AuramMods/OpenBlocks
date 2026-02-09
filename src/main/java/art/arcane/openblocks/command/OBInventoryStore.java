package art.arcane.openblocks.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

public final class OBInventoryStore {

    private static final String PREFIX = "inventory-";
    private static final String EXT = ".dat";
    private static final String ID_MAIN_INVENTORY = "main";

    private static final String TAG_CREATED = "Created";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_PLAYER_NAME = "PlayerName";
    private static final String TAG_PLAYER_UUID = "PlayerUUID";
    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_SLOT = "Slot";

    private static final int MAIN_SLOTS = 36;
    private static final int ARMOR_SLOT_BASE = 100;
    private static final int ARMOR_SLOT_COUNT = 4;
    private static final int OFFHAND_SLOT = 150;
    private static final int TOTAL_SLOT_COUNT = 41;

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

        Files.createDirectories(dumpPath.getParent());
        NbtIo.writeCompressed(root, dumpPath.toFile());
        return stripFilename(dumpPath.getFileName().toString());
    }

    public static boolean restoreInventory(final ServerPlayer player, final String inventoryId) throws IOException {
        final CompoundTag root = readInventoryTag(player.serverLevel(), inventoryId);
        if (root == null || !root.contains(TAG_INVENTORY, Tag.TAG_LIST)) return false;

        final ListTag inventoryData = root.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
        player.getInventory().load(inventoryData);
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        return true;
    }

    public static List<ItemStack> getSpawnStacks(
            final ServerLevel level,
            final String inventoryId,
            final String target,
            final Integer slotIndex) throws IOException {
        if (!ID_MAIN_INVENTORY.equalsIgnoreCase(target)) {
            throw new IllegalArgumentException("Unsupported sub inventory: " + target);
        }

        final CompoundTag root = readInventoryTag(level, inventoryId);
        if (root == null || !root.contains(TAG_INVENTORY, Tag.TAG_LIST)) return List.of();

        final ListTag inventoryData = root.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
        final List<ItemStack> inventory = decodeInventory(inventoryData);

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
            if (root != null && root.contains(TAG_INVENTORY, Tag.TAG_LIST)) return List.of(ID_MAIN_INVENTORY);
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

    private static List<ItemStack> decodeInventory(final ListTag inventoryData) {
        final List<ItemStack> slots = new ArrayList<>(TOTAL_SLOT_COUNT);
        for (int i = 0; i < TOTAL_SLOT_COUNT; i++) {
            slots.add(ItemStack.EMPTY);
        }

        for (int i = 0; i < inventoryData.size(); i++) {
            final CompoundTag stackTag = inventoryData.getCompound(i);
            final ItemStack stack = ItemStack.of(stackTag);
            if (stack.isEmpty()) continue;

            final int encodedSlot = stackTag.getByte(TAG_SLOT) & 255;
            final int decodedSlot = decodeSlot(encodedSlot);
            if (decodedSlot >= 0 && decodedSlot < TOTAL_SLOT_COUNT) slots.set(decodedSlot, stack);
        }

        return slots;
    }

    private static int decodeSlot(final int encodedSlot) {
        if (encodedSlot >= 0 && encodedSlot < MAIN_SLOTS) return encodedSlot;
        if (encodedSlot >= ARMOR_SLOT_BASE && encodedSlot < ARMOR_SLOT_BASE + ARMOR_SLOT_COUNT) {
            return MAIN_SLOTS + (encodedSlot - ARMOR_SLOT_BASE);
        }
        if (encodedSlot == OFFHAND_SLOT) return MAIN_SLOTS + ARMOR_SLOT_COUNT;
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
}
