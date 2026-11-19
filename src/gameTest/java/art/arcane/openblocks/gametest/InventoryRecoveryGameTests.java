package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.api.OBInventoryEvent;
import art.arcane.openblocks.command.OBInventoryStore;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBDevNullItem;
import art.arcane.openblocks.item.OBElevatorItem;
import art.arcane.openblocks.item.OBGlyphItem;
import art.arcane.openblocks.item.OBImaginaryItem;
import art.arcane.openblocks.item.OBPaintBrushItem;
import art.arcane.openblocks.item.OBSkyItem;
import art.arcane.openblocks.item.OBStencilItem;
import art.arcane.openblocks.item.OBStencilPattern;
import art.arcane.openblocks.item.OBTankItem;
import art.arcane.openblocks.registry.OBFluids;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class InventoryRecoveryGameTests {

    @GameTest(template = "empty")
    public static void inventoryBackupRestoresAllSectionsAndSparseExtensionPayloads(final GameTestHelper helper) throws IOException {
        final var level = helper.getLevel();
        final var source = player(helper, "InventorySource");
        final var receiver = player(helper, "InventoryReceiver");
        final var helmet = new ItemStack(Items.IRON_HELMET);
        helmet.setHoverName(Component.literal("Recovery helm"));
        helmet.setDamageValue(17);
        helmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 3);
        source.getInventory().setItem(8, new ItemStack(Items.DIAMOND, 13));
        source.getInventory().armor.set(3, helmet);
        source.getInventory().offhand.set(0, new ItemStack(Items.SHIELD));
        source.getEnderChestInventory().setItem(26, new ItemStack(Items.GOLD_INGOT, 7));
        receiver.getInventory().setItem(0, new ItemStack(Items.DIRT));
        receiver.getEnderChestInventory().setItem(0, new ItemStack(Items.EMERALD));

        final var accessorySlot = new ItemStackHandler(1);
        final Consumer<OBInventoryEvent.Store> store = event -> {
            if (event.getEntity() == source) event.createSubInventory("qa:accessories")
                    .addItemStack(Integer.MAX_VALUE, new ItemStack(Items.AMETHYST_SHARD, 9));
        };
        final Consumer<OBInventoryEvent.Load> load = event -> {
            if (event.getEntity() == receiver) accessorySlot.setStackInSlot(0, event.getSubInventory("qa:accessories").getItemStack(Integer.MAX_VALUE));
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OBInventoryEvent.Store.class, store);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OBInventoryEvent.Load.class, load);
        String id = null;
        try {
            id = OBInventoryStore.storePlayerInventory(source, "command");
            final String fullName = OBInventoryStore.resolveDumpPath(level, id).getFileName().toString();
            helper.assertTrue(OBInventoryStore.getMatchedDumps(level, "InventorySource").contains(id)
                            && OBInventoryStore.getAvailableTargets(level, fullName).contains("qa:accessories"),
                    "The real file and extension target must be discoverable by completion");
            final var main = OBInventoryStore.getSpawnStacks(level, id, "main", null);
            helper.assertTrue(main.size() == 3 && main.get(0).getCount() == 13 && main.get(1).is(Items.IRON_HELMET) && main.get(2).is(Items.SHIELD),
                    "The logical main inventory must include armor and offhand");
            final var precise = OBInventoryStore.getSpawnStacks(level, id, "qa:accessories", Integer.MAX_VALUE);
            helper.assertTrue(precise.size() == 1 && precise.get(0).getCount() == 9,
                    "The largest valid custom slot index must remain addressable without allocating empty slots");
            final var commands = level.getServer().getCommands();
            final var issuer = level.getServer().createCommandSourceStack().withPosition(source.position()).withSuppressedOutput();
            final String spawn = "ob_inventory spawn " + id + " ";
            final var dispatcher = commands.getDispatcher();
            final var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(spawn, issuer)).join().getList();
            helper.assertTrue(suggestions.stream().anyMatch(option -> option.getText().equals("\"qa:accessories\""))
                            && commands.performPrefixedCommand(issuer, spawn + "\"qa:accessories\" " + Integer.MAX_VALUE) == 1,
                    "Tab completion must quote namespaced extension targets and the actual command must accept them");
            final var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(source.blockPosition()).inflate(1));
            helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(Items.AMETHYST_SHARD) && drops.get(0).getItem().getCount() == 9,
                    "The requested extension slot must spawn its exact item stack");
            drops.forEach(Entity::discard);
            helper.assertTrue(OBInventoryStore.restoreInventory(receiver, fullName)
                            && receiver.getInventory().getItem(0).isEmpty() && receiver.getInventory().getItem(8).getCount() == 13
                            && ItemStack.isSameItemSameTags(receiver.getInventory().armor.get(3), helmet)
                            && receiver.getInventory().offhand.get(0).is(Items.SHIELD)
                            && receiver.getEnderChestInventory().getItem(0).isEmpty() && receiver.getEnderChestInventory().getItem(26).getCount() == 7
                            && accessorySlot.getStackInSlot(0).is(Items.AMETHYST_SHARD) && accessorySlot.getStackInSlot(0).getCount() == 9,
                    "Restore must synchronize ordinary slots and deliver the exact extension data to its consumer");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(store);
            MinecraftForge.EVENT_BUS.unregister(load);
            if (id != null) OBInventoryStore.deleteDump(level, id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventorySpawnCommandRollsBackWhenForgeRejectsAStack(final GameTestHelper helper) throws IOException {
        final var level = helper.getLevel();
        final var player = player(helper, "InventorySpawn");
        player.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 7));
        player.getInventory().offhand.set(0, new ItemStack(Items.DIAMOND, 13));
        final String id = OBInventoryStore.storePlayerInventory(player, "command");
        final var server = level.getServer();
        final var source = server.createCommandSourceStack().withPosition(player.position()).withSuppressedOutput();
        final AABB dropArea = new AABB(player.blockPosition()).inflate(1);
        final Consumer<EntityJoinLevelEvent> deny = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof ItemEntity item
                    && item.getItem().is(Items.DIAMOND) && dropArea.contains(item.position())) event.setCanceled(true);
        };
        try {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, deny);
            try {
                final int result = server.getCommands().performPrefixedCommand(source, "ob_inventory spawn " + id);
                helper.assertTrue(result == 0 && level.getEntitiesOfClass(ItemEntity.class, dropArea).isEmpty(),
                        "A later canceled stack must make the actual command fail and roll back the earlier accepted item");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(deny);
            }

            final int result = server.getCommands().performPrefixedCommand(source, "ob_inventory spawn " + id + " main 40");
            final var drops = level.getEntitiesOfClass(ItemEntity.class, dropArea);
            helper.assertTrue(result == 1 && drops.size() == 1 && drops.get(0).getItem().is(Items.DIAMOND)
                            && drops.get(0).getItem().getCount() == 13 && OBInventoryStore.resolveDumpPath(level, id).toFile().isFile(),
                    "Logical slot 40 must spawn the full saved offhand stack once and leave the backup available");
            drops.forEach(Entity::discard);
        } finally {
            OBInventoryStore.deleteDump(level, id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void importsLegacyVanillaInventoryWithoutSilentlyDiscardingItems(final GameTestHelper helper) throws IOException {
        final var level = helper.getLevel();
        final var player = player(helper, "LegacyInventory");
        final String id = "LegacyInventory-2018-04-05_10.20.30-command-0";
        final var path = OBInventoryStore.resolveDumpPath(level, id);
        final var entries = new ListTag();
        entries.add(legacyItem(8, "minecraft:wool", 14, 6));
        final var sword = legacyItem(0, "minecraft:iron_sword", 25, 1);
        final var enchantment = new CompoundTag();
        enchantment.putShort("id", (short) 16);
        enchantment.putShort("lvl", (short) 2);
        final var enchantments = new ListTag();
        enchantments.add(enchantment);
        final var swordData = new CompoundTag();
        swordData.put("ench", enchantments);
        final var display = new CompoundTag();
        display.putString("Name", "Legacy blade");
        swordData.put("display", display);
        sword.put("tag", swordData);
        entries.add(sword);
        entries.add(legacyItem(40, "minecraft:shield", 0, 1));
        final var inventory = new CompoundTag();
        inventory.putInt("size", 41);
        inventory.put("Items", entries);
        final var root = new CompoundTag();
        root.putString("Type", "command");
        root.put("Inventory", inventory);
        java.nio.file.Files.createDirectories(path.getParent());
        try {
            NbtIo.writeCompressed(root, path.toFile());
            helper.assertTrue(OBInventoryStore.restoreInventory(player, id), "A 1.12.2 OpenModsLib backup of vanilla items must restore");
            final var restoredSword = player.getInventory().getItem(0);
            helper.assertTrue(restoredSword.is(Items.IRON_SWORD) && restoredSword.getDamageValue() == 25
                            && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, restoredSword) == 2
                            && restoredSword.getHoverName().getString().equals("Legacy blade")
                            && player.getInventory().getItem(8).is(Items.RED_WOOL) && player.getInventory().getItem(8).getCount() == 6
                            && player.getInventory().offhand.get(0).is(Items.SHIELD),
                    "Vanilla data fixes must preserve the sword's metadata, colored wool, and the original offhand slot");

            entries.add(legacyItem(9, "openblocks:generic", 8, 1));
            NbtIo.writeCompressed(root, path.toFile());
            boolean rejected = false;
            try {
                OBInventoryStore.restoreInventory(player, id);
            } catch (final IOException expected) {
                rejected = expected.getMessage().contains("openblocks:generic");
            }
            helper.assertTrue(rejected && player.getInventory().getItem(0) == restoredSword && player.getInventory().getItem(9).isEmpty(),
                    "Unsupported legacy mod data must report its item and leave the destination inventory intact");

            final var malformed = new ListTag();
            malformed.add(StringTag.valueOf("not an item"));
            root.put("Inventory", malformed);
            NbtIo.writeCompressed(root, path.toFile());
            try {
                OBInventoryStore.restoreInventory(player, id);
                helper.fail("A list of the wrong NBT type must never clear the player inventory");
            } catch (final IOException expected) {
                helper.assertTrue(player.getInventory().getItem(0) == restoredSword, "Malformed data must leave existing items untouched");
            }
        } finally {
            OBInventoryStore.deleteDump(level, id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyOpenBlocksItemsKeepVariantsDamageAndNestedContents(final GameTestHelper helper) throws Exception {
        final var player = player(helper, "LegacyOpenBlocksInventory");
        final var level = helper.getLevel();
        final String id = "LegacyOpenBlocksInventory-2018-04-05_10.20.30-command-0";
        final var file = OBInventoryStore.resolveDumpPath(level, id);
        final var root = TagParser.parseTag("""
                {Type:"command",Inventory:{size:41,Items:[
                  {Slot:0b,id:"openblocks:generic",Count:2b,Damage:0s},
                  {Slot:1b,id:"openblocks:paintBrush",Count:1b,Damage:7s,tag:{
                    color:11685080,display:{Name:"Saved Brush",Lore:["Original paint"]},ench:[{id:34s,lvl:2s}]}},
                  {Slot:2b,id:"openblocks:elevator",Count:1b,Damage:14s},
                  {Slot:3b,id:"openblocks:sky",Count:1b,Damage:1s},
                  {Slot:4b,id:"openblocks:stencil",Count:3b,Damage:13s},
                  {Slot:5b,id:"openblocks:glyph",Count:2b,Damage:65s},
                  {Slot:6b,id:"openblocks:imaginary",Count:1b,Damage:1s,tag:{Color:11259375,Uses:7.5f,Mode:3}},
                  {Slot:7b,id:"openblocks:tank",Count:1b,Damage:0s,tag:{tank:{FluidName:"xpjuice",Amount:4200}}},
                  {Slot:8b,id:"openblocks:dev_null",Count:1b,Damage:0s,tag:{inventory:{size:1,Items:[
                    {Slot:0b,id:"minecraft:wool",Count:8b,Damage:11s}]}}},
                  {Slot:9b,id:"openblocks:luggage",Count:1b,Damage:0s,tag:{size:54,Items:[
                    {Slot:42b,id:"openblocks:generic",Count:4b,Damage:7s}]}},
                  {Slot:10b,id:"minecraft:purple_shulker_box",Count:1b,Damage:0s,tag:{BlockEntityTag:{Items:[
                    {Slot:3b,id:"openblocks:generic",Count:6b,Damage:5s}]}}},
                  {Slot:40b,id:"openblocks:generic_unstackable",Count:1b,Damage:0s}
                ]}}
                """);
        java.nio.file.Files.createDirectories(file.getParent());
        try {
            NbtIo.writeCompressed(root, file.toFile());
            helper.assertTrue(OBInventoryStore.restoreInventory(player, id), "The OpenBlocks 1.12.2 inventory did not restore");
            final var inventory = player.getInventory();
            helper.assertTrue(inventory.getItem(0).is(OBItems.GLIDER_WING.get()) && inventory.getItem(0).getCount() == 2
                    && inventory.offhand.get(0).is(OBItems.POINTER.get()), "Generic item metadata did not resolve to its distinct item");
            final ItemStack brush = inventory.getItem(1);
            final String lore = brush.getTagElement("display").getList("Lore", Tag.TAG_STRING).getString(0);
            helper.assertTrue(brush.is(OBItems.PAINTBRUSH.get()) && brush.getDamageValue() == 7
                            && OBPaintBrushItem.color(brush) == 11685080 && brush.getHoverName().getString().equals("Saved Brush")
                            && Component.Serializer.fromJson(lore).getString().equals("Original paint")
                            && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, brush) == 2,
                    "The paint brush lost its RGB, wear, name, lore, or enchantment");
            helper.assertTrue(OBElevatorItem.getColor(inventory.getItem(2)) == DyeColor.RED && OBSkyItem.isInverted(inventory.getItem(3))
                            && OBStencilItem.pattern(inventory.getItem(4)) == OBStencilPattern.MUSIC && inventory.getItem(4).getCount() == 3
                            && OBGlyphItem.character(OBGlyphItem.index(inventory.getItem(5))) == 'A',
                    "Block, stencil, or glyph metadata was lost");
            final ItemStack crayon = inventory.getItem(6);
            final var fluid = OBTankItem.contents(inventory.getItem(7));
            helper.assertTrue(OBImaginaryItem.color(crayon) == 11259375 && OBImaginaryItem.uses(crayon) == 7.5F
                            && OBImaginaryItem.mode(crayon) == OBImaginaryItem.Mode.STAIRS
                            && fluid.getFluid() == OBFluids.XP_JUICE.get() && fluid.getAmount() == 4200,
                    "The legacy crayon settings or tank's Liquid XP changed during import");
            final var stored = OBDevNullItem.getStored(inventory.getItem(8));
            final var packed = inventory.getItem(9).getTag();
            final var luggageItem = packed.getList("Items", Tag.TAG_COMPOUND).getCompound(0);
            final ItemStack memory = ItemStack.of(luggageItem);
            helper.assertTrue(stored.is(Items.BLUE_WOOL) && stored.getCount() == 8 && packed.getInt("size") == 54
                            && luggageItem.getByte("Slot") == 42 && memory.is(OBItems.MAP_MEMORY.get()) && memory.getCount() == 4,
                    "A nested inventory lost an item, its old slot, or its variant");
            final var shulkerItem = inventory.getItem(10).getTagElement("BlockEntityTag").getList("Items", Tag.TAG_COMPOUND).getCompound(0);
            final var line = ItemStack.of(shulkerItem);
            helper.assertTrue(shulkerItem.getByte("Slot") == 3 && line.is(OBItems.LINE.get()) && line.getCount() == 6,
                    "The shulker box lost its OpenBlocks item");
            final var spawned = OBInventoryStore.getSpawnStacks(level, id, "main", 9);
            helper.assertTrue(spawned.size() == 1 && ItemStack.isSameItemSameTags(spawned.get(0), inventory.getItem(9)),
                    "Indexed spawning used different migration rules from restore");

            root.getCompound("Inventory").getList("Items", Tag.TAG_COMPOUND)
                    .getCompound(9).getCompound("tag").getList("Items", Tag.TAG_COMPOUND)
                    .getCompound(0).putString("id", "openblocks:height_map");
            NbtIo.writeCompressed(root, file.toFile());
            try {
                OBInventoryStore.restoreInventory(player, id);
                helper.fail("A packed map with old world data references must stop restore");
            } catch (final IOException expected) {
                helper.assertTrue(expected.getMessage().contains("openblocks:height_map") && inventory.getItem(1) == brush,
                        "An unsupported item nested in luggage replaced part of the destination inventory");
            }
        } finally {
            OBInventoryStore.deleteDump(level, id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deathInventoryBackupHonorsTheLegacySwitchAndDeathVeto(final GameTestHelper helper) throws IOException {
        final var level = helper.getLevel();
        final String name = "DeathBackupQA";
        final var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name)) {
            @Override public void sendSystemMessage(final Component message) {}
        };
        player.getInventory().setItem(7, new ItemStack(Items.EMERALD, 12));
        final var enabled = OBServerConfig.DUMP_DEATH_INVENTORY;
        final boolean previous = enabled.get();
        final Consumer<LivingDeathEvent> veto = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        try {
            enabled.set(false);
            MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(player, level.damageSources().generic()));
            helper.assertTrue(OBInventoryStore.getMatchedDumps(level, name).isEmpty(), "Turning off death backups must suppress the snapshot");
            enabled.set(true);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingDeathEvent.class, veto);
            try {
                MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(player, level.damageSources().generic()));
                helper.assertTrue(OBInventoryStore.getMatchedDumps(level, name).isEmpty(), "A vetoed death must not create a death snapshot");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(veto);
            }
            MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(player, level.damageSources().generic()));
            final List<String> files = OBInventoryStore.getMatchedDumps(level, name);
            helper.assertTrue(files.size() == 1 && files.get(0).contains("-death-"), "A normal enabled death must create one snapshot");
            final var stacks = OBInventoryStore.getSpawnStacks(level, files.get(0), "main", 7);
            helper.assertTrue(stacks.size() == 1 && stacks.get(0).getCount() == 12, "The snapshot must capture the pre-drop inventory");
        } finally {
            enabled.set(previous);
            for (final String id : OBInventoryStore.getMatchedDumps(level, name)) OBInventoryStore.deleteDump(level, id);
        }
        helper.succeed();
    }

    private static FakePlayer player(final GameTestHelper helper, final String name) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        final Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static CompoundTag legacyItem(final int slot, final String id, final int damage, final int count) {
        final var tag = new CompoundTag();
        tag.putByte("Slot", (byte) slot);
        tag.putString("id", id);
        tag.putByte("Count", (byte) count);
        tag.putShort("Damage", (short) damage);
        return tag;
    }
}
