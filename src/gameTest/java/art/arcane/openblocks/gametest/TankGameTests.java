package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBTankBlock;
import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.item.OBTankItem;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBFluids;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBLiquidExperience;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class TankGameTests {

    @GameTest(template = "empty")
    public static void tankColumnFillsFromBottomAndDrainsFromTop(final GameTestHelper helper) {
        final BlockPos lowerPos = new BlockPos(3, 2, 3);
        final var lower = placeTank(helper, lowerPos);
        final var upper = placeTank(helper, lowerPos.above());
        final IFluidHandler handler = handler(lower);
        final int capacity = OBTankBlockEntity.capacity();
        final var fill = new FluidStack(Fluids.WATER, capacity + 1700);
        helper.assertTrue(handler.fill(fill, FluidAction.SIMULATE) == fill.getAmount(), "Simulation did not include the upper tank");
        helper.assertTrue(lower.contents().isEmpty() && upper.contents().isEmpty(), "Fill simulation changed storage");
        helper.assertTrue(handler.fill(fill, FluidAction.EXECUTE) == fill.getAmount(), "Column could not receive the simulated volume");
        helper.assertTrue(lower.contents().getAmount() == capacity && upper.contents().getAmount() == 1700, "Column did not fill bottom first");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE) == 0, "Column mixed fluids");
        helper.assertTrue(handler.drain(2200, FluidAction.SIMULATE).getAmount() == 2200 && upper.contents().getAmount() == 1700,
                "Drain simulation changed storage or omitted connected fluid");
        helper.assertTrue(handler.drain(2200, FluidAction.EXECUTE).getAmount() == 2200, "Column could not dispense the simulated volume");
        helper.assertTrue(lower.contents().getAmount() == capacity - 500 && upper.contents().isEmpty(), "Column did not drain top first");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tankGravityAndBalancingConserveFluid(final GameTestHelper helper) {
        final BlockPos bottom = new BlockPos(3, 2, 3);
        final var lower = placeTank(helper, bottom);
        final var upper = placeTank(helper, bottom.above());
        final var side = placeTank(helper, bottom.east());
        final var incompatible = placeTank(helper, bottom.west());
        handler(incompatible).fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
        handler(upper).fill(new FluidStack(Fluids.WATER, 3000), FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            helper.assertTrue(upper.contents().isEmpty() && lower.contents().getAmount() == 1500
                    && side.contents().getAmount() == 1500, "Water has yet to fall and balance between the lower tanks");
            helper.assertTrue(incompatible.contents().getFluid() == Fluids.LAVA && incompatible.contents().getAmount() == 1000,
                    "An adjacent tank of a different fluid changed");
        });
    }

    @GameTest(template = "empty")
    public static void tankSaveAndTankItemRoundTripPreserveFluid(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(3, 2, 3);
        final var tank = placeTank(helper, pos);
        final var stored = new FluidStack(OBFluids.XP_JUICE.get(), 3333);
        stored.getOrCreateTag().putString("fixture", "preserved");
        handler(tank).fill(stored, FluidAction.EXECUTE);
        final var saved = tank.saveWithFullMetadata();
        final var restored = OBTankBlockEntity.loadStatic(helper.absolutePos(pos), helper.getBlockState(pos), saved);
        helper.assertTrue(restored instanceof OBTankBlockEntity loaded && stored.isFluidStackIdentical(loaded.contents()),
                "Tank save/load lost contents or fluid data");

        final var fluidCapability = tank.getCapability(ForgeCapabilities.FLUID_HANDLER);
        final var level = helper.getLevel();
        final var absolute = helper.absolutePos(pos);
        level.destroyBlock(absolute, true);
        helper.assertTrue(!fluidCapability.isPresent(), "Destroyed tank left a live fluid capability");
        final var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1.0),
                drop -> drop.getItem().is(OBItems.TANK.get()));
        helper.assertTrue(drops.size() == 1, "Tank did not drop exactly one portable tank");
        final ItemStack drop = drops.get(0).getItem().copy();
        helper.assertTrue(stored.isFluidStackIdentical(OBTankItem.contents(drop)), "Tank drop changed the stored fluid");
        drops.get(0).discard();

        final var player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, drop);
        helper.setBlock(pos.below(), Blocks.STONE);
        final var hit = hit(absolute.below());
        helper.assertTrue(drop.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Tank item would not place");
        final var replaced = (OBTankBlockEntity) level.getBlockEntity(absolute);
        helper.assertTrue(replaced != null && stored.isFluidStackIdentical(replaced.contents()), "Tank item placed without its contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void portableTanksTransferFluidInWholeStackShares(final GameTestHelper helper) {
        final var stack = new ItemStack(OBItems.TANK.get(), 3);
        final var handler = FluidUtil.getFluidHandler(stack).orElseThrow(IllegalStateException::new);
        final var fill = new FluidStack(Fluids.WATER, 1000);
        helper.assertTrue(handler.fill(fill, FluidAction.SIMULATE) == 999 && OBTankItem.contents(stack).isEmpty(),
                "Portable tank simulation did not distribute fluid or touched the item");
        helper.assertTrue(handler.fill(fill, FluidAction.EXECUTE) == 999 && OBTankItem.contents(stack).getAmount() == 333,
                "Portable tank did not split fluid equally between stack members");
        helper.assertTrue(handler.drain(200, FluidAction.EXECUTE).getAmount() == 198 && handler.getFluidInTank(0).getAmount() == 801,
                "Portable tanks lost or duplicated fluid when drained");
        helper.assertTrue(handler.drain(1000, FluidAction.EXECUTE).getAmount() == 801 && OBTankItem.contents(stack).isEmpty(),
                "Portable tanks retained fluid on emptying");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tankExchangesBucketsAndDispensesExperience(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(3, 2, 3);
        final var tank = placeTank(helper, pos);
        final var player = helper.makeMockSurvivalPlayer();
        final var level = helper.getLevel();
        final var abs = helper.absolutePos(pos);
        final var state = helper.getBlockState(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.XP_BUCKET.get()));
        helper.assertTrue(state.use(level, player, InteractionHand.MAIN_HAND, hit(abs)).consumesAction(), "XP bucket did not interact with tank");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && tank.contents().getAmount() == 1000, "Emptying the XP bucket failed");
        helper.assertTrue(state.use(level, player, InteractionHand.MAIN_HAND, hit(abs)).consumesAction(), "Empty bucket did not interact with tank");
        helper.assertTrue(player.getMainHandItem().is(OBItems.XP_BUCKET.get()) && tank.contents().isEmpty(), "Filling the XP bucket failed");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        handler(tank).fill(new FluidStack(OBFluids.XP_JUICE.get(), 1010), FluidAction.EXECUTE);
        helper.assertTrue(state.use(level, player, InteractionHand.MAIN_HAND, hit(abs)).consumesAction(), "Empty hand did not recover experience");
        helper.assertTrue(player.experienceLevel == 1 && OBLiquidExperience.playerPoints(player) == 7
                && tank.contents().getAmount() == 870, "Tank did not use exactly enough fluid to reach the next level");
        helper.assertTrue(state.getAnalogOutputSignal(level, abs) == 1, "A partially filled tank had the wrong comparator output");
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(pos).getValue(OBTankBlock.LIGHT) == 10, "Liquid XP did not light the tank"));
    }

    @GameTest(template = "empty")
    public static void tankRefundsFluidWhenModCancelsXpGain(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(3, 2, 3);
        final var tank = placeTank(helper, pos);
        final var player = helper.makeMockSurvivalPlayer();
        final FluidStack xp = OBLiquidExperience.juice(50);
        handler(tank).fill(xp, FluidAction.EXECUTE);
        final Consumer<PlayerXpEvent.XpChange> cancel = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerXpEvent.XpChange.class, cancel);
        try {
            helper.getBlockState(pos).use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper.absolutePos(pos)));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
        }
        helper.assertTrue(OBLiquidExperience.playerPoints(player) == 0 && tank.contents().isFluidStackIdentical(xp),
                "A canceled experience withdrawal consumed fluid or awarded points");
        helper.succeed();
    }

    private static OBTankBlockEntity placeTank(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, OBBlocks.TANK.get());
        return (OBTankBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static IFluidHandler handler(final OBTankBlockEntity entity) {
        return entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElseThrow(IllegalStateException::new);
    }

    private static BlockHitResult hit(final BlockPos pos) {
        return new BlockHitResult(Vec3.atBottomCenterOf(pos.above()), Direction.UP, pos, false);
    }
}
