package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBVacuumHopperBlock;
import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity;
import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity.Output;
import art.arcane.openblocks.menu.OBVacuumHopperMenu;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBFluids;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBLiquidExperience;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class VacuumHopperGameTests {

    private static final BlockPos HOPPER = new BlockPos(3, 2, 3);

    @GameTest(template = "empty")
    public static void vacuumHopperRecipeAndCapacityMatchLegacy(final GameTestHelper helper) {
        final var ingredients = ComponentRecipeGameTests.input(helper, Items.HOPPER, Items.OBSIDIAN, Items.ENDER_EYE);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, ingredients).is(OBItems.VACUUM_HOPPER.get()), "The legacy recipe was missing");
        final var hopper = placeHopper(helper);
        helper.assertTrue(hopper.capacity() == OBLiquidExperience.juice(55).getAmount() && hopper.items().getSlots() == 10,
                "The machine did not provide ten slots and five levels of Liquid XP storage");
        helper.assertTrue(hopper.outputMask(Output.ITEMS) == 0 && hopper.outputMask(Output.XP) == 0, "A new hopper exposed outputs by default");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vacuumHopperAttractsItemsAndExperience(final GameTestHelper helper) {
        final var hopper = placeHopper(helper);
        final Vec3 center = Vec3.atCenterOf(helper.absolutePos(HOPPER));
        final var item = spawnItem(helper, center.add(2.2, 0.0, 0.0), new ItemStack(Items.IRON_INGOT, 3));
        final var orb = spawnOrb(helper, center.add(0.0, 0.0, -2.2), 7, 1);
        helper.runAfterDelay(5, () -> helper.assertTrue(item.getX() < center.x + 2.2 && orb.getZ() > center.z - 2.2,
                "The hopper did not pull entities from its suction range"));
        helper.succeedWhen(() -> {
            helper.assertTrue(item.isRemoved() && orb.isRemoved(), "Waiting for the item and orb to reach the hopper");
            helper.assertTrue(hopper.items().getStackInSlot(0).is(Items.IRON_INGOT) && hopper.items().getStackInSlot(0).getCount() == 3
                            && hopper.contents().isFluidStackIdentical(OBLiquidExperience.juice(7)),
                    "Collected entities did not reach the inventory and XP tank exactly once");
        });
    }

    @GameTest(template = "empty")
    public static void vacuumHopperPreservesUncollectedItemAndOrbRemainders(final GameTestHelper helper) {
        final var hopper = placeHopper(helper);
        for (int slot = 0; slot < hopper.items().getSlots(); slot++) hopper.items().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
        hopper.items().setStackInSlot(9, new ItemStack(Items.IRON_INGOT, 62));
        fluid(hopper, null).fill(new FluidStack(OBFluids.XP_JUICE.get(), hopper.capacity() - 140), FluidAction.EXECUTE);
        final Vec3 top = Vec3.atCenterOf(helper.absolutePos(HOPPER)).add(0.0, 0.7, 0.0);
        final var item = spawnItem(helper, top, new ItemStack(Items.IRON_INGOT, 5));
        final var orb = spawnOrb(helper, top, 3, 3);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(!item.isRemoved() && item.getItem().getCount() == 3 && hopper.items().getStackInSlot(9).getCount() == 64,
                    "Partial pickup lost or duplicated ingots");
            helper.assertTrue(!orb.isRemoved() && orb.count == 1 && hopper.contents().getAmount() == hopper.capacity() - 20,
                    "Partial pickup lost an uncollected XP constituent");
            item.discard();
            orb.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void vacuumHopperRoutesItemsAndXpOnlyToConfiguredFaces(final GameTestHelper helper) {
        final var hopper = placeHopper(helper);
        final Player player = playerNear(helper);
        final var menu = new OBVacuumHopperMenu(1, player.getInventory(), hopper);
        final var chest = placeChest(helper, HOPPER.east());
        final var tank = placeTank(helper, HOPPER.north());
        final var unconfigured = placeTank(helper, HOPPER.south());
        helper.assertTrue(!menu.clickMenuButton(player, 12), "An invalid output control changed state");
        configure(helper, menu, player, Output.ITEMS, Direction.EAST);
        configure(helper, menu, player, Output.XP, Direction.NORTH);
        configure(helper, menu, player, Output.XP, Direction.EAST);
        final var itemCapability = hopper.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.EAST);
        final var fluidCapability = hopper.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.EAST);
        helper.assertTrue(itemCapability.isPresent() && fluidCapability.isPresent(), "The same face could not expose both outputs");
        helper.assertTrue(!itemCapability.resolve().orElseThrow().insertItem(0, new ItemStack(Items.STONE), false).isEmpty()
                && fluidCapability.resolve().orElseThrow().fill(OBLiquidExperience.juice(1), FluidAction.EXECUTE) == 0,
                "An output accepted an insertion");
        configure(helper, menu, player, Output.XP, Direction.EAST);
        helper.assertTrue(!fluidCapability.isPresent() && itemCapability.isPresent(), "Changing Liquid XP access invalidated the wrong capability");

        hopper.items().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 9));
        fluid(hopper, null).fill(OBLiquidExperience.juice(8), FluidAction.EXECUTE);
        helper.runAfterDelay(9, () -> helper.assertTrue(tank.contents().getAmount() <= 50, "The hopper exceeded its 50 mB transfer budget"));
        helper.succeedWhen(() -> {
            helper.assertTrue(chest.getItem(0).is(Items.IRON_INGOT) && chest.getItem(0).getCount() == 9, "The chest did not receive the stored ingots");
            helper.assertTrue(tank.contents().isFluidStackIdentical(OBLiquidExperience.juice(8)), "Waiting for all the XP fluid to reach the configured tank");
            helper.assertTrue(unconfigured.contents().isEmpty() && hopper.contents().isEmpty() && hopper.items().getStackInSlot(0).isEmpty(),
                    "An unconfigured face received XP or output was duplicated");
        });
    }

    @GameTest(template = "empty")
    public static void vacuumHopperSneakTogglePausesSuctionAndActiveOutput(final GameTestHelper helper) {
        final var hopper = placeHopper(helper);
        final Player player = playerNear(helper);
        final var menu = new OBVacuumHopperMenu(1, player.getInventory(), hopper);
        final var chest = placeChest(helper, HOPPER.west());
        configure(helper, menu, player, Output.ITEMS, Direction.WEST);
        final var pos = helper.absolutePos(HOPPER);
        final var center = Vec3.atCenterOf(pos);
        final var hit = new BlockHitResult(center, Direction.SOUTH, pos, false);
        player.setShiftKeyDown(true);
        helper.getBlockState(HOPPER).use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(!menu.isEnabled(), "Empty-hand sneak-use did not pause the hopper");
        final var remote = spawnItem(helper, center.add(2, 0, 0), new ItemStack(Items.IRON_INGOT));
        final var contact = spawnItem(helper, center, new ItemStack(Items.IRON_INGOT));
        helper.getBlockState(HOPPER).entityInside(helper.getLevel(), pos, contact);
        helper.assertTrue(contact.isRemoved(), "The paused hopper did not retain direct contact pickup");
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(remote.position().equals(center.add(2, 0, 0)) && chest.getItem(0).isEmpty()
                    && hopper.items().getStackInSlot(0).getCount() == 1, "Pause failed to stop suction and automatic output");
            helper.getBlockState(HOPPER).use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(menu.isEnabled(), "Sneak-use did not resume the hopper");
            helper.succeedWhen(() -> helper.assertTrue(remote.isRemoved() && chest.getItem(0).getCount() == 2,
                    "Resuming the hopper did not collect and eject both items"));
        });
    }

    @GameTest(template = "empty")
    public static void vacuumHopperSavesConfigurationAndDropsInventory(final GameTestHelper helper) {
        final var hopper = placeHopper(helper);
        final Player player = playerNear(helper);
        final var menu = new OBVacuumHopperMenu(1, player.getInventory(), hopper);
        final var pos = helper.absolutePos(HOPPER);
        configure(helper, menu, player, Output.ITEMS, Direction.UP);
        configure(helper, menu, player, Output.XP, Direction.DOWN);
        fluid(hopper, null).fill(OBLiquidExperience.juice(20), FluidAction.EXECUTE);
        hopper.items().setStackInSlot(9, new ItemStack(Items.EMERALD, 4));
        final var state = helper.getBlockState(HOPPER).setValue(OBVacuumHopperBlock.ENABLED, false);
        helper.setBlock(HOPPER, state);
        final var snapshot = hopper.saveWithFullMetadata();
        final var restored = (OBVacuumHopperBlockEntity) BlockEntity.loadStatic(pos, state, snapshot);
        helper.assertTrue(restored != null && !restored.getBlockState().getValue(OBVacuumHopperBlock.ENABLED)
                        && restored.outputMask(Output.ITEMS) == hopper.outputMask(Output.ITEMS)
                        && restored.outputMask(Output.XP) == hopper.outputMask(Output.XP)
                        && restored.contents().isFluidStackIdentical(hopper.contents())
                        && restored.items().getStackInSlot(9).getCount() == 4,
                "Reloading lost vacuum settings or contents");
        final var clientState = hopper.getUpdateTag();
        helper.assertTrue(clientState.getInt("itemOutputs") == 1 << Direction.UP.ordinal()
                        && clientState.getInt("xpOutputs") == 1 << Direction.DOWN.ordinal(), "The model update omitted sided output settings");
        final var capability = hopper.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        helper.getLevel().destroyBlock(pos, true);
        helper.assertTrue(!capability.isPresent() && !menu.stillValid(player), "The removed hopper still exposed its menu or item capability");
        final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0));
        helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(Items.EMERALD)).mapToInt(drop -> drop.getItem().getCount()).sum() == 4,
                "Breaking the machine lost or duplicated inventory");
        helper.succeed();
    }

    private static OBVacuumHopperBlockEntity placeHopper(final GameTestHelper helper) {
        helper.setBlock(HOPPER, OBBlocks.VACUUM_HOPPER.get());
        return (OBVacuumHopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(HOPPER));
    }

    private static IFluidHandler fluid(final BlockEntity entity, final Direction side) {
        return entity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new);
    }

    private static OBTankBlockEntity placeTank(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, OBBlocks.TANK.get());
        return (OBTankBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static ChestBlockEntity placeChest(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static Player playerNear(final GameTestHelper helper) {
        final var player = helper.makeMockSurvivalPlayer();
        final Vec3 pos = Vec3.atCenterOf(helper.absolutePos(HOPPER)).add(0, 0, 2);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static void configure(final GameTestHelper helper, final OBVacuumHopperMenu menu, final Player player,
            final Output output, final Direction direction) {
        helper.assertTrue(menu.clickMenuButton(player, OBVacuumHopperMenu.sideButton(output, direction)), "The menu rejected a valid output side");
    }

    private static ItemEntity spawnItem(final GameTestHelper helper, final Vec3 position, final ItemStack stack) {
        final var item = new ItemEntity(helper.getLevel(), position.x, position.y, position.z, stack, 0.0, 0.0, 0.0);
        item.setNoGravity(true);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    private static ExperienceOrb spawnOrb(final GameTestHelper helper, final Vec3 position, final int value, final int count) {
        final var orb = new ExperienceOrb(helper.getLevel(), position.x, position.y, position.z, value);
        orb.count = count;
        orb.setDeltaMovement(Vec3.ZERO);
        orb.setNoGravity(true);
        helper.getLevel().addFreshEntity(orb);
        return orb;
    }
}
