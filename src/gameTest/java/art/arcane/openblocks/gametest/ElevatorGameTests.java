package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBElevatorBlock;
import art.arcane.openblocks.block.OBElevatorRotatingBlock;
import art.arcane.openblocks.block.OBElevatorTravel;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBElevatorItem;
import art.arcane.openblocks.player.OBExperience;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class ElevatorGameTests {

    @GameTest(template = "empty")
    public static void elevatorKeepsColorThroughRecipesPlacementAndDrops(final GameTestHelper helper) {
        final var recipe = ComponentRecipeGameTests.input(helper,
                Items.RED_WOOL, Items.RED_WOOL, Items.RED_WOOL,
                Items.RED_WOOL, Items.ENDER_PEARL, Items.RED_WOOL,
                Items.RED_WOOL, Items.RED_WOOL, Items.RED_WOOL);
        final ItemStack elevator = ComponentRecipeGameTests.craft(helper, recipe);
        helper.assertTrue(elevator.is(OBItems.ELEVATOR.get()) && OBElevatorItem.getColor(elevator) == DyeColor.RED,
                "Red wool must craft a red elevator");

        final var upgrade = ComponentRecipeGameTests.input(helper, Items.AIR, Items.IRON_INGOT, Items.IRON_INGOT);
        upgrade.setItem(0, elevator);
        final ItemStack rotating = ComponentRecipeGameTests.craft(helper, upgrade);
        helper.assertTrue(rotating.is(OBItems.ELEVATOR_ROTATING.get()) && OBElevatorItem.getColor(rotating) == DyeColor.RED,
                "Rotating conversion must preserve color");
        upgrade.setItem(0, new ItemStack(OBItems.ELEVATOR.get()));
        final ItemStack white = ComponentRecipeGameTests.craft(helper, upgrade);
        helper.assertTrue(white.is(OBItems.ELEVATOR_ROTATING.get()) && !white.hasTag(), "An old untagged elevator must upgrade to white");

        final BlockPos relativeSupport = new BlockPos(3, 1, 3);
        final BlockPos support = helper.absolutePos(relativeSupport);
        final BlockPos pos = support.above();
        helper.setBlock(relativeSupport, Blocks.STONE);
        final Player player = standingPlayer(helper, relativeSupport.east());
        final BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, support, false);
        final var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, rotating, hit);
        final var item = (BlockItem) rotating.getItem();
        helper.assertTrue(item.place(context).consumesAction(), "Rotating elevator did not place");

        final var redState = helper.getLevel().getBlockState(pos);
        helper.assertTrue(redState.getValue(OBElevatorBlock.COLOR) == DyeColor.RED, "Placed elevator lost color");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_DYE));
        helper.useBlock(relativeSupport.above(), player);
        final var blueState = helper.getLevel().getBlockState(pos);
        helper.assertTrue(blueState.getValue(OBElevatorBlock.COLOR) == DyeColor.BLUE, "Dye did not recolor elevator");
        helper.assertTrue(blueState.getValue(OBElevatorRotatingBlock.FACING) == redState.getValue(OBElevatorRotatingBlock.FACING),
                "Dye changed the elevator facing");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "Recoloring consumed the reusable dye");

        final var drops = Block.getDrops(blueState, helper.getLevel(), pos, null, player, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(OBItems.ELEVATOR_ROTATING.get())
                && OBElevatorItem.getColor(drops.get(0)) == DyeColor.BLUE, "Breaking the elevator did not return its blue variant");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void elevatorSkipsDifferentColorsAndBlockedLandings(final GameTestHelper helper) {
        final BlockPos origin = new BlockPos(3, 1, 3);
        final var red = OBBlocks.ELEVATOR.get().defaultBlockState().setValue(OBElevatorBlock.COLOR, DyeColor.RED);
        helper.setBlock(origin, red);
        helper.setBlock(origin.above(2), OBBlocks.ELEVATOR.get());
        helper.setBlock(origin.above(4), red);
        helper.setBlock(origin.above(5), Blocks.STONE);
        final BlockPos destination = origin.above(7);
        helper.setBlock(destination, red);

        final Player player = standingPlayer(helper, origin);
        player.setOnGround(false);
        helper.assertTrue(!OBElevatorTravel.use(player, false), "An airborne player activated an elevator");
        player.setOnGround(true);
        helper.assertTrue(OBElevatorTravel.use(player, false), "The next clear, matching elevator was not found");
        helper.assertTrue(player.blockPosition().equals(helper.absolutePos(destination.above())), "Player landed on the wrong elevator");
        helper.assertTrue(!OBElevatorTravel.use(player, true), "An immediate request reused the previous grounded state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void elevatorAppliesOverridesBlockRulesAndXpCost(final GameTestHelper helper) {
        final var oldRules = OBServerConfig.ELEVATOR_RULES.get();
        final var oldOverrides = OBServerConfig.ELEVATOR_OVERRIDES.get();
        final double oldRatio = OBServerConfig.ELEVATOR_XP_PER_BLOCK.get();
        try {
            OBServerConfig.ELEVATOR_XP_PER_BLOCK.set(2.0D);
            OBServerConfig.ELEVATOR_OVERRIDES.set(List.of("minecraft:blue_wool;blue"));
            OBServerConfig.ELEVATOR_RULES.set(List.of("minecraft:glass:abort"));

            final BlockPos origin = new BlockPos(3, 1, 3);
            helper.setBlock(origin, Blocks.BLUE_WOOL);
            helper.setBlock(origin.above(2), Blocks.GLASS);
            final BlockPos destination = origin.above(4);
            helper.setBlock(destination, OBBlocks.ELEVATOR.get().defaultBlockState().setValue(OBElevatorBlock.COLOR, DyeColor.BLUE));
            final Player player = standingPlayer(helper, origin);
            player.giveExperiencePoints(100);
            helper.assertTrue(!OBElevatorTravel.use(player, false), "An abort rule allowed travel through glass");
            helper.assertTrue(OBExperience.points(player) == 100, "Failed travel cost experience");

            OBServerConfig.ELEVATOR_RULES.set(List.of("minecraft:glass:ignore"));
            helper.assertTrue(OBElevatorTravel.use(player, false), "Override or pass-through rule did not permit travel");
            helper.assertTrue(player.blockPosition().equals(helper.absolutePos(destination.above())), "Player missed the matching elevator");
            helper.assertTrue(OBExperience.points(player) == 92, "A four-block trip did not spend eight experience points");
        } finally {
            OBServerConfig.ELEVATOR_RULES.set(oldRules);
            OBServerConfig.ELEVATOR_OVERRIDES.set(oldOverrides);
            OBServerConfig.ELEVATOR_XP_PER_BLOCK.set(oldRatio);
        }
        helper.succeed();
    }

    private static Player standingPlayer(final GameTestHelper helper, final BlockPos platform) {
        final Player player = helper.makeMockSurvivalPlayer();
        final Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(platform.above()));
        player.moveTo(start.x, start.y, start.z, 0.0F, 0.0F);
        player.setOnGround(true);
        return player;
    }
}
