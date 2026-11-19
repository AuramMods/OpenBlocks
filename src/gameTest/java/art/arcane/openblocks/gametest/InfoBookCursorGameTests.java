package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.player.OBExperience;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class InfoBookCursorGameTests {

    private static final BlockPos TARGET = new BlockPos(2, 2, 2);

    @GameTest(template = "empty")
    public static void cursorAndInformationBookUseTheirOriginalRecipes(final GameTestHelper helper) {
        final var wool = Items.WHITE_WOOL;
        final var cursor = ComponentRecipeGameTests.craft(helper,
                ComponentRecipeGameTests.input(helper, wool, Items.AIR, Items.AIR, wool, wool, wool, wool, wool, wool));
        helper.assertTrue(cursor.is(OBItems.CURSOR.get()) && cursor.getCount() == 1 && cursor.getMaxStackSize() == 1,
                "Seven white wool in the pointer shape must craft a single unstackable Cursor");

        final var book = ComponentRecipeGameTests.craft(helper, ComponentRecipeGameTests.input(helper, Items.CLAY_BALL, Items.BOOK));
        helper.assertTrue(book.is(OBItems.INFO_BOOK.get()) && book.getCount() == 1, "A clay ball plus a book must craft the Information Book");
        final var player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        player.setItemInHand(InteractionHand.OFF_HAND, book.copy());
        final var heldBook = book.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(heldBook.getResult().consumesAction() && heldBook.getObject() == book && player.containerMenu == player.inventoryMenu,
                "Opening the client-only book must consume an interaction without modifying the book or player's server menu");
        helper.assertTrue(book.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult() == InteractionResult.PASS,
                "The Information Book must open from the main hand only");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cursorRemotelyTogglesWithTheLegacyExperienceCost(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var player = player(helper);
        final var target = lever(helper);
        final var cursor = link(helper, player, target, Direction.NORTH);
        helper.assertTrue(cursor.getTag() != null && cursor.getTag().getInt("side") == Direction.NORTH.get3DDataValue(),
                "Linking a Cursor must remember the selected face");
        helper.assertTrue(!powered(helper), "Sneak-linking must leave the lever untouched");

        final var saved = cursor.save(new CompoundTag());
        final var restored = ItemStack.of(saved);
        player.setItemInHand(InteractionHand.MAIN_HAND, restored);
        player.setPos(target.getX(), target.getY(), target.getZ() - 5.0);
        player.giveExperiencePoints(32);
        player.gameMode.useItem(player, level, restored, InteractionHand.MAIN_HAND);
        helper.assertTrue(powered(helper) && OBExperience.points(player) == 17, "At a distance of 5 blocks the lever must toggle for 15 XP");

        player.setPos(target.getX(), target.getY(), target.getZ() - 2.0);
        player.gameMode.useItem(player, level, restored, InteractionHand.MAIN_HAND);
        helper.assertTrue(!powered(helper) && OBExperience.points(player) == 17, "A close activation must be free");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cursorChecksRangeDimensionAndPlayerExperience(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var player = player(helper);
        final var target = lever(helper);
        final var cursor = link(helper, player, target, Direction.UP);
        player.giveExperiencePoints(14);
        player.setPos(target.getX(), target.getY(), target.getZ() - 5.0);
        player.gameMode.useItem(player, level, cursor, InteractionHand.MAIN_HAND);
        helper.assertTrue(!powered(helper) && OBExperience.points(player) == 14, "A player one point short must keep their XP and leave the block untouched");

        player.setGameMode(GameType.CREATIVE);
        player.setPos(target.getX(), target.getY(), target.getZ() - 8.0);
        player.gameMode.useItem(player, level, cursor, InteractionHand.MAIN_HAND);
        helper.assertTrue(!powered(helper), "The squared range limit must apply to creative players");

        player.setPos(target.getX(), target.getY(), target.getZ() - 5.0);
        cursor.getOrCreateTag().putString("dimension", (level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER).location().toString());
        player.gameMode.useItem(player, level, cursor, InteractionHand.MAIN_HAND);
        helper.assertTrue(!powered(helper), "A link from another dimension must leave the block untouched");

        final int legacy = level.dimension() == Level.NETHER ? -1 : level.dimension() == Level.END ? 1 : 0;
        cursor.getOrCreateTag().putInt("dimension", legacy);
        player.setItemInHand(InteractionHand.OFF_HAND, cursor.copy());
        final var offHand = player.getOffhandItem();
        player.gameMode.useItem(player, level, offHand, InteractionHand.OFF_HAND);
        helper.assertTrue(!powered(helper), "An offhand Cursor must not activate the target");
        player.gameMode.useItem(player, level, cursor, InteractionHand.MAIN_HAND);
        helper.assertTrue(powered(helper) && OBExperience.points(player) == 14, "A legacy link must activate within range, free in creative");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cursorHonorsForgeProtectionAtTheLinkedFace(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var player = player(helper);
        final var target = lever(helper);
        final var cursor = link(helper, player, target, Direction.EAST);
        player.setPos(target.getX(), target.getY(), target.getZ() - 5.0);
        player.giveExperiencePoints(25);
        final var listener = new TargetProtection(player, target);
        MinecraftForge.EVENT_BUS.register(listener);
        try {
            player.gameMode.useItem(player, level, cursor, InteractionHand.MAIN_HAND);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        helper.assertTrue(listener.face == Direction.EAST && !powered(helper) && OBExperience.points(player) == 25,
                "The Forge event must describe the actual clicked face and block both activation and cost when denied");
        helper.succeed();
    }

    private static BlockPos lever(final GameTestHelper helper) {
        helper.setBlock(TARGET.below(), Blocks.STONE);
        helper.setBlock(TARGET, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
        return helper.absolutePos(TARGET);
    }

    private static boolean powered(final GameTestHelper helper) {
        return helper.getBlockState(TARGET).getValue(LeverBlock.POWERED);
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CursorQA"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static ItemStack link(final GameTestHelper helper, final FakePlayer player, final BlockPos target, final Direction face) {
        final var cursor = new ItemStack(OBItems.CURSOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, cursor);
        player.setShiftKeyDown(true);
        player.setPos(target.getX(), target.getY(), target.getZ() - 2.0);
        final var result = player.gameMode.useItemOn(player, helper.getLevel(), cursor, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(target).relative(face, 0.5), face, target, false));
        player.setShiftKeyDown(false);
        helper.assertTrue(result.consumesAction(), "Sneak-use must link the Cursor through the ordinary Forge interaction path");
        return cursor;
    }

    private static final class TargetProtection {
        private final FakePlayer player;
        private final BlockPos target;
        private Direction face;

        TargetProtection(final FakePlayer player, final BlockPos target) {
            this.player = player;
            this.target = target;
        }

        @SubscribeEvent
        public void onInteract(final PlayerInteractEvent.RightClickBlock event) {
            if (event.getEntity() != player || !event.getPos().equals(target)) return;
            face = event.getFace();
            event.setUseBlock(Event.Result.DENY);
        }
    }
}
