package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBGraveBlockEntity;
import art.arcane.openblocks.command.OBInventoryStore;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlocks;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class GraveGameTests {

    @GameTest(template = "empty")
    public static void deniedGraveDropKeepsItsLootForAnotherAttempt(final GameTestHelper helper) throws IOException {
        final var level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var player = player(helper, pos);
        final var shovel = new ItemStack(Items.IRON_SHOVEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        final var gold = new ItemStack(Items.GOLD_INGOT, 7);
        final var diamonds = new ItemStack(Items.DIAMOND, 13);
        final String id = OBInventoryStore.storeGraveBackup(player, pos, List.of(gold, diamonds));
        final var file = OBInventoryStore.resolveDumpPath(level, id).toFile();
        final var priorDump = NbtIo.readCompressed(file);
        priorDump.remove("Placed");
        priorDump.remove("GraveLocation");
        NbtIo.writeCompressed(priorDump, file);
        level.setBlockAndUpdate(pos, OBBlocks.GRAVE.get().defaultBlockState());
        final var grave = (OBGraveBlockEntity) level.getBlockEntity(pos);
        final var tag = new CompoundTag();
        tag.putString("InventoryId", id);
        tag.putString("OwnerName", "Grave test");
        tag.putString("DeathMessage", "An earlier 1.20.1 grave");
        grave.load(tag);

        final AABB dropArea = new AABB(pos).inflate(1);
        final Consumer<EntityJoinLevelEvent> deny = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof ItemEntity item
                    && item.getItem().is(Items.DIAMOND) && dropArea.contains(item.position())) event.setCanceled(true);
        };
        final double specialAction = OBServerConfig.GRAVE_SPECIAL_ACTION.get();
        OBServerConfig.GRAVE_SPECIAL_ACTION.set(0.0);
        try {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, deny);
            try {
                dig(helper, pos, player);
                final var pending = OBInventoryStore.readDroppedItems(level, id);
                helper.assertTrue(level.getBlockEntity(pos) == grave && pending.size() == 2 && pending.get(1).getCount() == 13
                                && shovel.getDamageValue() == 0 && level.getEntitiesOfClass(ItemEntity.class, dropArea).isEmpty(),
                        "A canceled spawn must roll back the accepted gold drop and keep both stacks and the shovel intact");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(deny);
            }

            dig(helper, pos, player);
            final var released = level.getEntitiesOfClass(ItemEntity.class, dropArea);
            helper.assertTrue(released.size() == 2 && count(released, gold) == 7 && count(released, diamonds) == 13
                            && !file.exists() && shovel.getDamageValue() == 2 && level.getBlockEntity(pos) == grave && !grave.hasLoot(),
                    "An accepted old grave dig must release both stacks once, consume its live dump, and keep the memorial");
            released.forEach(Entity::discard);
            dig(helper, pos, player);
            helper.assertTrue(shovel.getDamageValue() == 2 && level.getEntitiesOfClass(ItemEntity.class, dropArea).isEmpty(),
                    "An empty old grave must neither hand out the drops again nor wear the shovel");
        } finally {
            OBServerConfig.GRAVE_SPECIAL_ACTION.set(specialAction);
            OBInventoryStore.deleteDump(level, id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deathGraveGivesBackDropsOnPlacementVetoAndPersistsWithoutBackup(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final Vec3 origin = Vec3.atBottomCenterOf(pos);
        final String name = "GraveNewQA";
        final var donor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name)) {
            @Override public void sendSystemMessage(final Component message) {}
        };
        donor.setPos(origin.x, origin.y, origin.z);
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.GRASS.defaultBlockState());
        final var stored = new ItemStack(Items.IRON_SWORD);
        stored.setDamageValue(12);
        stored.setHoverName(Component.literal("Buried blade"));
        final var dropped = new ItemEntity(level, origin.x, origin.y, origin.z, stored);
        final var drops = new ArrayList<>(List.of(dropped));
        final Consumer<BlockEvent.EntityPlaceEvent> deny = event -> {
            if (event.getEntity() == donor) event.setCanceled(true);
        };
        final var player = player(helper, pos);
        final boolean backups = OBServerConfig.GRAVE_BACKUPS.get();
        final double specialAction = OBServerConfig.GRAVE_SPECIAL_ACTION.get();
        OBServerConfig.GRAVE_BACKUPS.set(false);
        OBServerConfig.GRAVE_SPECIAL_ACTION.set(0.0);
        try {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityPlaceEvent.class, deny);
            try {
                MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(donor, level.damageSources().generic(), drops, 0, false));
                helper.assertTrue(level.getBlockState(pos).is(Blocks.GRASS) && drops.size() == 1 && drops.get(0) == dropped,
                        "A denied grave placement must restore the grass and leave the original item for vanilla");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(deny);
            }

            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(donor, level.damageSources().generic(), drops, 0, false));
            helper.assertTrue(drops.isEmpty() && level.getBlockEntity(pos) instanceof OBGraveBlockEntity
                            && OBInventoryStore.getMatchedDumps(level, name).isEmpty(),
                    "A new grave must accept the drop even with separate backups disabled");
            final var grave = (OBGraveBlockEntity) level.getBlockEntity(pos);
            final var saved = grave.saveWithoutMetadata();
            helper.assertTrue(saved.getList("Loot", Tag.TAG_COMPOUND).size() == 1 && name.equals(saved.getString("OwnerName"))
                            && saved.getString("DeathMessageComponent").contains("grave.open_blocks.death_message"),
                    "The grave must save its item, owner, and styled message in the block entity");
            grave.load(saved);
            final var state = level.getBlockState(pos);
            helper.assertTrue(!state.onDestroyedByPlayer(level, pos, player, false, level.getFluidState(pos)),
                    "Normal mining must protect an occupied grave");
            final var shovel = new ItemStack(Items.IRON_SHOVEL);
            player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
            dig(helper, pos, player);
            final var after = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            helper.assertTrue(after.size() == 1 && ItemStack.isSameItemSameTags(after.get(0).getItem(), stored)
                            && level.getBlockEntity(pos) == grave && !grave.hasLoot() && shovel.getDamageValue() == 2,
                    "A different player must be able to dig the saved item once with its exact tags and wear the shovel");
            after.forEach(Entity::discard);
        } finally {
            OBServerConfig.GRAVE_BACKUPS.set(backups);
            OBServerConfig.GRAVE_SPECIAL_ACTION.set(specialAction);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void graveBackupCommandsKeepStacksBeyondPlayerSlots(final GameTestHelper helper) throws IOException {
        final var level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var player = player(helper, pos);
        final List<ItemStack> contents = new ArrayList<>();
        final int slots = player.getInventory().getContainerSize();
        for (int i = 0; i < slots + 2; i++) contents.add(new ItemStack(Items.COBBLESTONE, i + 1));
        contents.set(slots + 1, new ItemStack(Items.DIAMOND, 13));
        final String id = OBInventoryStore.storeGraveBackup(player, pos, contents);
        try {
            final var exposed = OBInventoryStore.getSpawnStacks(level, id, "main", null);
            final var last = OBInventoryStore.getSpawnStacks(level, id, "main", slots + 1);
            helper.assertTrue(exposed.size() == slots + 2 && last.size() == 1 && last.get(0).is(Items.DIAMOND),
                    "The administrator's main inventory target must expose every grave backup slot");
            player.getInventory().setItem(0, new ItemStack(Items.EMERALD));
            final AABB dropArea = new AABB(player.blockPosition()).inflate(1);
            final Consumer<EntityJoinLevelEvent> deny = event -> {
                if (event.getLevel() == level && event.getEntity() instanceof ItemEntity item
                        && item.getItem().is(Items.DIAMOND) && dropArea.contains(item.position())) event.setCanceled(true);
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, deny);
            try {
                helper.assertTrue(!OBInventoryStore.restoreInventory(player, id) && player.getInventory().getItem(0).is(Items.EMERALD)
                                && level.getEntitiesOfClass(ItemEntity.class, dropArea).isEmpty(),
                        "If an overflow drop is denied, restoration must leave the player and world as they were");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(deny);
            }
            helper.assertTrue(OBInventoryStore.restoreInventory(player, id), "The full backup should restore with permitted overflow");
            final var overflow = level.getEntitiesOfClass(ItemEntity.class, dropArea);
            helper.assertTrue(player.getInventory().getItem(0).is(Items.COBBLESTONE)
                            && player.getInventory().getItem(slots - 1).getCount() == slots && overflow.size() == 2
                            && count(overflow, contents.get(slots)) == slots + 1 && count(overflow, contents.get(slots + 1)) == 13,
                    "A grave restore must place every saved item in a player slot or accepted overflow drop");
            overflow.forEach(Entity::discard);
        } finally {
            OBInventoryStore.deleteDump(level, id);
        }
        helper.succeed();
    }

    private static int count(final List<ItemEntity> entities, final ItemStack stack) {
        return entities.stream().map(ItemEntity::getItem).filter(item -> item.is(stack.getItem())).mapToInt(ItemStack::getCount).sum();
    }

    private static FakePlayer player(final GameTestHelper helper, final BlockPos pos) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "GraveQA")) {
            @Override public void sendSystemMessage(final Component message) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        final Vec3 center = Vec3.atBottomCenterOf(pos.south());
        player.setPos(center.x, center.y, center.z);
        return player;
    }

    private static void dig(final GameTestHelper helper, final BlockPos pos, final FakePlayer player) {
        final var level = helper.getLevel();
        final var state = level.getBlockState(pos);
        final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false);
        state.use(level, player, InteractionHand.MAIN_HAND, hit);
    }
}
