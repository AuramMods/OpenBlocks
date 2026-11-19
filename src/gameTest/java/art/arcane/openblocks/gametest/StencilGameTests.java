package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import art.arcane.openblocks.block.entity.OBDrawingTableBlockEntity;
import art.arcane.openblocks.entity.OBGlyph;
import art.arcane.openblocks.item.OBGlyphItem;
import art.arcane.openblocks.item.OBPaintBrushItem;
import art.arcane.openblocks.item.OBStencilItem;
import art.arcane.openblocks.item.OBStencilPattern;
import art.arcane.openblocks.menu.OBDrawingTableMenu;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class StencilGameTests {

    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final int RED = 0xE6324C;
    private static final int BLUE = 0x42629F;

    @GameTest(template = "empty")
    public static void drawingTableCraftsAndConsumesOnlyTakenOutput(final GameTestHelper helper) {
        final var blanks = ComponentRecipeGameTests.input(helper, Items.AIR, Items.PAPER, Items.AIR,
                Items.PAPER, Items.IRON_INGOT, Items.PAPER, Items.AIR, Items.PAPER, Items.AIR);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, blanks).is(OBItems.UNPREPARED_STENCIL.get()), "The original blank recipe failed");
        final var blank = OBItems.UNPREPARED_STENCIL.get();
        final var tableRecipe = ComponentRecipeGameTests.input(helper, blank, OBItems.SKETCHING_PENCIL.get(), blank,
                Items.OAK_PLANKS, Items.CRAFTING_TABLE, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, tableRecipe).is(OBItems.DRAWING_TABLE.get()), "The original table recipe failed");

        final var table = table(helper);
        final Player first = player(helper);
        final Player second = player(helper);
        final var firstMenu = new OBDrawingTableMenu(1, first.getInventory(), table);
        final var secondMenu = new OBDrawingTableMenu(2, second.getInventory(), table);
        table.input().setItem(0, new ItemStack(blank, 5));
        helper.assertTrue(firstMenu.clickMenuButton(first, OBDrawingTableMenu.NEXT) && table.pattern() == OBStencilPattern.BORDER,
                "The table did not advance to the next original pattern");
        secondMenu.broadcastChanges();

        firstMenu.clicked(1, 1, ClickType.PICKUP, first);
        helper.assertTrue(firstMenu.getCarried().getCount() == 3 && OBStencilItem.pattern(firstMenu.getCarried()) == OBStencilPattern.BORDER
                && table.input().getItem(0).getCount() == 2, "A right click did not take and charge exactly half the output");
        secondMenu.clicked(1, 0, ClickType.SWAP, second);
        helper.assertTrue(second.getInventory().getItem(0).getCount() == 2 && table.input().isEmpty()
                && firstMenu.quickMoveStack(first, 1).isEmpty(), "A second viewer received stale output or the number key skipped payment");

        table.input().setItem(0, new ItemStack(blank, 4));
        firstMenu.quickMoveStack(first, 1);
        helper.assertTrue(table.input().isEmpty() && first.getInventory().countItem(OBItems.STENCIL.get()) == 4,
                "Shift-click failed to charge the entire transferred stack");
        table.input().setItem(0, new ItemStack(blank, 2));
        helper.getLevel().destroyBlock(table.getBlockPos(), true);
        helper.assertItemEntityCountIs(blank, POS, 2, 2);
        helper.assertItemEntityCountIs(OBItems.STENCIL.get(), POS, 2, 0);
        helper.assertTrue(!firstMenu.stillValid(first), "The destroyed table retained an interactive menu");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drawingTablePrintsSupportedGlyphsAndKeepsItsRemainingText(final GameTestHelper helper) {
        final var table = table(helper);
        final Player player = player(helper);
        final var menu = new OBDrawingTableMenu(3, player.getInventory(), table);
        table.input().setItem(0, new ItemStack(OBItems.UNPREPARED_STENCIL.get(), 37));
        menu.print(player, "Bad");
        helper.assertTrue(table.input().getItem(0).getCount() == 37, "Table printed glyphs in stencil mode");
        menu.clickMenuButton(player, OBDrawingTableMenu.CYCLE_MODE);
        menu.clickMenuButton(player, OBDrawingTableMenu.NEXT);
        helper.assertTrue(OBGlyphItem.character(table.glyph()) == 'B' && OBGlyphItem.index(table.result()) == table.glyph(),
                "Glyph selection did not change the virtual output");

        final String text = "A".repeat(34) + "★C";
        menu.print(player, text);
        helper.assertTrue(table.input().getItem(0).getCount() == 5 && table.remainingText().equals("AA★C"),
                "One print request exceeded the 32-character limit");
        menu.print(player, table.remainingText());
        helper.assertTrue(table.input().getItem(0).getCount() == 3 && table.remainingText().equals("★C"),
                "Printing consumed or skipped a character outside the legacy alphabet");
        helper.assertItemEntityCountIs(OBItems.GLYPH.get(), POS.above(), 2, 34);

        final Consumer<EntityJoinLevelEvent> deny = event -> {
            if (event.getEntity() instanceof ItemEntity drop && drop.getItem().is(OBItems.GLYPH.get())
                    && drop.blockPosition().equals(table.getBlockPos().above())) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(deny);
        try {
            menu.print(player, "CD");
            helper.assertTrue(table.input().getItem(0).getCount() == 3 && table.remainingText().equals("CD"),
                    "Rejected print drop charged for a missing glyph");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        final var restored = (OBDrawingTableBlockEntity) BlockEntity.loadStatic(table.getBlockPos(), table.getBlockState(), table.saveWithFullMetadata());
        helper.assertTrue(restored != null && restored.glyphMode() && restored.glyph() == table.glyph()
                && restored.input().getItem(0).getCount() == 3 && restored.remainingText().equals("CD"),
                "The drawing table lost its mode, selection, paper, or remaining text on reload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stencilsRotateLayerAndReturnWithoutErasingTheirPaint(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var player = player(helper);
        final var pos = helper.absolutePos(POS);
        helper.setBlock(POS, Blocks.STONE_BRICKS);
        final var stencil = OBStencilItem.create(OBStencilPattern.BORDER, 2);
        hold(player, stencil);
        final var hit = hit(pos, Direction.NORTH);
        helper.assertTrue(use(player, hit).consumesAction() && stencil.getCount() == 1,
                "Attaching a stencil did not wrap the stone or consume one sheet");
        helper.assertTrue(!use(player, hit).consumesAction() && stencil.getCount() == 1, "A second stencil occupied the same face");
        final var canvas = (OBCanvasBlockEntity) level.getBlockEntity(pos);

        hold(player, OBPaintBrushItem.create(RED));
        use(player, hit);
        hold(player, ItemStack.EMPTY);
        canvas.getBlockState().use(level, player, InteractionHand.MAIN_HAND, hit);
        hold(player, OBPaintBrushItem.create(BLUE));
        use(player, hit);
        final var face = canvas.face(Direction.NORTH);
        helper.assertTrue(face.cover().rotation() == 1 && face.layers().size() == 2
                && face.layers().get(0).mask().rotation() == 0 && face.layers().get(0).color() == RED
                && face.layers().get(1).mask().rotation() == 1 && face.layers().get(1).color() == BLUE
                && !canvas.face(Direction.SOUTH).decorated(), "Rotating the stencil changed older layers or a different face");
        final var loaded = (OBCanvasBlockEntity) BlockEntity.loadStatic(pos, canvas.getBlockState(), canvas.saveWithFullMetadata());
        helper.assertTrue(loaded != null && loaded.face(Direction.NORTH).equals(face) && loaded.original().is(Blocks.STONE_BRICKS),
                "Patterned paint, stencil rotation, or original block failed to reload");

        hold(player, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        canvas.getBlockState().use(level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(canvas.face(Direction.NORTH).cover() == null && canvas.face(Direction.NORTH).layers().size() == 2,
                "Recovering a stencil also erased its paint");
        helper.assertItemEntityCountIs(OBItems.STENCIL.get(), POS, 2, 1);
        hold(player, new ItemStack(OBItems.SQUEEGEE.get()));
        use(player, hit);
        helper.assertTrue(level.getBlockState(pos).is(Blocks.STONE_BRICKS), "Cleaning finished layers failed to restore the original block");
        helper.assertItemEntityCountIs(OBItems.STENCIL.get(), POS, 2, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stencilRecoveryIsAtomicAndBreakDropsEveryAttachedSheet(final GameTestHelper helper) {
        final var player = player(helper);
        final var level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(POS);
        helper.setBlock(POS, Blocks.GLASS);
        hold(player, OBStencilItem.create(OBStencilPattern.HEART, 2));
        use(player, hit(pos, Direction.NORTH));
        final var veto = (Consumer<BlockEvent.EntityPlaceEvent>) event -> {
            if (event.getEntity() == player && event.getPos().equals(pos)) event.setCanceled(true);
        };
        hold(player, new ItemStack(OBItems.SQUEEGEE.get()));
        MinecraftForge.EVENT_BUS.addListener(veto);
        try {
            helper.assertTrue(use(player, hit(pos, Direction.NORTH)) == InteractionResult.FAIL
                    && level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas
                    && canvas.face(Direction.NORTH).cover().pattern() == OBStencilPattern.HEART,
                    "Denied cleanup did not restore the attached cover");
            helper.assertItemEntityCountIs(OBItems.STENCIL.get(), POS, 2, 0);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(veto);
        }

        hold(player, OBStencilItem.create(OBStencilPattern.STRIPES, 1));
        use(player, hit(pos, Direction.EAST));
        level.destroyBlock(pos, true);
        helper.assertItemEntityCountIs(OBItems.STENCIL.get(), POS, 2, 2);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void glyphsMountOnTheSubBlockGridAndSaveAndDropTheSameCharacter(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var player = player(helper);
        final var pos = helper.absolutePos(POS);
        helper.setBlock(POS, Blocks.QUARTZ_BLOCK);
        final var glyph = OBGlyphItem.create(OBGlyphItem.indexOf('B'), 2);
        hold(player, glyph);
        final var north = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.70, 0.37, 0), Direction.NORTH, pos, false);
        helper.assertTrue(use(player, north).consumesAction() && glyph.getCount() == 1, "Mounting a glyph did not consume exactly one item");
        final var hanging = level.getEntitiesOfClass(OBGlyph.class, new AABB(pos).inflate(2)).get(0);
        helper.assertTrue(hanging.getDirection() == Direction.NORTH && hanging.offsetX() == 11 && hanging.offsetY() == 5
                && hanging.getPos().equals(pos.north()), "Glyph did not snap to the clicked grid cell or face");
        helper.assertTrue(!use(player, north).consumesAction() && glyph.getCount() == 1, "An overlapping glyph consumed an item");
        final var restored = new OBGlyph(OBEntities.GLYPH.get(), level);
        restored.setPos(hanging.getX(), hanging.getY(), hanging.getZ());
        restored.readAdditionalSaveData(hanging.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
        helper.assertTrue(restored.getBoundingBox().equals(hanging.getBoundingBox()) && restored.glyph() == hanging.glyph(),
                "Saved glyph lost its placement or character");
        final var packet = new FriendlyByteBuf(Unpooled.buffer());
        try {
            hanging.writeSpawnData(packet);
            restored.readSpawnData(packet);
            helper.assertTrue(restored.glyph() == hanging.glyph() && restored.getBoundingBox().equals(hanging.getBoundingBox()),
                    "The glyph spawn payload lost its placement");
        } finally {
            packet.release();
        }
        hanging.hurt(level.damageSources().playerAttack(player), 1);
        helper.assertItemEntityCountIs(OBItems.GLYPH.get(), POS, 2, 1);
        helper.succeed();
    }

    private static OBDrawingTableBlockEntity table(final GameTestHelper helper) {
        helper.setBlock(POS, OBBlocks.DRAWING_TABLE.get());
        return (OBDrawingTableBlockEntity) helper.getBlockEntity(POS);
    }

    private static Player player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "StencilQA"));
        final Vec3 feet = helper.absoluteVec(new Vec3(2.5, 2, 0.5));
        player.moveTo(feet.x, feet.y, feet.z, 0, 0);
        return player;
    }

    private static BlockHitResult hit(final BlockPos pos, final Direction direction) {
        return new BlockHitResult(Vec3.atCenterOf(pos).relative(direction, 0.5), direction, pos, false);
    }

    private static void hold(final Player player, final ItemStack item) { player.setItemInHand(InteractionHand.MAIN_HAND, item); }

    private static InteractionResult use(final Player player, final BlockHitResult hit) {
        final var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        final var first = player.getMainHandItem().onItemUseFirst(context);
        return first == InteractionResult.PASS ? player.getMainHandItem().useOn(context) : first;
    }
}
