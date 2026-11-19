package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBGuideBlock;
import art.arcane.openblocks.block.OBRedstoneMachineBlock;
import art.arcane.openblocks.guide.OBGuideOrientation;
import art.arcane.openblocks.item.OBWrenchItem;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent.BlockToolModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class UtilityToolGameTests {

    private static final BlockPos TARGET = new BlockPos(2, 2, 2);

    @GameTest(template = "empty")
    public static void utilityToolsKeepTheirOriginalRecipes(final GameTestHelper helper) {
        final var bar = ComponentRecipeGameTests.input(helper, Items.AIR, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT);
        final var wrench = ComponentRecipeGameTests.craft(helper, bar);
        helper.assertTrue(wrench.is(OBItems.WRENCH.get()) && wrench.getMaxStackSize() == 1 && wrench.canPerformAction(OBWrenchItem.ROTATE),
                "Seven iron ingots must craft one wrench with the Forge rotation action");
        final var eraser = ComponentRecipeGameTests.craft(helper,
                ComponentRecipeGameTests.input(helper, Items.LAPIS_LAZULI, Items.SLIME_BALL, Items.PINK_WOOL));
        helper.assertTrue(eraser.is(OBItems.EPIC_ERASER.get()) && eraser.hasFoil() && eraser.getMaxDamage() == 15 && eraser.getMaxStackSize() == 1,
                "The original Epic Eraser recipe must make one glinting eraser with fifteen uses");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void epicEraserClearsOnlyLoreFromOneItem(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper);
        final var eraser = new ItemStack(OBItems.EPIC_ERASER.get());
        final var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setHoverName(Component.literal("Camp blade"));
        sword.setDamageValue(37);
        sword.enchant(Enchantments.SHARPNESS, 3);
        sword.getOrCreateTag().putInt("PortCheck", 28);
        final var expected = sword.copy();
        addLore(sword, "Lore");
        addLore(sword, "SillyLore");
        grid.setItem(0, sword);
        grid.setItem(4, eraser);
        final var clean = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(clean.getCount() == 1 && ItemStack.isSameItemSameTags(clean, expected) && !ItemStack.isSameItemSameTags(sword, expected),
                "Erasing lore altered the source item or its existing enchantments, name, damage, or custom data");

        final var stackable = new ItemStack(Items.STICK, 8);
        addLore(stackable, "SillyLore");
        grid.setItem(0, stackable);
        final var one = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(one.is(Items.STICK) && one.getCount() == 1 && stackable.getCount() == 8 && !one.hasTag(),
                "A lored stack must craft one clean item and preserve the source until taking the result");
        grid.setItem(1, eraser.copy());
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "Two erasers incorrectly matched the lore recipe");
        grid.setItem(1, sword);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "Two lore items incorrectly matched the recipe");
        grid.setItem(1, ItemStack.EMPTY);
        grid.setItem(0, expected);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "An item with no lore matched the eraser");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void epicEraserCostsOneUseAndReturnsNoExtraContainers(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper);
        final var milk = new ItemStack(Items.MILK_BUCKET);
        addLore(milk, "Lore");
        grid.setItem(7, milk);
        var eraser = new ItemStack(OBItems.EPIC_ERASER.get());
        eraser.setHoverName(Component.literal("Travel eraser"));
        for (int used = 0; used < 15; used++) {
            grid.setItem(2, eraser);
            final var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow();
            final var result = recipe.assemble(grid, helper.getLevel().registryAccess());
            final var remaining = recipe.getRemainingItems(grid);
            helper.assertTrue(result.is(Items.MILK_BUCKET) && !result.hasTag() && remaining.get(7).isEmpty(),
                    "The target's ordinary crafting remainder would duplicate its container");
            final var next = remaining.get(2);
            helper.assertTrue(eraser.getDamageValue() == used && (used == 14 ? next.isEmpty()
                    : next.is(OBItems.EPIC_ERASER.get()) && next.getCount() == 1 && next.getDamageValue() == used + 1
                            && next.getHoverName().getString().equals("Travel eraser")), "The eraser charged the wrong durability or lost its name");
            eraser = next;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wrenchPreservesVanillaAndOpenBlocksMachineInventories(final GameTestHelper helper) {
        final var player = player(helper);
        final var level = helper.getLevel();
        final BlockPos target = helper.absolutePos(TARGET);
        helper.setBlock(TARGET, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.NORTH));
        final var dispenser = (DispenserBlockEntity) level.getBlockEntity(target);
        dispenser.setItem(1, new ItemStack(Items.ARROW, 9));
        helper.assertTrue(use(player, target, Direction.EAST).consumesAction()
                && helper.getBlockState(TARGET).getValue(DispenserBlock.FACING) == Direction.DOWN
                && level.getBlockEntity(target) == dispenser && dispenser.getItem(1).getCount() == 9,
                "A dispenser should turn about the clicked axis and preserve the existing inventory entity");

        helper.setBlock(TARGET, OBBlocks.BLOCK_PLACER.get().defaultBlockState().setValue(OBRedstoneMachineBlock.FACING, Direction.NORTH));
        final var placer = level.getBlockEntity(target);
        final var input = new ItemStack(Items.STONE, 12);
        placer.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.SOUTH).orElseThrow(AssertionError::new).insertItem(0, input, false);
        helper.assertTrue(use(player, target, Direction.UP).consumesAction()
                && helper.getBlockState(TARGET).getValue(OBRedstoneMachineBlock.FACING) == Direction.UP && level.getBlockEntity(target) == placer,
                "The six-way OpenBlocks machine should face the clicked side");
        helper.assertTrue(placer.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(AssertionError::new).getStackInSlot(0).getCount() == 12,
                "Turning the machine lost its stored stack or broke its sided inventory");
        helper.assertTrue(use(player, target, Direction.UP).consumesAction()
                && helper.getBlockState(TARGET).getValue(OBRedstoneMachineBlock.FACING) == Direction.DOWN,
                "Clicking the machine's existing face must turn it to the opposite side");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wrenchHonorsSneakingAndRefusesJoinedOrUnsupportedParts(final GameTestHelper helper) {
        final var player = player(helper);
        final var level = helper.getLevel();
        final BlockPos target = helper.absolutePos(TARGET);
        helper.setBlock(TARGET.below(), Blocks.STONE);
        final var single = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        helper.setBlock(TARGET, single);
        final var chest = (ChestBlockEntity) level.getBlockEntity(target);
        chest.setItem(0, new ItemStack(Items.EMERALD, 11));
        helper.assertTrue(use(player, target, Direction.NORTH) == InteractionResult.FAIL && helper.getBlockState(TARGET) == single,
                "An ordinary wrench use rotated or opened a chest");
        player.setShiftKeyDown(true);
        helper.assertTrue(use(player, target, Direction.NORTH).consumesAction()
                && helper.getBlockState(TARGET).getValue(ChestBlock.FACING) == Direction.EAST && chest.getItem(0).getCount() == 11,
                "Sneak-use did not turn the single chest and keep its contents");

        final var half = single.setValue(ChestBlock.TYPE, ChestType.LEFT);
        helper.setBlock(TARGET.relative(ChestBlock.getConnectedDirection(half)), single.setValue(ChestBlock.TYPE, ChestType.RIGHT));
        helper.setBlock(TARGET, half);
        helper.assertTrue(use(player, target, Direction.NORTH) == InteractionResult.FAIL && helper.getBlockState(TARGET) == half,
                "A double chest became disconnected under the wrench");

        final var wallLever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.NORTH);
        helper.setBlock(TARGET.south(), Blocks.STONE);
        helper.setBlock(TARGET, wallLever);
        helper.setBlock(TARGET.west(), Blocks.AIR);
        helper.assertTrue(use(player, target, Direction.NORTH) == InteractionResult.FAIL && helper.getBlockState(TARGET) == wallLever,
                "The wrench turned a mounted lever toward a missing support");
        helper.setBlock(TARGET.west(), Blocks.STONE);
        helper.assertTrue(use(player, target, Direction.NORTH).consumesAction()
                && helper.getBlockState(TARGET).getValue(LeverBlock.FACING) == Direction.EAST, "A supported sneaking lever rotation failed");
        final var piston = Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH).setValue(PistonBaseBlock.EXTENDED, true);
        helper.setBlock(TARGET, piston);
        helper.assertTrue(use(player, target, Direction.EAST) == InteractionResult.FAIL && helper.getBlockState(TARGET) == piston,
                "The wrench moved an extended piston");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wrenchReorientsGuidesAndLeavesVetoedBlocksAlone(final GameTestHelper helper) {
        final var player = player(helper);
        final BlockPos target = helper.absolutePos(TARGET);
        helper.setBlock(TARGET, OBBlocks.GUIDE.get());
        helper.assertTrue(use(player, target, Direction.UP).consumesAction()
                && helper.getBlockState(TARGET).getValue(OBGuideBlock.ORIENTATION) == OBGuideOrientation.ZP_YP,
                "Tool rotation around the Guide's top did not match its local turn");
        helper.assertTrue(use(player, target, Direction.EAST).consumesAction()
                && helper.getBlockState(TARGET).getValue(OBGuideBlock.ORIENTATION) == OBGuideOrientation.ZN_XN,
                "A perpendicular click did not put the Guide on its new alignment");

        final var before = helper.getBlockState(TARGET);
        final var veto = new RotationVeto(target);
        MinecraftForge.EVENT_BUS.register(veto);
        try {
            helper.assertTrue(use(player, target, Direction.UP) == InteractionResult.FAIL && veto.called && helper.getBlockState(TARGET) == before,
                    "Forge's tool modification event could not veto a real rotation");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(veto);
        }

        helper.setBlock(TARGET.below(), Blocks.STONE);
        final var door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(TARGET, door);
        helper.setBlock(TARGET.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.assertTrue(use(player, target, Direction.NORTH).consumesAction()
                && helper.getBlockState(TARGET).getValue(DoorBlock.FACING) == Direction.EAST
                && helper.getBlockState(TARGET.above()).getValue(DoorBlock.FACING) == Direction.EAST,
                "A normal rotation left the two door halves facing different ways");
        helper.succeed();
    }

    private static void addLore(final ItemStack stack, final String key) {
        final var lines = new ListTag();
        lines.add(StringTag.valueOf("{\"text\":\"Once lost, then found.\"}"));
        stack.getOrCreateTagElement("display").put(key, lines);
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "UtilityToolQA"));
        final var pos = Vec3.atBottomCenterOf(helper.absolutePos(TARGET.north(2)));
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.WRENCH.get()));
        return player;
    }

    private static InteractionResult use(final FakePlayer player, final BlockPos target, final Direction face) {
        final var hit = new BlockHitResult(Vec3.atCenterOf(target).relative(face, 0.5), face, target, false);
        return player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    public static final class RotationVeto {
        private final BlockPos target;
        private boolean called;

        private RotationVeto(final BlockPos target) { this.target = target; }

        @SubscribeEvent
        public void cancel(final BlockToolModificationEvent event) {
            if (event.getPos().equals(target) && event.getToolAction() == OBWrenchItem.ROTATE && !event.isSimulated()) {
                called = true;
                event.setCanceled(true);
            }
        }
    }
}
