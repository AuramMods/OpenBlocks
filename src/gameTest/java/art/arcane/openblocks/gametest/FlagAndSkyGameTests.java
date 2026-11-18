package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBFlagBlock;
import art.arcane.openblocks.block.OBSkyBlock;
import art.arcane.openblocks.block.entity.OBFlagBlockEntity;
import art.arcane.openblocks.item.OBFlagItem;
import art.arcane.openblocks.item.OBSkyItem;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class FlagAndSkyGameTests {

    private static final BlockPos DEVICE = new BlockPos(3, 3, 3);

    @GameTest(template = "empty")
    public static void flagsCraftAllCarpetColorsAndCycleInHand(final GameTestHelper helper) {
        for (final DyeColor color : DyeColor.values()) {
            final var carpet = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_carpet"));
            final var grid = ComponentRecipeGameTests.input(helper, Items.STICK, carpet, carpet,
                    Items.STICK, carpet, Items.AIR, Items.STICK);
            final ItemStack flag = ComponentRecipeGameTests.craft(helper, grid);
            helper.assertTrue(flag.is(OBItems.FLAG.get()) && OBFlagItem.getColor(flag) == color, "Crafting lost " + color + " flag color");
        }
        final var player = helper.makeMockSurvivalPlayer();
        final var flag = OBFlagItem.create(DyeColor.BLACK);
        flag.setCount(4);
        player.setItemInHand(InteractionHand.MAIN_HAND, flag);
        player.setItemInHand(InteractionHand.OFF_HAND, OBFlagItem.create(DyeColor.RED));
        player.getOffhandItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(OBFlagItem.getColor(player.getOffhandItem()) == DyeColor.RED, "Offhand use cycled a flag");
        player.setShiftKeyDown(true);
        helper.setBlock(DEVICE.below(), Blocks.STONE);
        helper.assertTrue(!place(helper, player, flag, DEVICE.below(), Direction.UP), "Sneak-use placed the flag");
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(OBFlagItem.getColor(flag) == DyeColor.WHITE && flag.getCount() == 4, "Color cycling failed to wrap or changed count");
        for (int step = 0; step < DyeColor.LIME.getId(); step++) {
            player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(OBFlagItem.getColor(flag) == DyeColor.LIME && !flag.hasTag(), "Returning to lime did not restore a stackable default");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flagsStackOnFenceAndDropTheirColorsWhenSupportBreaks(final GameTestHelper helper) {
        final var player = helper.makeMockSurvivalPlayer();
        player.setYHeadRot(-27);
        helper.setBlock(DEVICE.below(), Blocks.OAK_FENCE);
        helper.assertTrue(place(helper, player, OBFlagItem.create(DyeColor.CYAN), DEVICE.below(), Direction.UP)
                && place(helper, player, OBFlagItem.create(DyeColor.RED), DEVICE, Direction.UP),
                "Flags did not stack on a fence and another flag");
        final var lower = helper.getBlockState(DEVICE);
        final var pos = helper.absolutePos(DEVICE);
        final var level = helper.getLevel();
        final var flag = (OBFlagBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(lower.getValue(OBFlagBlock.FACING) == Direction.UP && lower.getValue(OBFlagBlock.COLOR) == DyeColor.CYAN
                && lower.getCollisionShape(level, pos).isEmpty() && flag.yaw() == -30, "The ground flag lost its color, snapped bearing, or collision behavior");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(DEVICE, player);
        helper.assertTrue(flag.yaw() == -20, "Use did not rotate the flag by ten degrees");
        player.setShiftKeyDown(true);
        helper.useBlock(DEVICE, player);
        final var restored = new OBFlagBlockEntity(pos, lower);
        restored.load(flag.getUpdateTag());
        helper.assertTrue(restored.yaw() == -30, "Sneak-use or synchronized yaw failed");
        helper.setBlock(DEVICE.below(), Blocks.AIR);
        helper.runAfterDelay(4, () -> {
            helper.assertBlockPresent(Blocks.AIR, DEVICE);
            helper.assertBlockPresent(Blocks.AIR, DEVICE.above());
            final var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2), item -> item.getItem().is(OBItems.FLAG.get()));
            helper.assertTrue(drops.size() == 2 && drops.stream().anyMatch(item -> OBFlagItem.getColor(item.getItem()) == DyeColor.CYAN)
                    && drops.stream().anyMatch(item -> OBFlagItem.getColor(item.getItem()) == DyeColor.RED), "Removing a fence lost or duplicated colored flags");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void wallFlagsKeepTheirMountedFaceAndRejectCeilings(final GameTestHelper helper) {
        final var player = helper.makeMockSurvivalPlayer();
        helper.setBlock(DEVICE.south(), Blocks.STONE);
        helper.setBlock(DEVICE.above(), Blocks.STONE);
        helper.assertTrue(!place(helper, player, OBFlagItem.create(DyeColor.BLUE), DEVICE.above(), Direction.DOWN)
                && place(helper, player, OBFlagItem.create(DyeColor.BLUE), DEVICE.south(), Direction.NORTH),
                "The flag accepted a ceiling or rejected a sturdy wall");
        final BlockPos pos = helper.absolutePos(DEVICE);
        final var state = helper.getBlockState(DEVICE);
        final var flag = (OBFlagBlockEntity) helper.getLevel().getBlockEntity(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(DEVICE, player);
        helper.assertTrue(state.getValue(OBFlagBlock.FACING) == Direction.NORTH && flag.yaw() == 0
                && state.getShape(helper.getLevel(), pos).bounds().getZsize() > state.getShape(helper.getLevel(), pos).bounds().getXsize(),
                "The wall flag rotated or did not use its wall selection shape");
        helper.setBlock(DEVICE.south(), Blocks.AIR);
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(Blocks.AIR, DEVICE);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void skyBlocksCraftAndInvertWithOneTorch(final GameTestHelper helper) {
        final var skyGrid = ComponentRecipeGameTests.input(helper, Items.GLASS, Items.ENDER_EYE, Items.GLASS, Items.GLASS,
                Items.END_STONE, Items.GLASS, Items.GLASS, Items.ENDER_EYE, Items.GLASS);
        final ItemStack normal = ComponentRecipeGameTests.craft(helper, skyGrid);
        helper.assertTrue(normal.is(OBItems.SKY.get()) && normal.getCount() == 6 && !OBSkyItem.isInverted(normal), "The legacy recipe did not craft six normal Sky Blocks");
        final var toggleGrid = ComponentRecipeGameTests.input(helper, Items.AIR, Items.REDSTONE_TORCH);
        toggleGrid.setItem(0, normal);
        final ItemStack inverted = ComponentRecipeGameTests.craft(helper, toggleGrid);
        helper.assertTrue(inverted.is(OBItems.SKY.get()) && OBSkyItem.isInverted(inverted) && inverted.getCount() == 1,
                "A redstone torch did not invert one Sky Block");
        toggleGrid.setItem(0, inverted);
        final ItemStack restored = ComponentRecipeGameTests.craft(helper, toggleGrid);
        helper.assertTrue(restored.is(OBItems.SKY.get()) && !restored.hasTag(), "Inverting again did not return a stackable normal block");
        toggleGrid.setItem(2, new ItemStack(Items.REDSTONE_TORCH));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, toggleGrid).isEmpty(), "Two torch slots incorrectly matched the conversion");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skyBlocksPreservePolarityDropsAndSwitchWithRedstone(final GameTestHelper helper) {
        final var normal = DEVICE;
        final var inverted = DEVICE.east(2);
        final var player = helper.makeMockSurvivalPlayer();
        helper.setBlock(normal.below(), Blocks.STONE);
        helper.setBlock(inverted.below(), Blocks.STONE);
        helper.setBlock(normal.west(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(place(helper, player, OBSkyItem.create(false), normal.below(), Direction.UP)
                && place(helper, player, OBSkyItem.create(true), inverted.below(), Direction.UP), "Sky Block items failed to place");
        helper.runAfterDelay(2, () -> {
            final var normalState = helper.getBlockState(normal);
            final var invertedState = helper.getBlockState(inverted);
            final var level = helper.getLevel();
            final var pos = helper.absolutePos(inverted);
            helper.assertTrue(OBSkyBlock.isActive(normalState) && normalState.getValue(OBSkyBlock.POWERED)
                    && OBSkyBlock.isActive(invertedState) && !invertedState.getValue(OBSkyBlock.POWERED)
                    && invertedState.getCollisionShape(level, pos).bounds().equals(Block.box(0, 0, 0, 16, 16, 16).bounds()),
                    "Sky Block power, inverted placement or solid collision failed");
            helper.setBlock(normal.west(), Blocks.AIR);
            helper.setBlock(inverted.east(), Blocks.REDSTONE_BLOCK);
            helper.runAfterDelay(2, () -> {
                final var powered = helper.getBlockState(inverted);
                helper.assertTrue(powered.getValue(OBSkyBlock.INVERTED) && powered.getValue(OBSkyBlock.POWERED)
                        && !OBSkyBlock.isActive(powered) && !OBSkyBlock.isActive(helper.getBlockState(normal)),
                        "Powered inverse or unpowered normal Sky Block remained active");
                final var drops = Block.getDrops(powered, level, pos, level.getBlockEntity(pos));
                helper.assertTrue(drops.size() == 1 && ItemStack.isSameItemSameTags(drops.get(0), OBSkyItem.create(true)),
                        "A powered inverse Sky Block drop lost its inversion or kept its temporary power");
                helper.succeed();
            });
        });
    }

    private static boolean place(final GameTestHelper helper, final Player player, final ItemStack stack,
            final BlockPos relativeSupport, final Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos support = helper.absolutePos(relativeSupport);
        final Vec3 point = Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        final var hit = new BlockHitResult(point, face, support, false);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }
}
