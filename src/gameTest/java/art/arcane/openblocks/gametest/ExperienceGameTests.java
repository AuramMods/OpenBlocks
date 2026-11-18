package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBXPShowerBlock;
import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.entity.OBFallingExperienceOrb;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBFluids;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBLiquidExperience;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class ExperienceGameTests {

    @GameTest(template = "empty")
    public static void drainSpendsOnlyWholePointsAcrossLevelBoundary(final GameTestHelper helper) {
        final BlockPos drain = new BlockPos(3, 2, 3);
        final var tank = placeTank(helper, drain.below(), OBTankBlockEntity.capacity() - 50);
        helper.setBlock(drain, OBBlocks.XP_DRAIN.get());
        final Player player = putPlayerOnDrain(helper, drain, 8);
        helper.runAfterDelay(8, () -> {
            player.discard();
            helper.assertTrue(OBLiquidExperience.playerPoints(player) == 6 && player.experienceLevel == 0,
                    "The drain did not remove two whole points across the level boundary");
            helper.assertTrue(tank.contents().getAmount() == OBTankBlockEntity.capacity() - 10,
                    "The drain filled the tank with fractional XP");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void drainKeepsTheRemainderOfAMergedOrb(final GameTestHelper helper) {
        final BlockPos drain = new BlockPos(3, 2, 3);
        final var tank = placeTank(helper, drain.below(), OBTankBlockEntity.capacity() - 140);
        helper.setBlock(drain, OBBlocks.XP_DRAIN.get());
        final Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(drain)).add(0.0, 0.15, 0.0);
        final var orb = new ExperienceOrb(helper.getLevel(), origin.x, origin.y, origin.z, 3);
        orb.count = 3;
        orb.setDeltaMovement(Vec3.ZERO);
        orb.setNoGravity(true);
        helper.getLevel().addFreshEntity(orb);

        helper.runAfterDelay(8, () -> {
            helper.assertTrue(!orb.isRemoved() && orb.count == 1, "Two of the three constituent orbs should fit");
            helper.assertTrue(tank.contents().getAmount() == OBTankBlockEntity.capacity() - 20, "Merged orb transfer lost or duplicated XP");
            handler(tank).drain(40, FluidAction.EXECUTE);
            helper.succeedWhen(() -> {
                helper.assertTrue(orb.isRemoved(), "The final constituent did not transfer after space was freed");
                helper.assertTrue(tank.contents().getAmount() == OBTankBlockEntity.capacity(), "Final transfer did not fill the tank exactly");
            });
        });
    }

    @GameTest(template = "empty")
    public static void drainHonorsCanceledExperienceLoss(final GameTestHelper helper) {
        final BlockPos drain = new BlockPos(3, 2, 3);
        final var tank = placeTank(helper, drain.below(), 0);
        helper.setBlock(drain, OBBlocks.XP_DRAIN.get());
        final Player player = putPlayerOnDrain(helper, drain, 7);
        final var fired = new AtomicBoolean();
        final Consumer<PlayerXpEvent.XpChange> listener = event -> {
            if (event.getEntity() != player) return;
            fired.set(true);
            event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerXpEvent.XpChange.class, listener);
        helper.runAfterDelay(8, () -> {
            MinecraftForge.EVENT_BUS.unregister(listener);
            player.discard();
            helper.assertTrue(fired.get(), "The drain did not call the Forge XP event");
            helper.assertTrue(OBLiquidExperience.playerPoints(player) == 7 && tank.contents().isEmpty(),
                    "Canceling XP loss still changed the player or the receiver");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void poweredShowerFeedsAnXpDrainWithNoFluidLoss(final GameTestHelper helper) {
        final BlockPos shower = new BlockPos(3, 4, 3);
        final BlockPos drain = shower.below(2);
        final var source = placeTank(helper, shower.north(), 160);
        final var sink = placeTank(helper, drain.below(), 0);
        helper.setBlock(drain, OBBlocks.XP_DRAIN.get());
        placeShowerFromTankSide(helper, shower.north(), Direction.SOUTH);

        helper.runAfterDelay(12, () -> {
            helper.assertTrue(source.contents().getAmount() == 160 && sink.contents().isEmpty(), "Unpowered shower moved fluid");
            helper.setBlock(shower.east(), Blocks.REDSTONE_BLOCK);
            helper.assertTrue(helper.getBlockState(shower).getValue(OBXPShowerBlock.POWERED), "Shower did not detect redstone power");
            helper.succeedWhen(() -> {
                helper.assertTrue(sink.contents().getAmount() == 160, "Falling XP orbs have yet to reach the drain");
                helper.assertTrue(source.contents().isEmpty(), "The shower created XP without consuming liquid");
                helper.setBlock(shower.east(), Blocks.AIR);
                helper.assertTrue(!helper.getBlockState(shower).getValue(OBXPShowerBlock.POWERED), "Shower stayed powered after redstone was removed");
            });
        });
    }

    @GameTest(template = "empty")
    public static void showerKeepsItsBufferAcrossLoadAndCanceledSpawns(final GameTestHelper helper) {
        final BlockPos shower = new BlockPos(3, 4, 3);
        final var source = placeTank(helper, shower.west(), 40);
        placeShowerFromTankSide(helper, shower.west(), Direction.EAST);
        final var absolute = helper.absolutePos(shower);
        final var area = new AABB(absolute).expandTowards(0.0, -3.0, 0.0);
        helper.setBlock(shower.north(), Blocks.REDSTONE_BLOCK);

        final var blocked = new AtomicBoolean();
        final Consumer<EntityJoinLevelEvent> listener = event -> {
            if (event.getLevel() != helper.getLevel() || !(event.getEntity() instanceof OBFallingExperienceOrb orb)
                    || !area.contains(orb.position())) return;
            blocked.set(true);
            event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, listener);
        helper.runAfterDelay(12, () -> {
            MinecraftForge.EVENT_BUS.unregister(listener);
            final var entity = helper.getLevel().getBlockEntity(absolute);
            final var saved = entity.saveWithFullMetadata();
            final var restored = BlockEntity.loadStatic(absolute, helper.getBlockState(shower), saved);
            helper.assertTrue(blocked.get() && source.contents().isEmpty(), "The powered shower did not fill and try to spawn");
            helper.assertTrue(restored != null && restored.saveWithoutMetadata().getCompound("buffer").getInt("Amount") == 40,
                    "The shower lost buffered fluid during a canceled spawn or serialization");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(OBFallingExperienceOrb.class, area).isEmpty(),
                    "A canceled orb remained in the world");
            helper.succeedWhen(() -> {
                final var orbs = helper.getLevel().getEntitiesOfClass(OBFallingExperienceOrb.class, area);
                helper.assertTrue(!orbs.isEmpty(), "The saved buffer did not resume releasing XP");
                helper.assertTrue(orbs.get(0).getValue() == 1 && orbs.get(0).getX() == absolute.getX() + 0.5,
                        "The shower emitted the wrong orb value or position");
            });
        });
    }

    private static OBTankBlockEntity placeTank(final GameTestHelper helper, final BlockPos pos, final int amount) {
        helper.setBlock(pos, OBBlocks.TANK.get());
        final var tank = (OBTankBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        handler(tank).fill(new FluidStack(OBFluids.XP_JUICE.get(), amount), FluidAction.EXECUTE);
        return tank;
    }

    private static IFluidHandler handler(final OBTankBlockEntity tank) {
        return tank.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
    }

    private static Player putPlayerOnDrain(final GameTestHelper helper, final BlockPos pos, final int points) {
        final Player player = helper.makeMockSurvivalPlayer();
        final Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(pos)).add(0.0, 1.0 / 16.0, 0.0);
        player.moveTo(origin.x, origin.y, origin.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        player.giveExperiencePoints(points);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static void placeShowerFromTankSide(final GameTestHelper helper, final BlockPos tank, final Direction side) {
        final Player player = helper.makeMockSurvivalPlayer();
        final var stack = new ItemStack(OBItems.XP_SHOWER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos clicked = helper.absolutePos(tank);
        final var hit = new BlockHitResult(Vec3.atCenterOf(clicked).relative(side, 0.5), side, clicked, false);
        helper.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Shower failed to place on a tank side");
        final var state = helper.getBlockState(tank.relative(side));
        helper.assertTrue(state.is(OBBlocks.XP_SHOWER.get()) && state.getValue(OBXPShowerBlock.FACING) == side,
                "Shower turned away from the clicked face");
    }
}
