package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.entity.OBCartographer;
import art.arcane.openblocks.item.OBEmptyMapItem;
import art.arcane.openblocks.item.OBHeightMapItem;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBHeightMapData;
import art.arcane.openblocks.world.OBHeightMapSurvey;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class CartographerGameTests {

    private static final BlockPos CENTER = new BlockPos(3, 3, 3);

    @GameTest(template = "empty")
    public static void cartographerCraftsAndMapsResizeAndClone(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, OBItems.ASSISTANT_BASE.get(), Items.ENDER_EYE);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).is(OBItems.CARTOGRAPHER.get()), "The assistant parts did not craft a Cartographer");
        grid.clearContent();
        grid.setItem(4, new ItemStack(OBItems.MAP_CONTROLLER.get()));
        for (final int slot : new int[] { 1, 3, 5, 7 }) grid.setItem(slot, new ItemStack(OBItems.MAP_MEMORY.get()));
        ItemStack blank = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(blank.is(OBItems.EMPTY_MAP.get()) && OBEmptyMapItem.scale(blank) == 0, "The controller and memory did not craft a blank 1:1 map");

        grid.clearContent();
        grid.setItem(3, new ItemStack(OBItems.MAP_MEMORY.get()));
        grid.setItem(5, new ItemStack(OBItems.MAP_MEMORY.get()));
        for (int scale = 1; scale <= OBHeightMapData.MAX_SCALE; scale++) {
            grid.setItem(4, blank);
            blank = ComponentRecipeGameTests.craft(helper, grid);
            helper.assertTrue(blank.is(OBItems.EMPTY_MAP.get()) && OBEmptyMapItem.scale(blank) == scale, "Resize produced the wrong map scale");
        }
        grid.setItem(4, blank);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "Resize accepted a map already at 1:16");
        grid.setItem(4, OBEmptyMapItem.create(0));
        grid.setItem(0, new ItemStack(Items.DIAMOND));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "Resize would have consumed an unrelated stack");
        grid.setItem(0, ItemStack.EMPTY);
        grid.setItem(1, new ItemStack(OBItems.MAP_MEMORY.get()));
        grid.setItem(7, new ItemStack(OBItems.MAP_MEMORY.get()));
        helper.assertTrue(OBEmptyMapItem.scale(ComponentRecipeGameTests.craft(helper, grid)) == 1,
                "Resize rejected the four-memory layout");

        final int mapId = OBHeightMapData.create(helper.getLevel(), helper.absolutePos(CENTER), 4);
        final ItemStack surveyed = OBHeightMapItem.create(blank, mapId, OBHeightMapData.get(helper.getLevel(), mapId));
        surveyed.setHoverName(Component.literal("Survey copy"));
        grid.clearContent();
        grid.setItem(0, surveyed);
        grid.setItem(8, blank);
        final ItemStack copies = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(copies.getCount() == 2 && OBHeightMapItem.mapId(copies) == mapId && copies.getHoverName().getString().equals("Survey copy"),
                "Cloning did not produce two copies of the same named survey");
        grid.setItem(8, OBEmptyMapItem.create(1));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "Cloning accepted a mismatched blank map");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cartographerSurveysTallAndNegativeTerrainWithSeparateLiquid(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos anchor = helper.absolutePos(CENTER);
        final int x = (anchor.getX() & ~15) + 9;
        final int z = (anchor.getZ() & ~15) + 9;
        level.setBlockAndUpdate(new BlockPos(x, 290, z), Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(x, 291, z), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(x, 300, z), Blocks.RED_WOOL.defaultBlockState());
        final BlockPos negative = new BlockPos(x + 4, -19, z);
        for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, negative.getX(), negative.getZ()) - 1; y > negative.getY(); y--) {
            level.setBlock(negative.atY(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        level.setBlockAndUpdate(negative, Blocks.EMERALD_BLOCK.defaultBlockState());
        final int mapId = OBHeightMapData.create(level, new BlockPos(x, 70, z), 0);
        final OBHeightMapData map = OBHeightMapData.get(level, mapId);
        final var previous = OBServerConfig.CARTOGRAPHER_BLACKLIST.get();
        OBServerConfig.CARTOGRAPHER_BLACKLIST.set(List.of("minecraft:red_wool"));
        try {
            helper.assertTrue(OBHeightMapSurvey.scanNext(map, level, x, z), "The cartographer missed the nearest loaded terrain chunk");
        } finally {
            OBServerConfig.CARTOGRAPHER_BLACKLIST.set(previous);
        }
        final int px = x - (map.minChunkX() << 4);
        final int pz = z - (map.minChunkZ() << 4);
        helper.assertTrue(map.surveyedChunks() == 1 && map.hasChunk(x >> 4, z >> 4), "The wrong chunk was marked surveyed");
        final var tall = map.sample(px, pz);
        helper.assertTrue(tall.terrainY() == 290 && tall.terrainColor() == MapColor.GOLD.id
                && tall.liquidY() == 291 && tall.liquidColor() == MapColor.WATER.id,
                "The saved terrain and liquid heights or colors were incorrect");
        final var belowZero = map.sample(px + 4, pz);
        helper.assertTrue(belowZero.terrainY() == -19 && belowZero.terrainColor() == MapColor.EMERALD.id,
                "The terrain layer did not preserve a negative world height: " + belowZero);

        final var restored = OBHeightMapData.load(map.save(new CompoundTag()));
        helper.assertTrue(restored.dimension().equals(level.dimension().location()) && restored.surveyedChunks() == 1
                && restored.sample(px, pz).equals(tall) && restored.sample(px + 4, pz).equals(belowZero),
                "Saving and reading did not preserve the terrain, liquid, or survey progress");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cartographerAveragesScaleAndNeverGeneratesUnloadedChunks(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos anchor = helper.absolutePos(CENTER);
        final int x = (anchor.getX() & ~15) + 8;
        final int z = (anchor.getZ() & ~15) + 8;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                final int y = 220 + 2 * dx + 2 * dz;
                level.setBlockAndUpdate(new BlockPos(x + dx, y, z + dz), Blocks.GOLD_BLOCK.defaultBlockState());
                if (dx == 0) level.setBlockAndUpdate(new BlockPos(x + dx, y + 3, z + dz), Blocks.WATER.defaultBlockState());
            }
        }
        final int mapId = OBHeightMapData.create(level, new BlockPos(x, 70, z), 1);
        final var map = OBHeightMapData.get(level, mapId);
        helper.assertTrue(OBHeightMapSurvey.scanNext(map, level, x, z), "The 1:2 map did not survey its origin chunk");
        final var avg = map.sample((x - (map.minChunkX() << 4)) / 2, (z - (map.minChunkZ() << 4)) / 2);
        helper.assertTrue(avg.terrainY() == 222 && avg.terrainColor() == MapColor.GOLD.id && avg.liquidColor() == 0,
                "The 1:2 map did not average terrain or incorrectly showed water covering only half the sample");

        final int far = 1_400_000;
        final int farId = OBHeightMapData.create(level, new BlockPos(-far - 1, 70, -far - 1), 4);
        final var farMap = OBHeightMapData.get(level, farId);
        helper.assertTrue(farMap.centerX() == -far - 16 && farMap.centerZ() == -far - 16 && farMap.chunksPerSide() == 64,
                "A maximum-scale survey at negative coordinates used the wrong bounds");
        helper.assertTrue(!OBHeightMapSurvey.scanNext(farMap, level, -far, -far) && farMap.surveyedChunks() == 0
                && level.getChunkSource().getChunkNow(-far >> 4, -far >> 4) == null, "The survey generated unloaded terrain");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cartographerPacksAndRedeploysAnActiveNamedSurvey(final GameTestHelper helper) {
        final var server = helper.getLevel();
        final FakePlayer player = player(helper);
        final ItemStack device = new ItemStack(OBItems.CARTOGRAPHER.get());
        device.setHoverName(Component.literal("Survey assistant"));
        player.setItemInHand(InteractionHand.MAIN_HAND, device);
        final Consumer<EntityJoinLevelEvent> deny = event -> {
            if (event.getEntity() instanceof OBCartographer drone && drone.distanceToSqr(player) < 9) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, deny);
        try {
            helper.assertTrue(use(player) == InteractionResult.FAIL && player.getMainHandItem() == device,
                    "Forge canceled deployment but the Cartographer was spent");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        helper.assertTrue(use(player).consumesAction() && player.getMainHandItem().isEmpty(), "Deployment did not spend one Cartographer");
        final var deployed = helper.getEntities(OBEntities.CARTOGRAPHER.get(), CENTER, 4).get(0);

        final ItemStack maps = OBEmptyMapItem.create(2);
        maps.setCount(2);
        maps.setHoverName(Component.literal("Survey paper"));
        player.setItemInHand(InteractionHand.MAIN_HAND, maps);
        helper.assertTrue(deployed.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS && maps.getCount() == 2,
                "Standing use moved a map into the Cartographer");
        player.setShiftKeyDown(true);
        helper.assertTrue(deployed.interact(player, InteractionHand.MAIN_HAND).consumesAction() && maps.getCount() == 1,
                "Sneak-use did not insert one map from a stack");
        for (int tick = 0; tick < 9; tick++) deployed.tick();

        final var saved = new CompoundTag();
        helper.assertTrue(deployed.save(saved) && saved.hasUUID("Owner") && saved.getUUID("Owner").equals(player.getUUID()),
                "The original Cartographer did not save its owner");
        final var id = OBHeightMapItem.mapId(ItemStack.of(saved.getCompound("MapItem")));
        final var map = OBHeightMapData.get(server, id);
        helper.assertTrue(map != null && map.surveyedChunks() > 0, "The held map was never surveyed");
        final int completed = map.surveyedChunks();
        deployed.discard();

        final var reloaded = EntityType.loadEntityRecursive(saved, server, entity -> entity);
        helper.assertTrue(reloaded instanceof OBCartographer, "Cartographer failed to load");
        final var drone = (OBCartographer) reloaded;
        final Consumer<EntityJoinLevelEvent> denyRecovery = event -> {
            if (event.getEntity() instanceof ItemEntity item && item.getItem().is(OBItems.CARTOGRAPHER.get())
                    && item.distanceToSqr(drone) < 0.1) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, denyRecovery);
        try {
            helper.assertTrue(!drone.hurt(server.damageSources().playerAttack(player), 1F) && !drone.isRemoved(),
                    "A rejected recovery drop ate the assistant or its survey");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(denyRecovery);
        }
        helper.assertTrue(drone.hurt(server.damageSources().playerAttack(player), 1F) && drone.isRemoved(),
                "Attacking the Cartographer did not pack it back into its item");
        final ItemEntity drop = server.getEntitiesOfClass(ItemEntity.class, drone.getBoundingBox().inflate(2),
                item -> item.getItem().is(OBItems.CARTOGRAPHER.get())).get(0);
        helper.assertTrue(drop.getItem().getHoverName().getString().equals("Survey assistant"), "Packing lost the Cartographer's name");
        player.setItemInHand(InteractionHand.MAIN_HAND, drop.getItem().copy());
        drop.discard();
        player.setShiftKeyDown(false);
        helper.assertTrue(use(player).consumesAction() && player.getMainHandItem().isEmpty(), "The packed assistant could not redeploy");
        final var resumed = helper.getEntities(OBEntities.CARTOGRAPHER.get(), CENTER, 4).get(0);
        for (int tick = 0; tick < 12; tick++) resumed.tick();
        helper.assertTrue(map.surveyedChunks() > completed, "The original survey did not resume after redeployment");

        player.setShiftKeyDown(true);
        resumed.interact(player, InteractionHand.MAIN_HAND);
        final ItemStack extracted = player.getMainHandItem();
        helper.assertTrue(extracted.getCount() == 1 && OBHeightMapItem.mapId(extracted) == id && map.scale() == 2
                && extracted.getHoverName().getString().equals("Survey paper") && resumed.progress().isEmpty(),
                "Empty-hand sneak-use lost the original map, scale, or name");
        resumed.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cartographerResurveyUpdatesCopiesAndStopsThePreviousSurvey(final GameTestHelper helper) {
        final FakePlayer player = player(helper);
        player.setShiftKeyDown(true);
        final var level = helper.getLevel();
        final var first = OBEntities.CARTOGRAPHER.get().create(level);
        first.setPos(helper.absoluteVec(new Vec3(4, 3.5, 4)));
        player.setItemInHand(InteractionHand.MAIN_HAND, OBEmptyMapItem.create(0));
        first.interact(player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 6; tick++) first.tick();
        final var entityTag = new CompoundTag();
        first.save(entityTag);
        final ItemStack copy = ItemStack.of(entityTag.getCompound("MapItem"));
        final int id = OBHeightMapItem.mapId(copy);
        final OBHeightMapData data = OBHeightMapData.get(level, id);
        helper.assertTrue(data.surveyedChunks() > 0, "The first survey did not start");
        final int previousCenter = data.centerX();

        final var second = OBEntities.CARTOGRAPHER.get().create(level);
        final Vec3 target = new Vec3(previousCenter + 24.5, first.getY(), first.getZ());
        second.setPos(target);
        player.moveTo(target.x, target.y, target.z, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, copy.copy());
        second.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(data.centerX() == previousCenter + 16 && data.surveyedChunks() == 0,
                "A copy did not recenter the shared map and clear stale survey data");
        for (int tick = 0; tick < 6; tick++) first.tick();
        helper.assertTrue(data.surveyedChunks() == 0, "The previous Cartographer continued writing the resurveyed map");
        copy.getItem().inventoryTick(copy, level, player, 0, true);
        helper.assertTrue(copy.getTag().getLong("Survey") == data.survey() && copy.getTag().getInt("CenterX") == data.centerX(),
                "An existing copy did not update to the new survey");
        helper.succeed();
    }

    private static InteractionResult use(final FakePlayer player) {
        return player.gameMode.useItem(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CartographyTest"));
        final Vec3 position = helper.absoluteVec(new Vec3(3.5, 3.0, 3.5));
        player.moveTo(position.x, position.y, position.z, 0, 0);
        return player;
    }
}
