package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBBigButtonBlock;
import art.arcane.openblocks.block.OBFoldingTargetBlock;
import art.arcane.openblocks.block.entity.OBBigButtonBlockEntity;
import art.arcane.openblocks.block.entity.OBFoldingTargetBlockEntity;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class RedstoneInputGameTests {

    private static final BlockPos DEVICE = new BlockPos(3, 3, 3);

    @GameTest(template = "empty")
    public static void redstoneInputsCraftAndMountOnTheClickedSurface(final GameTestHelper helper) {
        for (final Item button : new Item[] {Items.STONE_BUTTON, Items.OAK_BUTTON}) {
            final var grid = ComponentRecipeGameTests.input(helper, button, button, Items.AIR, button, button);
            helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).is(button == Items.STONE_BUTTON
                    ? OBItems.BIG_BUTTON.get() : OBItems.BIG_BUTTON_WOOD.get()), "A Big Button did not craft from four buttons");
        }
        final var targetRecipe = ComponentRecipeGameTests.input(helper, Items.WHITE_WOOL, Items.WHITE_WOOL, Items.WHITE_WOOL,
                Items.WHITE_WOOL, Items.WHITE_WOOL, Items.WHITE_WOOL, Items.STICK, Items.AIR, Items.STICK);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, targetRecipe).is(OBItems.TARGET.get()), "The legacy Target recipe was missing");

        for (final Direction facing : Direction.values()) {
            final BlockPos support = DEVICE.relative(facing.getOpposite());
            helper.setBlock(support, Blocks.STONE);
            final var player = helper.makeMockSurvivalPlayer();
            helper.assertTrue(placeItem(player, OBItems.BIG_BUTTON.get(), helper.absolutePos(support), facing),
                    "A stone Big Button could not mount facing " + facing);
            helper.assertTrue(helper.getBlockState(DEVICE).getValue(OBBigButtonBlock.FACING) == facing,
                    "The button did not face away from its support");
            helper.setBlock(DEVICE, Blocks.AIR);
            helper.setBlock(support, Blocks.AIR);
        }

        final var player = helper.makeMockSurvivalPlayer();
        player.setYRot(90);
        helper.setBlock(DEVICE.below(), Blocks.STONE);
        helper.assertTrue(!placeItem(player, OBItems.TARGET.get(), helper.absolutePos(DEVICE.below()), Direction.EAST),
                "The folding target mounted on the side of a support block");
        helper.assertTrue(placeItem(player, OBItems.TARGET.get(), helper.absolutePos(DEVICE.below()), Direction.UP),
                "Normal target placement failed");
        helper.assertTrue(helper.getBlockState(DEVICE).getValue(OBFoldingTargetBlock.FACING) == Direction.EAST
                && !helper.getBlockState(DEVICE).getValue(OBFoldingTargetBlock.POWERED), "The unpowered target ignored its placement direction");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bigButtonInventorySetsExactPulseLengthAndStrongOutput(final GameTestHelper helper) {
        final var button = placeButton(helper, DEVICE, false, Direction.NORTH);
        final var player = helper.makeMockSurvivalPlayer();
        final var abs = button.getBlockPos();
        final var center = Vec3.atCenterOf(abs);
        player.moveTo(center.x, center.y, center.z - 2, 0, 0);
        final var menu = button.createMenu(1, player.getInventory(), player);
        final var items = button.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(IllegalStateException::new);
        helper.assertTrue(menu.pulseLength() == 1 && items.getSlots() == 8, "The empty Big Button was not a one-tick, eight-slot device");
        player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 22));
        helper.assertTrue(menu.quickMoveStack(player, 8).getCount() == 22, "Shift-click did not fill the timing inventory");
        helper.assertTrue(items.insertItem(7, new ItemStack(Items.GOLD_INGOT, 13), false).isEmpty()
                && menu.pulseLength() == 35 && menu.stillValid(player), "The menu and sided inventory did not count one tick per item");
        final var loaded = new OBBigButtonBlockEntity(false, abs, button.getBlockState());
        loaded.load(button.saveWithoutMetadata());
        helper.assertTrue(loaded.pulseLength() == 35 && loaded.items().getStackInSlot(7).is(Items.GOLD_INGOT), "The timing inventory did not survive reload");

        final var level = helper.getLevel();
        helper.useBlock(DEVICE, player);
        helper.assertTrue(bigButtonPowered(helper, DEVICE) && level.getDirectSignal(abs, Direction.NORTH) == 15
                && level.getDirectSignal(abs, Direction.SOUTH) == 0 && level.getSignal(abs, Direction.UP) == 15,
                "The plate did not strongly power its support and weakly power the other faces");
        helper.runAfterDelay(34, () -> helper.assertTrue(bigButtonPowered(helper, DEVICE), "A 35-tick pulse ended early"));
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(!bigButtonPowered(helper, DEVICE), "A 35-tick pulse ran long");
            items.extractItem(0, 64, false);
            items.extractItem(7, 64, false);
            helper.useBlock(DEVICE, player);
            helper.assertTrue(bigButtonPowered(helper, DEVICE), "An empty Big Button emitted no pulse");
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(!bigButtonPowered(helper, DEVICE), "An empty Big Button emitted more than one tick");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void bigButtonReturnsItsItemsWhenSupportBreaks(final GameTestHelper helper) {
        final var button = placeButton(helper, DEVICE, true, Direction.DOWN);
        final var cap = button.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        cap.orElseThrow(IllegalStateException::new).insertItem(5, new ItemStack(Items.EMERALD, 11), false);
        helper.getLevel().destroyBlock(helper.absolutePos(DEVICE.above()), false);
        helper.assertBlockPresent(Blocks.AIR, DEVICE);
        helper.assertTrue(!cap.isPresent() && countItems(helper, DEVICE, OBItems.BIG_BUTTON_WOOD.get()) == 1
                && countItems(helper, DEVICE, Items.EMERALD) == 11, "Removing the support lost or duplicated the button or its inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void embeddedArrowHoldsWoodenButtonAndIgnoresStone(final GameTestHelper helper) {
        final BlockPos stonePos = DEVICE.east(2);
        final var wood = placeButton(helper, DEVICE, true, Direction.NORTH);
        placeButton(helper, stonePos, false, Direction.NORTH);
        wood.items().setStackInSlot(0, new ItemStack(Items.STICK, 3));
        final var woodArrow = shootAt(helper, DEVICE, 0, 0.55, new ItemStack(Items.ARROW));
        final var stoneArrow = shootAt(helper, stonePos, 0, 0.55, new ItemStack(Items.ARROW));
        helper.runAfterDelay(18, () -> {
            helper.assertTrue(bigButtonPowered(helper, DEVICE) && !bigButtonPowered(helper, stonePos),
                    "Stuck arrows did not sustain the wood pulse or incorrectly pressed stone");
            woodArrow.discard();
            stoneArrow.discard();
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(!bigButtonPowered(helper, DEVICE), "Wooden button stayed powered without its arrow");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void targetScoresActualImpactAndReturnsOnlyItsArrows(final GameTestHelper helper) {
        final var target = placeTarget(helper);
        final var level = helper.getLevel();
        final BlockPos powerPos = DEVICE.east();
        helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);
        helper.assertTrue(helper.getBlockState(DEVICE).getValue(OBFoldingTargetBlock.POWERED), "Input power did not raise the target");
        final var ordinary = shootAt(helper, DEVICE, 0, 0.55, new ItemStack(Items.ARROW));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(target.outputPower() == 15 && level.getSignal(target.getBlockPos(), Direction.SOUTH) == 15,
                    "The bullseye did not emit strength 15");
            final var tippedStack = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), Potions.POISON);
            final var tipped = shootAt(helper, DEVICE, 0.25, 0.55, tippedStack);
            final var creative = shootAt(helper, DEVICE, -0.25, 0.55, new ItemStack(Items.ARROW));
            creative.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            final var unrelated = new Arrow(EntityType.ARROW, level);
            unrelated.setPos(target.getBlockPos().getX() + 0.7, target.getBlockPos().getY() + 0.5, target.getBlockPos().getZ() + 0.8);
            unrelated.setNoGravity(true);
            unrelated.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(unrelated);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(target.outputPower() == 7, "The off-center arrow did not emit legacy strength 7");
                helper.setBlock(powerPos, Blocks.AIR);
                helper.assertTrue(!helper.getBlockState(DEVICE).getValue(OBFoldingTargetBlock.POWERED) && unrelated.isAlive(),
                        "The target latched its own output or collected an unrelated arrow");
                final var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(target.getBlockPos()).inflate(1));
                helper.assertTrue(ordinary.isRemoved() && tipped.isRemoved() && creative.isRemoved()
                        && countItems(helper, DEVICE, Items.ARROW) == 1 && countItems(helper, DEVICE, Items.TIPPED_ARROW) == 1
                        && drops.stream().anyMatch(item -> item.getItem().is(Items.TIPPED_ARROW) && PotionUtils.getPotion(item.getItem()) == Potions.POISON),
                        "Folding lost, duplicated, or changed the shots' pickup items");
                unrelated.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void targetPulseExpiresAndRejectedReturnLeavesOriginalArrow(final GameTestHelper helper) {
        final var target = placeTarget(helper);
        helper.setBlock(DEVICE.east(), Blocks.REDSTONE_BLOCK);
        final var arrow = shootAt(helper, DEVICE, 0, 0.55, new ItemStack(Items.ARROW));
        helper.runAfterDelay(5, () -> {
            final var saved = target.saveWithoutMetadata();
            final var loaded = new OBFoldingTargetBlockEntity(target.getBlockPos(), target.getBlockState());
            loaded.load(saved);
            helper.assertTrue(loaded.outputPower() == 15, "The pulse state did not survive serialization");
            final long expiry = saved.getLong("expiresAt");
            final long remaining = expiry - helper.getLevel().getGameTime();
            helper.assertTrue(remaining > 0 && remaining <= 10, "A hit had an invalid pulse deadline");
            helper.runAfterDelay(remaining - 1, () -> helper.assertTrue(target.outputPower() == 15, "The target pulse expired early"));
            helper.runAfterDelay(remaining, () -> {
                helper.assertTrue(target.outputPower() == 0 && helper.getBlockState(DEVICE).getValue(OBFoldingTargetBlock.POWERED),
                        "The target did not clear its output after ten ticks while still deployed");
                final Consumer<EntityJoinLevelEvent> protect = event -> {
                    if (event.getLevel() == helper.getLevel() && event.getEntity() instanceof ItemEntity item
                            && item.getItem().is(Items.ARROW) && item.blockPosition().equals(target.getBlockPos())) event.setCanceled(true);
                };
                MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, protect);
                try {
                    helper.setBlock(DEVICE.east(), Blocks.AIR);
                } finally {
                    MinecraftForge.EVENT_BUS.unregister(protect);
                }
                helper.assertTrue(arrow.isAlive() && countItems(helper, DEVICE, Items.ARROW) == 0,
                        "A canceled arrow return deleted or duplicated the original projectile");
                arrow.discard();
                helper.succeed();
            });
        });
    }

    private static OBFoldingTargetBlockEntity placeTarget(final GameTestHelper helper) {
        helper.setBlock(DEVICE.below(), Blocks.STONE);
        helper.setBlock(DEVICE, OBBlocks.TARGET.get());
        return (OBFoldingTargetBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(DEVICE));
    }

    private static OBBigButtonBlockEntity placeButton(final GameTestHelper helper, final BlockPos pos, final boolean wooden, final Direction facing) {
        helper.setBlock(pos.relative(facing.getOpposite()), Blocks.STONE);
        final Block block = wooden ? OBBlocks.BIG_BUTTON_WOOD.get() : OBBlocks.BIG_BUTTON.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(OBBigButtonBlock.FACING, facing));
        return (OBBigButtonBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static boolean placeItem(final Player player, final Item item, final BlockPos support, final Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        final Vec3 hitPos = Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        final var hit = new BlockHitResult(hitPos, face, support, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }

    private static boolean bigButtonPowered(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockState(pos).getValue(OBBigButtonBlock.POWERED);
    }

    private static Arrow shootAt(final GameTestHelper helper, final BlockPos pos, final double offset, final double height, final ItemStack stack) {
        final var level = helper.getLevel();
        final var target = helper.absolutePos(pos);
        final var arrow = new Arrow(EntityType.ARROW, level);
        arrow.setEffectsFromItem(stack);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        arrow.setPos(target.getX() + 0.5 + offset, target.getY() + height, target.getZ() - 2);
        arrow.setNoGravity(true);
        arrow.setDeltaMovement(0, 0, 0.8);
        level.addFreshEntity(arrow);
        return arrow;
    }

    private static int countItems(final GameTestHelper helper, final BlockPos origin, final Item item) {
        final AABB area = new AABB(helper.absolutePos(origin)).inflate(1);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, drop -> drop.getItem().is(item)).stream()
                .mapToInt(drop -> drop.getItem().getCount()).sum();
    }
}
