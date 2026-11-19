package art.arcane.openblocks.command;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBFlagBlock;
import art.arcane.openblocks.item.OBGlyphItem;
import art.arcane.openblocks.item.OBImaginaryItem;
import art.arcane.openblocks.item.OBStencilPattern;
import art.arcane.openblocks.registry.OBMissingMappings;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

final class OBLegacyInventory {
    private static final int LEGACY_DATA_VERSION = 1343;
    // The deprecated Cursor occupied index 8, which remained reserved in 1.12.2.
    private static final List<String> COMPONENTS = List.of("glider_wing", "beam", "crane_engine", "crane_magnet", "miracle_magnet",
            "line", "map_controller", "map_memory", "", "assistant_base", "unprepared_stencil", "sketching_pencil");
    private static final OBStencilPattern[] STENCILS = OBStencilPattern.values();

    private OBLegacyInventory() {}

    static CompoundTag convertStack(final CompoundTag stack) throws IOException {
        final String name = stack.getString("id");
        final ResourceLocation id = ResourceLocation.tryParse(name);
        if (stack.getInt("Count") <= 0 || stack.getInt("Count") > Byte.MAX_VALUE) {
            throw unsupported(name, "invalid item count");
        }
        if (id != null && id.getNamespace().equals("minecraft")) {
            return convertVanilla(stack, id);
        }
        final ResourceLocation target = OBMissingMappings.legacyItemDestination(name);
        if (target == null) throw unsupported(name, "unknown legacy item ID");
        return convertOpenBlocks(stack, name, target);
    }

    private static CompoundTag convertVanilla(final CompoundTag stack, final ResourceLocation id) throws IOException {
        final var source = stack.copy();
        final var blockData = source.getCompound("tag").getCompound("BlockEntityTag");
        // The vanilla fixer would try to flatten OpenBlocks IDs in shulker slots as vanilla items.
        final Tag contents = id.getPath().endsWith("shulker_box") ? blockData.get("Items") : null;
        if (contents != null) blockData.remove("Items");
        verifyNestedStacks(source, true);
        final var output = fixVanilla(source, stack.getString("id"));
        if (contents != null) {
            final var data = output.getCompound("tag");
            final var convertedBlockData = data.getCompound("BlockEntityTag");
            convertedBlockData.put("Items", convertSlotList(contents, 27));
            data.put("BlockEntityTag", convertedBlockData);
            output.put("tag", data);
        }
        return output;
    }

    private static CompoundTag convertOpenBlocks(final CompoundTag stack, final String name, ResourceLocation target) throws IOException {
        int metadata = stack.getInt("Damage");
        if (metadata < 0) throw unsupported(name, "invalid item metadata");
        if (target.getPath().equals("generic")) {
            if (metadata >= COMPONENTS.size() || COMPONENTS.get(metadata).isEmpty()) throw unsupported(name, "unknown component " + metadata);
            target = OpenBlocks.id(COMPONENTS.get(metadata));
            metadata = 0;
        } else if (target.getPath().equals("generic_unstackable")) {
            if (metadata != 0) throw unsupported(name, "unknown unstackable component " + metadata);
            target = OpenBlocks.id("pointer");
        }
        if (!ForgeRegistries.ITEMS.containsKey(target)) throw unsupported(name, "no registered replacement");
        if (stack.contains("ForgeCaps") && (!(stack.get("ForgeCaps") instanceof CompoundTag caps) || !caps.isEmpty())) {
            throw unsupported(name, "contains legacy Forge capabilities");
        }
        if (stack.contains("tag") && !stack.contains("tag", Tag.TAG_COMPOUND)) throw unsupported(name, "invalid item tags");
        if (target.getPath().equals("height_map")) throw unsupported(name, "survey references old world data");

        final var source = stack.copy();
        source.putString("id", target.toString());
        final var inputData = source.getCompound("tag");
        final String path = target.getPath();
        final String storedKey = switch (path) {
            case "luggage" -> "Items";
            case "dev_null" -> "inventory";
            case "cartographer" -> "MapItem";
            default -> null;
        };
        if ((storedKey != null || path.equals("tank")) && metadata != 0) throw unsupported(name, "unexpected metadata " + metadata);
        final Tag stored = storedKey == null ? null : inputData.get(storedKey);
        if (storedKey != null) inputData.remove(storedKey);
        if (inputData.contains("BlockEntityTag")) throw unsupported(name, "contains legacy block entity data");
        verifyNestedStacks(inputData, false);
        final var output = fixVanilla(source, name);
        final var data = output.getCompound("tag");
        switch (path) {
            case "elevator", "elevator_rotating", "flag" -> {
                if (metadata >= 16) throw unsupported(name, "unknown dye color " + metadata);
                final DyeColor color = DyeColor.byId(metadata);
                if (color != (path.equals("flag") ? OBFlagBlock.DEFAULT_COLOR : DyeColor.WHITE)) {
                    putBlockState(data, "color", color.getName());
                }
            }
            case "sky" -> {
                if (metadata > 1) throw unsupported(name, "unknown Sky Block subtype " + metadata);
                if (metadata == 1) putBlockState(data, "inverted", "true");
            }
            case "stencil" -> {
                if (metadata >= STENCILS.length) throw unsupported(name, "unknown stencil " + metadata);
                data.putString("Pattern", STENCILS[metadata].getSerializedName());
            }
            case "glyph" -> {
                if (!OBGlyphItem.available(metadata)) throw unsupported(name, "unknown glyph " + metadata);
                data.putInt("CharIndex", metadata);
            }
            case "imaginary" -> {
                final int mode = data.getInt("Mode");
                final float uses = data.getFloat("Uses");
                if (metadata > 1 || (metadata == 1) != data.contains("Color", Tag.TAG_ANY_NUMERIC)
                        || mode < 0 || mode >= OBImaginaryItem.Mode.values().length || !Float.isFinite(uses) || uses < 0) {
                    throw unsupported(name, "invalid drawing tool data");
                }
                data.putFloat("Uses", uses);
            }
            case "tank" -> convertTank(name, data);
            case "luggage" -> {
                if (stored != null) convertLuggage(name, data, stored);
            }
            case "dev_null" -> {
                if (stored != null) convertDevNull(name, data, stored);
            }
            case "cartographer" -> {
                if (stored != null) {
                    if (!(stored instanceof CompoundTag map)) throw unsupported(name, "invalid packed map");
                    data.put("MapItem", convertStack(map));
                }
            }
            default -> {
                if (metadata != 0) {
                    final var item = new ItemStack(ForgeRegistries.ITEMS.getValue(target));
                    if (item.getMaxDamage() <= 0) throw unsupported(name, "unexpected metadata " + metadata);
                    data.putInt("Damage", metadata);
                }
            }
        }
        if (!data.isEmpty()) output.put("tag", data);
        return output;
    }

