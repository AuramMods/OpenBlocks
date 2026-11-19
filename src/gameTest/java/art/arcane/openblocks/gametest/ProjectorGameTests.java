package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBProjectorBlock;
import art.arcane.openblocks.block.entity.OBProjectorBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBEmptyMapItem;
import art.arcane.openblocks.item.OBHeightMapItem;
import art.arcane.openblocks.menu.OBProjectorMenu;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBHeightMapData;
import art.arcane.openblocks.world.OBHeightMapSurvey;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class ProjectorGameTests {

    private static final BlockPos POS = new BlockPos(2, 1, 2);

    @GameTest(template = "empty")
    public static void projectorCraftsAndControlsItsHalfBlockLight(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.LAPIS_LAZULI,
                Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT, Items.SMOOTH_STONE_SLAB, Items.REDSTONE, Items.SMOOTH_STONE_SLAB);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).is(OBItems.PROJECTOR.get()), "The original projector ingredients could not craft it");
        grid.setItem(0, new ItemStack(Items.LAPIS_LAZULI));
        grid.setItem(2, new ItemStack(Items.GLOWSTONE_DUST));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).is(OBItems.PROJECTOR.get()), "The mirrored projector recipe was rejected");

        final var projector = place(helper, POS);
        final var level = helper.getLevel();
        final var pos = projector.getBlockPos();
        helper.assertTrue(projector.getBlockState().getCollisionShape(level, pos).bounds().maxY == 0.5
                && projector.getBlockState().getLightEmission(level, pos) == 0, "The empty projector was not an unlit half block");
        final boolean light = OBServerConfig.PROJECTOR_LIGHT_ENABLED.get();
        final int brightness = OBServerConfig.PROJECTOR_LIGHT_LEVEL.get();
        try {
            OBServerConfig.PROJECTOR_LIGHT_ENABLED.set(true);
            OBServerConfig.PROJECTOR_LIGHT_LEVEL.set(12);
            projector.inventory().insertItem(0, OBEmptyMapItem.create(0), false);
            helper.assertTrue(projector.getBlockState().getValue(OBProjectorBlock.ACTIVE)
                    && projector.getBlockState().getLightEmission(level, pos) == 12, "An installed map did not use the configured light level");
            OBServerConfig.PROJECTOR_LIGHT_ENABLED.set(false);
            helper.assertTrue(projector.getBlockState().getValue(OBProjectorBlock.ACTIVE)
                    && projector.getBlockState().getLightEmission(level, pos) == 0, "Disabling light also disabled the projection or left it lit");
        } finally {
            OBServerConfig.PROJECTOR_LIGHT_ENABLED.set(light);
            OBServerConfig.PROJECTOR_LIGHT_LEVEL.set(brightness);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectorConvertsOneBlankMapAndPreservesItThroughMenuAndBreak(final GameTestHelper helper) {
        final var projector = place(helper, POS);
        final var capability = projector.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH);
        final var handler = capability.orElseThrow(IllegalStateException::new);
        final ItemStack blank = OBEmptyMapItem.create(2).copyWithCount(3);
        final var rejected = handler.insertItem(0, new ItemStack(Items.MAP), false);
        helper.assertTrue(rejected.is(Items.MAP) && handler.insertItem(0, blank, true).getCount() == 2 && projector.map() == null,
                "The map slot accepted a vanilla map or created a canvas during simulated insertion");
        helper.assertTrue(handler.insertItem(0, blank, false).getCount() == 2 && projector.map().scale() == 2
                && projector.map().layerCount() == 0, "Insertion did not turn one blank into an empty projection canvas");
        final int map = projector.mapId();
        final FakePlayer player = player(helper);
        final var menu = new OBProjectorMenu(1, player.getInventory(), projector);
        helper.assertTrue(menu.clickMenuButton(player, OBProjectorMenu.ROTATE_LEFT) && projector.rotation() == 3
                && !menu.clickMenuButton(player, 63), "The menu did not wrap rotation or accepted an unknown action");
        final var restored = BlockEntity.loadStatic(projector.getBlockPos(), projector.getBlockState(), projector.saveWithFullMetadata());
        helper.assertTrue(restored instanceof OBProjectorBlockEntity loaded && loaded.rotation() == 3 && loaded.mapId() == map,
                "Saved rotation or the map slot did not survive block entity loading");
        final var moved = menu.quickMoveStack(player, 0);
        helper.assertTrue(OBHeightMapItem.mapId(moved) == map && !projector.getBlockState().getValue(OBProjectorBlock.ACTIVE),
                "Shift-click did not remove the original map and turn the projector off");
        int playerSlot = -1;
        for (int i = 1; i < menu.slots.size(); i++) {
            if (OBHeightMapItem.mapId(menu.getSlot(i).getItem()) == map) playerSlot = i;
        }
        helper.assertTrue(playerSlot > 0 && !menu.quickMoveStack(player, playerSlot).isEmpty() && projector.mapId() == map,
                "Shift-click did not put the same map back");

        helper.getLevel().destroyBlock(projector.getBlockPos(), true);
        helper.assertTrue(!capability.isPresent() && !menu.stillValid(player) && !menu.clickMenuButton(player, OBProjectorMenu.ROTATE_RIGHT),
                "The removed projector retained a live menu or capability");
        helper.assertItemEntityCountIs(OBItems.HEIGHT_MAP.get(), POS, 2, 1);
        helper.assertItemEntityCountIs(OBItems.PROJECTOR.get(), POS, 2, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectorReadsExistingSurveyFilesAndPreservesScriptedLayers(final GameTestHelper helper) {
        final var older = new CompoundTag();
        older.putString("Dimension", "minecraft:overworld");
        older.putByte("Scale", (byte) 2);
        older.putLong("Survey", 7);
        older.putLongArray("Chunks", new long[] {3});
        final int[] terrain = new int[4096];
        final byte[] color = new byte[4096];
        terrain[1] = -21;
        color[1] = (byte) MapColor.EMERALD.id;
        older.putIntArray("TerrainHeight", terrain);
        older.putByteArray("TerrainColor", color);
        older.putIntArray("LiquidHeight", new int[4096]);
        older.putByteArray("LiquidColor", new byte[4096]);
        final OBHeightMapData map = OBHeightMapData.load(older);
        helper.assertTrue(map.surveyedChunks() == 2 && map.layerCount() == 2 && map.minHeight() == -64
                && map.layerHeight(0, 1, 0) == -21 && map.layerColor(0, 1, 0) == MapColor.EMERALD.id && map.layerAlpha(1) == 128,
                "The earlier 1.20.1 survey layout did not load correctly");
        final long revision = map.revision();
        helper.assertTrue(map.appendLayer(), "A survey could not receive an extra layer");
        map.setLayerAlpha(2, 90);
        map.setPoint(2, 13, 19, 302, MapColor.GOLD.id);
        final var restored = OBHeightMapData.load(map.save(new CompoundTag()));
        helper.assertTrue(restored.revision() > revision && restored.layerCount() == 3 && restored.layerAlpha(2) == 90
                && restored.layerHeight(2, 13, 19) == 302 && restored.layerColor(2, 13, 19) == MapColor.GOLD.id,
                "Scripted heights, colors, or opacity did not survive the new save layout");
        restored.clearLayer(2);
        helper.assertTrue(restored.layerCount() == 3 && restored.layerColor(2, 13, 19) == 0 && restored.layerAlpha(2) == 255,
                "Clearing a layer changed its index or retained its colors");
        restored.clearMap();
        helper.assertTrue(restored.layerCount() == 0 && restored.surveyedChunks() == 0 && restored.survey() > map.survey(),
                "Clearing all layers left a live Cartographer survey");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectorsShareSurveyUpdatesAndRejectOlderSnapshots(final GameTestHelper helper) {
        final var first = place(helper, POS);
        final var second = place(helper, POS.east(2));
        final var level = helper.getLevel();
        final var pos = first.getBlockPos();
        final int id = OBHeightMapData.create(level, pos, 0);
        final var data = OBHeightMapData.get(level, id);
        final var item = OBHeightMapItem.create(OBEmptyMapItem.create(0), id, data);
        first.inventory().insertItem(0, item, false);
        second.inventory().insertItem(0, item.copy(), false);
        final CompoundTag old = data.save(new CompoundTag());
        helper.assertTrue(OBHeightMapSurvey.scanNext(data, level, pos.getX(), pos.getZ()), "The shared map did not receive a first survey update");
        final CompoundTag update = first.getUpdateTag();
        helper.assertTrue(update.getLong("Revision") == second.getUpdateTag().getLong("Revision")
                && !update.contains("Layers") && !update.contains("Inventory"), "Projectors did not share a revision or sent full maps in the chunk tag");

        final var client = new OBProjectorBlockEntity(pos, first.getBlockState());
        client.handleUpdateTag(update);
        client.acceptProjection(id, old);
        helper.assertTrue(client.map() == null, "A stale projection arrived and replaced the advertised revision");
        client.acceptProjection(id, data.save(new CompoundTag()));
        helper.assertTrue(client.map() != null && client.map().surveyedChunks() == 1, "The current projection update was not accepted");
        final var cleared = new CompoundTag();
        cleared.putInt("Map", -1);
        cleared.putLong("Revision", -1);
        client.handleUpdateTag(cleared);
        client.acceptProjection(id, data.save(new CompoundTag()));
        helper.assertTrue(client.map() == null, "A removed map reappeared from a late packet");
        helper.succeed();
    }

    private static OBProjectorBlockEntity place(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, OBBlocks.PROJECTOR.get());
        return (OBProjectorBlockEntity) helper.getBlockEntity(pos);
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ProjectorQA"));
        final Vec3 anchor = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        player.moveTo(anchor.x, anchor.y, anchor.z, 0, 0);
        return player;
    }
}
