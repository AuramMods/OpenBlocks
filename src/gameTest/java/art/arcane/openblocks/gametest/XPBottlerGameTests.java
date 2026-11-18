package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBXPBottlerBlockEntity;
import art.arcane.openblocks.block.entity.OBXPBottlerBlockEntity.Port;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.menu.OBXPBottlerMenu;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBLiquidExperience;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class XPBottlerGameTests {

    private static final BlockPos MACHINE = new BlockPos(3, 2, 3);

    @GameTest(template = "empty")
    public static void bottlerRecipeUsesGlassAndIron(final GameTestHelper helper) {
        final var input = ComponentRecipeGameTests.input(helper,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.GLASS_BOTTLE, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, input).is(OBItems.XP_BOTTLER.get()),
                "The legacy iron and glass bottle recipe did not craft an XP Bottler");
        input.setItem(4, new ItemStack(Items.EXPERIENCE_BOTTLE));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, input).isEmpty(), "An experience bottle matched the empty bottle ingredient");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bottlerCraftsOneBottleAndSupportsShiftClick(final GameTestHelper helper) {
        final var bottler = placeBottler(helper);
        final var player = playerNear(helper, MACHINE);
        final var menu = new OBXPBottlerMenu(1, player.getInventory(), bottler);
        player.getInventory().setItem(9, new ItemStack(Items.STONE));
        helper.assertTrue(menu.quickMoveStack(player, 2).isEmpty(), "Bottler accepted stone into its input slot");
        player.getInventory().setItem(9, new ItemStack(Items.GLASS_BOTTLE, 2));
        helper.assertTrue(menu.quickMoveStack(player, 2).is(Items.GLASS_BOTTLE), "Shift-click did not move bottles into the machine");
        fluid(bottler, null).fill(OBLiquidExperience.juice(8), FluidAction.EXECUTE);

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(bottler.progress() > 0 && bottler.bottles().getStackInSlot(1).isEmpty(), "Bottler skipped the timed craft");
            helper.succeedWhen(() -> {
                final var output = bottler.bottles().getStackInSlot(1);
                helper.assertTrue(output.is(Items.EXPERIENCE_BOTTLE) && output.getCount() == 1, "Waiting for one bottle o' enchanting");
                helper.assertTrue(bottler.contents().isEmpty() && bottler.bottles().getStackInSlot(0).getCount() == 1,
                        "The craft did not charge exactly eight XP and one glass bottle");
                helper.assertTrue(menu.quickMoveStack(player, 1).is(Items.EXPERIENCE_BOTTLE)
                                && player.getInventory().contains(new ItemStack(Items.EXPERIENCE_BOTTLE)) && !menu.getSlot(1).hasItem(),
                        "The filled bottle could not be moved into the player inventory");
            });
        });
    }

    @GameTest(template = "empty")
    public static void bottlerPausesForFullOutputAndResetsWhenInputIsRemoved(final GameTestHelper helper) {
        final var bottler = placeBottler(helper);
        final FluidStack initial = OBLiquidExperience.juice(8);
        fluid(bottler, null).fill(initial, FluidAction.EXECUTE);
        bottler.bottles().setStackInSlot(0, new ItemStack(Items.GLASS_BOTTLE));
        bottler.bottles().setStackInSlot(1, new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(bottler.progress() == 0 && bottler.contents().isFluidStackIdentical(initial)
                    && bottler.bottles().getStackInSlot(0).getCount() == 1, "A full output allowed a craft to start or consumed its inputs");
            bottler.bottles().setStackInSlot(1, ItemStack.EMPTY);
        });
        helper.runAfterDelay(24, () -> {
            helper.assertTrue(bottler.progress() > 0, "Clearing the output did not start the craft");
            bottler.bottles().setStackInSlot(0, ItemStack.EMPTY);
        });
        helper.runAfterDelay(26, () -> {
            helper.assertTrue(bottler.progress() == 0 && bottler.contents().isFluidStackIdentical(initial)
                    && bottler.bottles().getStackInSlot(1).isEmpty(), "Removing the input did not cancel the current craft cleanly");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 170)
    public static void bottlerAutomatesTheDrainTankBottleChain(final GameTestHelper helper) {
        final var bottler = placeBottler(helper);
        final var player = playerNear(helper, MACHINE);
        final var menu = new OBXPBottlerMenu(1, player.getInventory(), bottler);
        final BlockPos tank = MACHINE.west();
        final BlockPos drain = tank.above();
        helper.setBlock(tank, OBBlocks.TANK.get());
        helper.setBlock(drain, OBBlocks.XP_DRAIN.get());
        helper.setBlock(MACHINE.north(), Blocks.CHEST);
        helper.setBlock(MACHINE.east(), Blocks.CHEST);
        final var source = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE.north()));
        final var receiver = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE.east()));
        source.setItem(0, new ItemStack(Items.STONE));
        source.setItem(1, new ItemStack(Items.GLASS_BOTTLE, 2));

        helper.assertTrue(!bottler.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH).isPresent()
                        && !bottler.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).isPresent(),
                "Unconfigured sides exposed automation capabilities");
        helper.assertTrue(!menu.clickMenuButton(player, 999), "The server accepted an invalid port button");
        configure(helper, menu, player, Port.INPUT, Direction.NORTH);
        configure(helper, menu, player, Port.XP, Direction.WEST);
        configure(helper, menu, player, Port.OUTPUT, Direction.EAST);
        final var outputCap = bottler.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.EAST);
        final var outputSide = outputCap.resolve().orElseThrow();
        helper.assertTrue(outputSide.getSlots() == 1 && !outputSide.insertItem(0, new ItemStack(Items.GLASS_BOTTLE), true).isEmpty(),
                "An output-only face exposed the input slot or accepted a glass bottle");
        bottler.bottles().setStackInSlot(1, new ItemStack(Items.EXPERIENCE_BOTTLE));
        helper.assertTrue(outputSide.extractItem(0, 1, false).is(Items.EXPERIENCE_BOTTLE), "The first output-side slot was not the output");
        helper.assertTrue(menu.clickMenuButton(player, OBXPBottlerMenu.sideButton(Port.OUTPUT, Direction.EAST)) && !outputCap.isPresent(),
                "Disabling a side left its cached capability active");
        menu.clickMenuButton(player, OBXPBottlerMenu.sideButton(Port.OUTPUT, Direction.EAST));
        helper.assertTrue(fluid(bottler, Direction.WEST).drain(160, FluidAction.SIMULATE).isEmpty(), "A fluid input face permitted extraction");

        final var xpPlayer = playerNear(helper, drain);
        final Vec3 grate = Vec3.atBottomCenterOf(helper.absolutePos(drain)).add(0.0, 1.0 / 16.0, 0.0);
        xpPlayer.moveTo(grate.x, grate.y, grate.z, 0.0F, 0.0F);
        xpPlayer.setNoGravity(true);
        xpPlayer.giveExperiencePoints(16);
        helper.getLevel().addFreshEntity(xpPlayer);
        helper.succeedWhen(() -> {
            final ItemStack output = receiver.getItem(0);
            helper.assertTrue(output.is(Items.EXPERIENCE_BOTTLE) && output.getCount() == 2, "Waiting for two automated XP bottles");
            helper.assertTrue(source.getItem(0).is(Items.STONE) && source.getItem(1).isEmpty() && bottler.contents().isEmpty()
                    && xpPlayer.experienceLevel == 0 && OBLiquidExperience.playerPoints(xpPlayer) == 0
                    && fluid(helper.getLevel().getBlockEntity(helper.absolutePos(tank)), null).getFluidInTank(0).isEmpty(),
                    "The drain to bottler chain lost or duplicated XP or bottles");
            xpPlayer.discard();
        });
    }

    @GameTest(template = "empty")
    public static void bottlerPreservesProgressAndConfigurationAndDropsItsInventory(final GameTestHelper helper) {
        final var bottler = placeBottler(helper);
        final var player = playerNear(helper, MACHINE);
        final var menu = new OBXPBottlerMenu(1, player.getInventory(), bottler);
        configure(helper, menu, player, Port.XP, Direction.UP);
        fluid(bottler, Direction.UP).fill(OBLiquidExperience.juice(8), FluidAction.EXECUTE);
        bottler.bottles().setStackInSlot(0, new ItemStack(Items.GLASS_BOTTLE, 3));
        bottler.bottles().setStackInSlot(1, new ItemStack(Items.EXPERIENCE_BOTTLE, 2));
        final var absolute = helper.absolutePos(MACHINE);
        helper.runAfterDelay(12, () -> {
            final var saved = bottler.saveWithFullMetadata();
            final var decoded = (OBXPBottlerBlockEntity) BlockEntity.loadStatic(absolute, helper.getBlockState(MACHINE), saved);
            helper.assertTrue(decoded != null && decoded.progress() > 0 && decoded.progress() == bottler.progress()
                            && decoded.settings(Port.XP) == bottler.settings(Port.XP) && decoded.contents().isFluidStackIdentical(bottler.contents())
                            && decoded.bottles().getStackInSlot(0).getCount() == 3 && decoded.bottles().getStackInSlot(1).getCount() == 2,
                    "A saved bottler lost inventory, fluid, progress, or settings");
            final var cap = bottler.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP);
            helper.getLevel().destroyBlock(absolute, true);
            helper.assertTrue(!cap.isPresent() && !menu.clickMenuButton(player, OBXPBottlerMenu.automaticButton(Port.XP)),
                    "A removed bottler accepted menu or capability use");
            final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1));
            helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(Items.GLASS_BOTTLE)).mapToInt(drop -> drop.getItem().getCount()).sum() == 3
                            && drops.stream().filter(drop -> drop.getItem().is(Items.EXPERIENCE_BOTTLE)).mapToInt(drop -> drop.getItem().getCount()).sum() == 2,
                    "Breaking the bottler lost or doubled stored bottles");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void bottlerSupportsAlternateFluidsAndFullWidthMenuValues(final GameTestHelper helper) {
        final var previous = OBServerConfig.XP_ADDITIONAL_FLUIDS.get();
        OBServerConfig.XP_ADDITIONAL_FLUIDS.set(List.of("minecraft:water=12500"));
        try {
            final var bottler = placeBottler(helper);
            final var player = playerNear(helper, MACHINE);
            final var absolute = helper.absolutePos(MACHINE);
            final var state = helper.getBlockState(MACHINE);
            final var server = new OBXPBottlerMenu(1, player.getInventory(), bottler);
            final var client = new OBXPBottlerMenu(1, player.getInventory());
            configure(helper, server, player, Port.XP, Direction.UP);
            final int amount = fluid(bottler, Direction.UP).fill(new FluidStack(Fluids.WATER, 100000), FluidAction.EXECUTE);
            helper.assertTrue(amount == 100000 && bottler.capacity() == 100000, "The configured XP fluid was omitted from the recipe capacity");
            bottler.bottles().setStackInSlot(0, new ItemStack(Items.GLASS_BOTTLE));

            server.setSynchronizer(new ContainerSynchronizer() {
                @Override
                public void sendInitialData(final AbstractContainerMenu menu, final NonNullList<ItemStack> items,
                        final ItemStack carried, final int[] data) {
                    for (int i = 0; i < data.length; i++) sendDataChange(menu, i, data[i]);
                }

                @Override public void sendSlotChange(final AbstractContainerMenu menu, final int slot, final ItemStack stack) {}

                @Override public void sendCarriedChange(final AbstractContainerMenu menu, final ItemStack carried) {}

                @Override
                public void sendDataChange(final AbstractContainerMenu menu, final int slot, final int value) {
                    final var packet = new ClientboundContainerSetDataPacket(menu.containerId, slot, value);
                    final var bytes = new FriendlyByteBuf(Unpooled.buffer());
                    try {
                        packet.write(bytes);
                        final var received = new ClientboundContainerSetDataPacket(bytes);
                        client.setData(received.getId(), received.getValue());
                    } finally {
                        bytes.release();
                    }
                }
            });
            helper.assertTrue(client.capacity() == 100000 && client.fluid().getAmount() == 100000 && client.fluid().getFluid() == Fluids.WATER
                            && client.isSideEnabled(Port.XP, Direction.UP) && client.isAutomatic(Port.XP),
                    "Menu data was truncated, mislabeled, or lost during packet serialization");
            for (int tick = 0; tick < OBXPBottlerBlockEntity.PROCESS_TICKS; tick++) {
                OBXPBottlerBlockEntity.serverTick(helper.getLevel(), absolute, state, bottler);
            }
            helper.assertTrue(bottler.progress() == 40 && bottler.bottles().getStackInSlot(1).isEmpty(), "The craft finished before its progress bar filled");
            OBXPBottlerBlockEntity.serverTick(helper.getLevel(), absolute, state, bottler);
            helper.assertTrue(bottler.contents().isEmpty() && bottler.bottles().getStackInSlot(1).is(Items.EXPERIENCE_BOTTLE),
                    "The bottler did not use the configured 100,000 mB to craft once");
        } finally {
            OBServerConfig.XP_ADDITIONAL_FLUIDS.set(previous);
        }
        helper.succeed();
    }

    private static void configure(final GameTestHelper helper, final OBXPBottlerMenu menu, final Player player, final Port port, final Direction side) {
        helper.assertTrue(menu.clickMenuButton(player, OBXPBottlerMenu.sideButton(port, side))
                && menu.clickMenuButton(player, OBXPBottlerMenu.automaticButton(port)), "The menu did not accept the port configuration");
    }

    private static IFluidHandler fluid(final BlockEntity blockEntity, final Direction side) {
        return blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new);
    }

    private static OBXPBottlerBlockEntity placeBottler(final GameTestHelper helper) {
        helper.setBlock(MACHINE, OBBlocks.XP_BOTTLER.get());
        return (OBXPBottlerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE));
    }

    private static Player playerNear(final GameTestHelper helper, final BlockPos pos) {
        final Player player = helper.makeMockSurvivalPlayer();
        final var center = Vec3.atBottomCenterOf(helper.absolutePos(pos));
        player.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        return player;
    }
}