    private static void putBlockState(final CompoundTag data, final String property, final String value) {
        final var state = data.getCompound(BlockItem.BLOCK_STATE_TAG);
        state.putString(property, value);
        data.put(BlockItem.BLOCK_STATE_TAG, state);
    }

    private static void convertTank(final String name, final CompoundTag data) throws IOException {
        if (!data.contains("tank")) return;
        if (!(data.get("tank") instanceof CompoundTag tank)) throw unsupported(name, "invalid fluid tank");
        if (tank.isEmpty() || tank.getBoolean("Empty")) {
            data.remove("tank");
            return;
        }
        final String fluidName = tank.getString("FluidName");
        final ResourceLocation fluid = switch (fluidName) {
            case "xpjuice", "liquidxp", "openblocks:xpjuice", "openblocks:liquidxp", "open_blocks:liquidxp" -> OpenBlocks.id("xpjuice");
            default -> ResourceLocation.tryParse(fluidName);
        };
        if (fluid == null || !ForgeRegistries.FLUIDS.containsKey(fluid) || ForgeRegistries.FLUIDS.getValue(fluid) == Fluids.EMPTY
                || tank.getInt("Amount") <= 0) throw unsupported(name, "unknown or invalid fluid '" + fluidName + "'");
        tank.putString("FluidName", fluid.toString());
    }

    private static void convertLuggage(final String name, final CompoundTag data, final Tag stored) throws IOException {
        final int size = data.getInt("size");
        if (size != 27 && size != 54) throw unsupported(name, "unknown luggage size " + size);
        data.put("Items", convertSlotList(stored, size));
    }

    private static ListTag convertSlotList(final Tag stored, final int size) throws IOException {
        final var contents = OBInventoryStore.decodeStacks(OBInventoryStore.requireItemList(stored), true, false, size);
        final var items = new ListTag();
        contents.forEach((slot, item) -> {
            final var entry = item.save(new CompoundTag());
            entry.putByte("Slot", slot.byteValue());
            items.add(entry);
        });
        return items;
    }

    private static void convertDevNull(final String name, final CompoundTag data, final Tag stored) throws IOException {
        if (!(stored instanceof CompoundTag inventory) || inventory.getInt("size") != 1) {
            throw unsupported(name, "invalid /dev/null inventory");
        }
        final var contents = OBInventoryStore.decodeStacks(OBInventoryStore.requireItemList(inventory.get("Items")), true, false, 1);
        final var item = contents.get(0);
        if (item != null) data.put("Contents", item.save(new CompoundTag()));
    }

    private static CompoundTag fixVanilla(final CompoundTag stack, final String name) throws IOException {
        final Tag updated = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, stack),
                LEGACY_DATA_VERSION, SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
        if (!(updated instanceof CompoundTag result)) throw unsupported(name, "item data fixer returned an invalid stack");
        verifyEnchantments(result, name);
        return result;
    }

    private static void verifyNestedStacks(final Tag data, final boolean allowVanilla) throws IOException {
        if (data instanceof CompoundTag tag) {
            if (tag.contains("id", Tag.TAG_STRING) && tag.contains("Count", Tag.TAG_ANY_NUMERIC) && tag.getInt("Count") > 0) {
                final ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
                if (!allowVanilla || id == null || !id.getNamespace().equals("minecraft")) {
                    throw unsupported(tag.getString("id"), "item is in an unsupported nested inventory");
                }
            }
            for (final String key : tag.getAllKeys()) verifyNestedStacks(tag.get(key), allowVanilla);
        } else if (data instanceof ListTag list) {
            for (final Tag entry : list) verifyNestedStacks(entry, allowVanilla);
        }
    }

    private static void verifyEnchantments(final Tag data, final String item) throws IOException {
        if (data instanceof CompoundTag tag) {
            for (final String key : tag.getAllKeys()) {
                final Tag value = tag.get(key);
                if (key.equals("Enchantments") || key.equals("StoredEnchantments")) {
                    for (final Tag entry : OBInventoryStore.requireItemList(value)) {
                        final String name = ((CompoundTag) entry).getString("id");
                        final ResourceLocation id = ResourceLocation.tryParse(name);
                        if (id == null || !ForgeRegistries.ENCHANTMENTS.containsKey(id)) throw unsupported(item, "unknown enchantment '" + name + "'");
                    }
                } else {
                    verifyEnchantments(value, item);
                }
            }
        } else if (data instanceof ListTag list) {
            for (final Tag entry : list) verifyEnchantments(entry, item);
        }
    }

    private static IOException unsupported(final String name, final String reason) {
        return new IOException("Could not convert legacy item '" + name + "': " + reason);
    }
}
