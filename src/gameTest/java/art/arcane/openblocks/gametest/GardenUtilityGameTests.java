package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBDonationStationBlock;
import art.arcane.openblocks.block.OBSprinklerBlock;
import art.arcane.openblocks.block.entity.OBDonationStationBlockEntity;
import art.arcane.openblocks.block.entity.OBSprinklerBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBDonationMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class GardenUtilityGameTests {

    private static final BlockPos MACHINE = new BlockPos(3, 3, 3);

    @GameTest(template = "empty")
    public static void gardenMachinesCraftAndSprinklerDropsWithItsSupport(final GameTestHelper helper) {
        final var sprinklerRecipe = ComponentRecipeGameTests.input(helper, Items.IRON_INGOT, Items.IRON_BARS, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.REDSTONE_TORCH, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_BARS, Items.IRON_INGOT);
        final var stationRecipe = ComponentRecipeGameTests.input(helper, Items.PORKCHOP, Items.PORKCHOP, Items.PORKCHOP,
                Items.PORKCHOP, Items.CHEST, Items.PORKCHOP, Items.PORKCHOP, Items.PORKCHOP, Items.PORKCHOP);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, sprinklerRecipe).is(OBItems.SPRINKLER.get())
                && ComponentRecipeGameTests.craft(helper, stationRecipe).is(OBItems.DONATION_STATION.get()), "Legacy crafting recipes were missing");

        final var player = playerNear(helper);
        player.setYRot(-90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.SPRINKLER.get()));
        helper.setBlock(MACHINE.below(), Blocks.STONE);
        final var surface = helper.absolutePos(MACHINE.below());
        final var hit = new BlockHitResult(Vec3.atCenterOf(surface).add(0, 0.5, 0), Direction.UP, surface, false);
        helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "An actual sprinkler stack did not place on solid support");
        final var sprinkler = (OBSprinklerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE));
        final var cap = sprinkler.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST);
        helper.assertTrue(sprinkler.getBlockState().getValue(OBSprinklerBlock.AXIS) == Direction.Axis.X,
                "The placed sprinkler ignored the player's horizontal direction");
        sprinkler.fertilizer().setStackInSlot(8, new ItemStack(Items.BONE_MEAL, 5));
        helper.getLevel().destroyBlock(surface, false);
        helper.assertBlockPresent(Blocks.AIR, MACHINE);
        helper.assertTrue(!cap.isPresent() && countDrops(helper, Items.BONE_MEAL) == 5 && countDrops(helper, OBItems.SPRINKLER.get()) == 1,
                "Support loss erased or duplicated the sprinkler or its fertilizer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sprinklerConsumesOneMillibucketPerWaterCycleAndKeepsDryBoneMeal(final GameTestHelper helper) {
        final var sprinkler = placeSprinkler(helper);
        final var fluid = fluid(sprinkler, Direction.EAST);
        final var player = playerNear(helper);
        final var menu = sprinkler.createMenu(1, player.getInventory(), player);
        helper.assertTrue(fluid.getTankCapacity(0) == 50 && menu.slots.size() == 45
                && fluid.fill(new FluidStack(Fluids.LAVA, 30), IFluidHandler.FluidAction.EXECUTE) == 0
                && fluid.fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.SIMULATE) == 50,
                "The sprinkler's buffer, slot count, or simulation was incorrect");
        player.getInventory().setItem(9, new ItemStack(Items.DIRT));
        helper.assertTrue(menu.quickMoveStack(player, 9).isEmpty(), "The sprinkler accepted dirt through shift-click");
        player.getInventory().setItem(9, new ItemStack(Items.BONE_MEAL, 3));
        helper.assertTrue(menu.quickMoveStack(player, 9).is(Items.BONE_MEAL), "Fertilizer shift-click failed");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!menu.isActive() && sprinkler.fertilizer().getStackInSlot(0).getCount() == 3, "Dry operation consumed bone meal");
            helper.assertTrue(fluid.fill(new FluidStack(Fluids.WATER, 2), IFluidHandler.FluidAction.EXECUTE) == 2
                    && fluid.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "The sprinkler rejected water or exposed an output port");
            helper.runAfterDelay(4, () -> helper.assertTrue(menu.isActive() && menu.fluid().getAmount() == 1
                    && sprinkler.fertilizer().getStackInSlot(0).getCount() == 2, "The first pulse did not spend one water and one bone meal"));
            helper.runAfterDelay(25, () -> helper.assertTrue(menu.isActive() && menu.fluid().isEmpty(), "The final millibucket did not buy a full spray interval"));
            helper.runAfterDelay(46, () -> {
                helper.assertTrue(!menu.isActive() && sprinkler.fertilizer().getStackInSlot(0).getCount() == 2, "The sprinkler ran or spent fertilizer after drying out");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void sprinklerPullsOnlyFromBelowAndPersistsItsBufferAndCycle(final GameTestHelper helper) {
        helper.setBlock(MACHINE.below(), OBBlocks.TANK.get());
        helper.setBlock(MACHINE.east(), OBBlocks.TANK.get());
        final var below = fluid(helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE.below())), Direction.UP);
        final var beside = fluid(helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE.east())), Direction.WEST);
        below.fill(new FluidStack(Fluids.WATER, 80), IFluidHandler.FluidAction.EXECUTE);
        beside.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(MACHINE, OBBlocks.SPRINKLER.get());
        final var sprinkler = (OBSprinklerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE));
        sprinkler.fertilizer().setStackInSlot(3, new ItemStack(Items.BONE_MEAL, 7));
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(sprinkler.isActive() && sprinkler.contents().getAmount() == 49
                    && below.getFluidInTank(0).getAmount() == 30 && beside.getFluidInTank(0).getAmount() == 100,
                    "Automatic intake did not preserve water or distinguish the lower face");
            final var tag = sprinkler.saveWithFullMetadata();
            final var restored = (OBSprinklerBlockEntity) BlockEntity.loadStatic(helper.absolutePos(MACHINE), sprinkler.getBlockState(), tag);
            helper.assertTrue(restored != null && restored.contents().getAmount() == 49 && restored.fertilizer().getStackInSlot(3).getCount() == 6
                    && restored.saveWithFullMetadata().getInt("waterTicks") == tag.getInt("waterTicks")
                    && restored.saveWithFullMetadata().getInt("fertilizerTicks") == tag.getInt("fertilizerTicks") && tag.getBoolean("boosted"),
                    "Water, fertilizer, or remaining cycle time did not save and reload");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void activeSprinklerHonorsForgeVetoThenGrowsCrops(final GameTestHelper helper) {
        final var sprinkler = placeSprinkler(helper);
        final int previousRange = OBServerConfig.SPRINKLER_RANGE.get();
        final int previousChance = OBServerConfig.SPRINKLER_BONEMEAL_CHANCE.get();
        OBServerConfig.SPRINKLER_RANGE.set(1);
        OBServerConfig.SPRINKLER_BONEMEAL_CHANCE.set(1);
        final var crops = new ArrayList<BlockPos>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                final var offset = MACHINE.offset(x, 0, z);
                helper.setBlock(offset.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                helper.setBlock(offset.above(2), Blocks.GLOWSTONE);
                crops.add(helper.absolutePos(offset));
            }
        }
        final var blocked = new ArrayList<BlockPos>();
        final Consumer<BonemealEvent> protect = event -> {
            if (event.getEntity() instanceof FakePlayer && event.getLevel() == helper.getLevel() && crops.contains(event.getPos())) {
                blocked.add(event.getPos());
                event.setCanceled(true);
            }
        };
        final boolean[] untouched = {false};
        helper.runAfterDelay(7, () -> {
            for (final var crop : crops) helper.getLevel().setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
            sprinkler.fertilizer().setStackInSlot(0, new ItemStack(Items.BONE_MEAL));
            fluid(sprinkler, null).fill(new FluidStack(Fluids.WATER, 5), IFluidHandler.FluidAction.EXECUTE);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BonemealEvent.class, protect);
        });
        helper.runAfterDelay(22, () -> {
            MinecraftForge.EVENT_BUS.unregister(protect);
            untouched[0] = !blocked.isEmpty() && crops.stream().allMatch(pos -> cropAge(helper, pos) == 0);
        });
        helper.runAfterDelay(42, () -> {
            OBServerConfig.SPRINKLER_RANGE.set(previousRange);
            OBServerConfig.SPRINKLER_BONEMEAL_CHANCE.set(previousChance);
            helper.assertTrue(untouched[0] && crops.stream().anyMatch(pos -> cropAge(helper, pos) > 0),
                    "Sprinkler either ignored a Forge growth veto or could not fertilize a permitted crop");
            helper.succeed();
        });
    }

    private static int cropAge(final GameTestHelper helper, final BlockPos pos) {
        final var state = helper.getLevel().getBlockState(pos);
        return state.is(Blocks.WHEAT) ? state.getValue(CropBlock.AGE) : -1;
    }

    @GameTest(template = "empty")
    public static void donationStationIdentifiesMetadataAndValidatesAuthorLinks(final GameTestHelper helper) {
        final var sample = new ItemStack(OBItems.SPRINKLER.get());
        final var entry = OBDonationMetadata.identify(sample);
        helper.assertTrue(entry != null && entry.name().equals("Open Blocks") && entry.authors().equals(List.of("cyberpwn"))
                && entry.homepage() != null && entry.homepage().toString().equals("https://github.com/AuramMods/OpenBlocks"),
                "The station did not read the sample's owning mod metadata");
        helper.assertTrue(OBDonationMetadata.identify(new ItemStack(Items.IRON_INGOT)) == null && OBDonationMetadata.identify(ItemStack.EMPTY) == null,
                "A vanilla sample or empty slot was assigned a third-party identity");
        OBDonationMetadata.registerLink(OpenBlocks.MODID, "file:///tmp/example");
        helper.assertTrue(OBDonationMetadata.identify(sample).donation() == null, "The station accepted a non-web link");
        OBDonationMetadata.registerLink(OpenBlocks.MODID, "https://example.invalid/support");
        OBDonationMetadata.registerLink(OpenBlocks.MODID, "https://example.invalid@wrong.invalid/support");
        helper.assertTrue(OBDonationMetadata.identify(sample).donation().toString().equals("https://example.invalid/support"),
                "An author-provided web link was ignored or replaced by a credential-bearing link");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void donationStationSavesAndReturnsItsSample(final GameTestHelper helper) {
        final var pos = helper.absolutePos(MACHINE);
        helper.setBlock(MACHINE, OBBlocks.DONATION_STATION.get().defaultBlockState().setValue(OBDonationStationBlock.FACING, Direction.SOUTH));
        final var station = (OBDonationStationBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var player = playerNear(helper);
        final var menu = station.createMenu(1, player.getInventory(), player);
        final var sample = new ItemStack(OBItems.SPRINKLER.get(), 6);
        sample.setHoverName(Component.literal("Sample"));
        player.getInventory().setItem(9, sample.copy());
        helper.assertTrue(menu.slots.size() == 37 && menu.quickMoveStack(player, 1).is(OBItems.SPRINKLER.get())
                && ItemStack.matches(sample, menu.sample()), "The sample slot consumed or changed the deposited stack");
        final var restored = (OBDonationStationBlockEntity) BlockEntity.loadStatic(pos, station.getBlockState(), station.saveWithFullMetadata());
        helper.assertTrue(restored != null && ItemStack.matches(sample, restored.items().getStackInSlot(0)), "The sample did not serialize correctly");
        final var itemAccess = station.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN);
        helper.assertTrue(itemAccess.orElseThrow(IllegalStateException::new).extractItem(0, 1, false).getCount() == 1,
                "Automation could not recover the sample");
        helper.getLevel().destroyBlock(pos, true);
        helper.assertTrue(countDrops(helper, OBItems.SPRINKLER.get()) == 5 && !itemAccess.isPresent() && !menu.stillValid(player),
                "Breaking the station did not return the residual sample exactly once");
        helper.succeed();
    }

    private static OBSprinklerBlockEntity placeSprinkler(final GameTestHelper helper) {
        helper.setBlock(MACHINE.below(), Blocks.STONE);
        helper.setBlock(MACHINE, OBBlocks.SPRINKLER.get());
        return (OBSprinklerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE));
    }

    private static IFluidHandler fluid(final BlockEntity entity, final Direction direction) {
        return entity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction).orElseThrow(IllegalStateException::new);
    }

    private static Player playerNear(final GameTestHelper helper) {
        final var player = helper.makeMockSurvivalPlayer();
        final Vec3 pos = Vec3.atCenterOf(helper.absolutePos(MACHINE)).add(-2, 0, 0);
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static int countDrops(final GameTestHelper helper, final Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MACHINE)).inflate(1)).stream()
                .filter(drop -> drop.getItem().is(item)).mapToInt(drop -> drop.getItem().getCount()).sum();
    }
}
