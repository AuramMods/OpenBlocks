package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBXpWorkbenchBlock.Anvil;
import art.arcane.openblocks.block.entity.OBXpWorkbenchBlockEntity;
import art.arcane.openblocks.block.entity.OBXpWorkbenchBlockEntity.Port;
import art.arcane.openblocks.menu.OBXpWorkbenchMenu;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBLiquidExperience;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class XpWorkbenchGameTests {

    private static final BlockPos MACHINE = new BlockPos(3, 3, 3);

    @GameTest(template = "empty")
    public static void xpWorkbenchesCraftAndKeepLegacyCapacities(final GameTestHelper helper) {
        assertRecipe(helper, Items.ANVIL, OBItems.AUTO_ANVIL.get());
        assertRecipe(helper, Items.ENCHANTING_TABLE, OBItems.AUTO_ENCHANTMENT_TABLE.get());
        final var anvil = place(helper, OBBlocks.AUTO_ANVIL.get());
        helper.assertTrue(anvil.capacity() == 80400 && anvil.items().getSlots() == 3, "Auto Anvil capacity was not 45 levels and three slots");
        final var state = helper.getBlockState(MACHINE);
        helper.assertTrue(state.getValue(Anvil.AXIS) == Direction.Axis.Z
                && state.rotate(Rotation.CLOCKWISE_90).getValue(Anvil.AXIS) == Direction.Axis.X, "Anvil rotation did not turn the long axis");
        final var table = place(helper, OBBlocks.AUTO_ENCHANTMENT_TABLE.get());
        helper.assertTrue(table.capacity() == 27900 && table.tier() == 1 && table.powerLimit() == 1,
                "Auto Enchantment Table storage or defaults were incorrect");
        helper.assertTrue(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(table.getBlockState()), "A basic pickaxe cannot harvest the workbench");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void autoAnvilSpendsExactXpAndMaterialAfterItsCooldown(final GameTestHelper helper) {
        final var anvil = place(helper, OBBlocks.AUTO_ANVIL.get());
        final var pickaxe = damaged(Items.DIAMOND_PICKAXE, 800);
        pickaxe.setHoverName(Component.literal("Old Pickaxe"));
        set(anvil, Port.TOOL, pickaxe);
        set(anvil, Port.REAGENT, new ItemStack(Items.DIAMOND, 4));
        final int repairBill = OBLiquidExperience.juice(OBLiquidExperience.pointsAtLevel(4)).getAmount();
        final var tank = fluid(anvil, null);
        tank.fill(juice(repairBill - 1), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(get(anvil, Port.OUTPUT).isEmpty() && get(anvil, Port.TOOL).getDamageValue() == 800
                            && get(anvil, Port.REAGENT).getCount() == 4 && anvil.contents().getAmount() == repairBill - 1,
                    "A short payment spent XP or materials");
            tank.fill(juice(201), IFluidHandler.FluidAction.EXECUTE);
            helper.runAfterDelay(12, () -> helper.assertTrue(get(anvil, Port.OUTPUT).isEmpty(), "The anvil skipped its retry cooldown"));
            helper.runAfterDelay(45, () -> {
                final ItemStack output = get(anvil, Port.OUTPUT);
                helper.assertTrue(output.is(Items.DIAMOND_PICKAXE) && output.getDamageValue() == 0 && !output.hasCustomHoverName()
                                && output.getBaseRepairCost() == 1, "The anvil did not apply vanilla repair and legacy name reset");
                helper.assertTrue(get(anvil, Port.TOOL).isEmpty() && get(anvil, Port.REAGENT).getCount() == 1
                                && anvil.contents().getAmount() == 200, "Repair charged the wrong XP or number of diamonds");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void autoAnvilHonorsForgeVetoAndCustomRepairResult(final GameTestHelper helper) {
        final var anvil = place(helper, OBBlocks.AUTO_ANVIL.get());
        final Vec3 center = Vec3.atCenterOf(helper.absolutePos(MACHINE));
        final var callbacks = new ArrayList<AnvilUpdateEvent>();
        final Consumer<AnvilUpdateEvent> customize = event -> {
            final Player player = event.getPlayer();
            if (!(player instanceof FakePlayer) || player.level() != helper.getLevel() || player.distanceToSqr(center) > 0.01) return;
            callbacks.add(event);
            if (callbacks.size() == 1) event.setCanceled(true);
            else {
                event.setOutput(new ItemStack(Items.QUARTZ));
                event.setCost(2);
                event.setMaterialCost(1);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AnvilUpdateEvent.class, customize);
        set(anvil, Port.TOOL, new ItemStack(Items.IRON_INGOT));
        set(anvil, Port.REAGENT, new ItemStack(Items.STICK, 2));
        fluid(anvil, null).fill(OBLiquidExperience.juice(23), IFluidHandler.FluidAction.EXECUTE);
        final boolean[] vetoPreserved = {false};
        helper.runAfterDelay(5, () -> vetoPreserved[0] = callbacks.size() == 1 && get(anvil, Port.OUTPUT).isEmpty()
                && get(anvil, Port.TOOL).is(Items.IRON_INGOT) && get(anvil, Port.REAGENT).getCount() == 2
                && anvil.contents().getAmount() == 460);
        helper.runAfterDelay(52, () -> {
            MinecraftForge.EVENT_BUS.unregister(customize);
            helper.assertTrue(vetoPreserved[0] && callbacks.size() == 2, "The first Forge veto changed the inputs, or the anvil evaluated extra previews");
            helper.assertTrue(get(anvil, Port.OUTPUT).is(Items.QUARTZ) && get(anvil, Port.REAGENT).getCount() == 1
                    && anvil.contents().getAmount() == 140, "The Forge recipe result or costs were ignored");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void autoAnvilPullsIngredientsAndXpAndPushesTheOutput(final GameTestHelper helper) {
        final var anvil = place(helper, OBBlocks.AUTO_ANVIL.get());
        final var player = playerNear(helper);
        final var menu = anvil.createMenu(1, player.getInventory(), player);
        helper.assertTrue(!anvil.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST).isPresent(), "An unconfigured machine accepted sided items");
        configure(helper, menu, player, Port.TOOL, Direction.WEST);
        configure(helper, menu, player, Port.REAGENT, Direction.NORTH);
        configure(helper, menu, player, Port.OUTPUT, Direction.EAST);
        configure(helper, menu, player, Port.XP, Direction.DOWN);
        final var inputFace = anvil.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST);
        final var input = inputFace.orElseThrow(IllegalStateException::new);
        menu.clickMenuButton(player, OBXpWorkbenchMenu.sideButton(Port.TOOL, Direction.WEST));
        helper.assertTrue(!inputFace.isPresent() && !input.insertItem(0, damaged(Items.IRON_PICKAXE, 60), false).isEmpty(),
                "A removed face kept accepting items through a cached capability");
        menu.clickMenuButton(player, OBXpWorkbenchMenu.sideButton(Port.TOOL, Direction.WEST));
        helper.assertTrue(fluid(anvil, Direction.DOWN).drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "XP port supports draining");

        final var tool = chest(helper, MACHINE.west());
        final var material = chest(helper, MACHINE.north());
        final var output = chest(helper, MACHINE.east());
        tool.setItem(0, damaged(Items.IRON_PICKAXE, 60));
        material.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
        helper.setBlock(MACHINE.below(), OBBlocks.TANK.get());
        final var source = fluid(helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE.below())), Direction.UP);
        source.fill(OBLiquidExperience.juice(10), IFluidHandler.FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            helper.assertTrue(output.getItem(0).is(Items.IRON_PICKAXE), "Waiting for the repaired item to reach the chest");
            helper.assertTrue(output.getItem(0).getDamageValue() == 0 && tool.getItem(0).isEmpty() && material.getItem(0).isEmpty()
                            && get(anvil, Port.REAGENT).getCount() == 1 && anvil.contents().getAmount() == 60
                            && source.getFluidInTank(0).isEmpty(), "Automated repair lost or duplicated ingredients or XP");
        });
    }

    @GameTest(template = "empty")
    public static void autoEnchantingChecksPowerLapisAndXpBeforeEnchantingABook(final GameTestHelper helper) {
        final var table = place(helper, OBBlocks.AUTO_ENCHANTMENT_TABLE.get());
        final var player = playerNear(helper);
        final var menu = table.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu.clickMenuButton(player, OBXpWorkbenchMenu.tierButton(3)) && table.tier() == 3, "Tier control failed");
        helper.setBlock(MACHINE.east(2), Blocks.BOOKSHELF);
        helper.setBlock(MACHINE.east(), Blocks.STONE);
        final var offered = new ArrayList<EnchantmentLevelSetEvent>();
        final BlockPos target = helper.absolutePos(MACHINE);
        final Consumer<EnchantmentLevelSetEvent> quote = event -> {
            if (event.getLevel() == helper.getLevel() && event.getPos().equals(target)) {
                offered.add(event);
                event.setEnchantLevel(20);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EnchantmentLevelSetEvent.class, quote);
        final var book = new ItemStack(Items.BOOK, 2);
        book.setHoverName(Component.literal("Field Notes"));
        player.getInventory().setItem(9, book);
        final boolean movedBook = menu.quickMoveStack(player, 3).is(Items.BOOK) && player.getInventory().getItem(9).getCount() == 1;
        set(table, Port.REAGENT, new ItemStack(Items.LAPIS_LAZULI, 2));
        final int threshold = OBLiquidExperience.juice(OBLiquidExperience.pointsAtLevel(20)).getAmount();
        fluid(table, null).fill(juice(threshold - 1), IFluidHandler.FluidAction.EXECUTE);
        final boolean[] checked = {false, false};
        helper.runAfterDelay(24, () -> {
            checked[0] = movedBook && table.availablePower() == 0 && offered.isEmpty() && get(table, Port.OUTPUT).isEmpty();
            helper.setBlock(MACHINE.east(), Blocks.AIR);
        });
        helper.runAfterDelay(48, () -> {
            checked[1] = table.availablePower() == 1 && offered.isEmpty() && get(table, Port.OUTPUT).isEmpty();
            set(table, Port.REAGENT, new ItemStack(Items.LAPIS_LAZULI, 4));
        });
        final boolean[] refusedShortXp = {false};
        helper.runAfterDelay(53, () -> {
            refusedShortXp[0] = get(table, Port.OUTPUT).isEmpty() && get(table, Port.REAGENT).getCount() == 4
                    && table.contents().getAmount() == threshold - 1 && !offered.isEmpty();
            fluid(table, null).fill(juice(1), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(60, () -> {
            MinecraftForge.EVENT_BUS.unregister(quote);
            final var result = get(table, Port.OUTPUT);
            helper.assertTrue(checked[0] && checked[1] && refusedShortXp[0], "Shelf obstruction, missing lapis, or insufficient XP did not block work");
            helper.assertTrue(offered.stream().allMatch(event -> event.getPower() == 1 && event.getEnchantRow() == 2), "Forge saw the wrong shelf power or tier");
            helper.assertTrue(result.is(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantments(result).isEmpty()
                            && result.getHoverName().getString().equals("Field Notes"), "Enchanting failed to turn the book into a named enchanted book");
            helper.assertTrue(get(table, Port.TOOL).isEmpty() && get(table, Port.REAGENT).getCount() == 1
                            && table.contents().getAmount() == OBLiquidExperience.juice(OBLiquidExperience.pointsAtLevel(17)).getAmount(),
                    "Enchanting charged the threshold, or charged the wrong number of levels or lapis");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void autoEnchantingPersistsSettingsSeedAndInventoryAndDropsStock(final GameTestHelper helper) {
        final var table = place(helper, OBBlocks.AUTO_ENCHANTMENT_TABLE.get());
        final var player = playerNear(helper);
        final var menu = table.createMenu(1, player.getInventory(), player);
        configure(helper, menu, player, Port.XP, Direction.UP);
        configure(helper, menu, player, Port.OUTPUT, Direction.WEST);
        helper.assertTrue(menu.clickMenuButton(player, OBXpWorkbenchMenu.powerButton(15))
                && menu.clickMenuButton(player, OBXpWorkbenchMenu.tierButton(2))
                && !menu.clickMenuButton(player, OBXpWorkbenchMenu.powerButton(31)), "The menu ignored or failed to bound the slider and tier buttons");
        final ItemStack reagent = new ItemStack(Items.LAPIS_LAZULI, 6);
        set(table, Port.REAGENT, reagent);
        fluid(table, Direction.UP).fill(OBLiquidExperience.juice(75), IFluidHandler.FluidAction.EXECUTE);
        final var tag = table.saveWithFullMetadata();
        final var restored = (OBXpWorkbenchBlockEntity) BlockEntity.loadStatic(helper.absolutePos(MACHINE), table.getBlockState(), tag);
        helper.assertTrue(restored != null && restored.saveWithFullMetadata().getLong("seed") == tag.getLong("seed")
                && restored.tier() == 2 && restored.powerLimit() == 15 && restored.contents().isFluidStackIdentical(OBLiquidExperience.juice(75))
                && ItemStack.matches(get(restored, Port.REAGENT), reagent) && restored.settings(Port.XP) == table.settings(Port.XP)
                && restored.settings(Port.OUTPUT) == table.settings(Port.OUTPUT), "Reloading lost machine state");
        final var cap = table.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP);
        helper.getLevel().destroyBlock(helper.absolutePos(MACHINE), true);
        final int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MACHINE)).inflate(1)).stream()
                .filter(item -> item.getItem().is(Items.LAPIS_LAZULI)).mapToInt(item -> item.getItem().getCount()).sum();
        helper.assertTrue(!cap.isPresent() && !menu.stillValid(player) && dropped == 6, "Breaking the table lost stock or left a live menu or capability");
        helper.succeed();
    }

    private static void assertRecipe(final GameTestHelper helper, final Item center, final Item result) {
        final var recipe = ComponentRecipeGameTests.input(helper, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.IRON_INGOT, center, Items.IRON_INGOT, Items.REDSTONE, Items.REDSTONE, Items.REDSTONE);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, recipe).is(result), "Missing recipe for " + result);
    }

    private static OBXpWorkbenchBlockEntity place(final GameTestHelper helper, final Block block) {
        helper.setBlock(MACHINE, block);
        return (OBXpWorkbenchBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE));
    }

    private static ChestBlockEntity chest(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static Player playerNear(final GameTestHelper helper) {
        final var player = helper.makeMockSurvivalPlayer();
        final Vec3 pos = Vec3.atCenterOf(helper.absolutePos(MACHINE)).add(0, 0, -2);
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static void configure(final GameTestHelper helper, final OBXpWorkbenchMenu menu, final Player player, final Port port, final Direction face) {
        helper.assertTrue(menu.clickMenuButton(player, OBXpWorkbenchMenu.sideButton(port, face))
                && menu.clickMenuButton(player, OBXpWorkbenchMenu.autoButton(port)), "The menu did not configure " + port);
    }

    private static ItemStack damaged(final Item item, final int damage) {
        final var stack = new ItemStack(item);
        stack.setDamageValue(damage);
        return stack;
    }

    private static void set(final OBXpWorkbenchBlockEntity machine, final Port slot, final ItemStack stack) {
        machine.items().setStackInSlot(slot.ordinal(), stack);
    }

    private static ItemStack get(final OBXpWorkbenchBlockEntity machine, final Port slot) {
        return machine.items().getStackInSlot(slot.ordinal());
    }

    private static IFluidHandler fluid(final BlockEntity entity, final Direction face) {
        return entity.getCapability(ForgeCapabilities.FLUID_HANDLER, face).orElseThrow(IllegalStateException::new);
    }

    private static FluidStack juice(final int millibuckets) {
        return new FluidStack(OBLiquidExperience.juice(1), millibuckets);
    }
}
