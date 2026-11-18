package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBBearTrapBlock;
import art.arcane.openblocks.block.entity.OBBearTrapBlockEntity;
import art.arcane.openblocks.block.entity.OBFanBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class FloorDeviceGameTests {

    private static final BlockPos DEVICE = new BlockPos(3, 3, 3);

    @GameTest(template = "empty")
    public static void fanAndTrapCraftAndKeepPlacementOrientation(final GameTestHelper helper) {
        final var fanGrid = ComponentRecipeGameTests.input(helper, Items.IRON_BARS, Items.AIR, Items.AIR, Items.IRON_INGOT,
                Items.AIR, Items.AIR, Items.SMOOTH_STONE_SLAB);
        final var trapGrid = ComponentRecipeGameTests.input(helper, Items.IRON_BARS, Items.IRON_INGOT, Items.IRON_BARS,
                Items.IRON_BARS, Items.IRON_INGOT, Items.IRON_BARS, Items.IRON_BARS, Items.IRON_INGOT, Items.IRON_BARS);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, fanGrid).is(OBItems.FAN.get())
                && ComponentRecipeGameTests.craft(helper, trapGrid).is(OBItems.BEARTRAP.get()), "A legacy crafting recipe was missing");

        final Player player = helper.makeMockSurvivalPlayer();
        player.setYRot(33.0F);
        player.setYHeadRot(33.0F);
        helper.setBlock(DEVICE.below(), Blocks.STONE);
        final var support = helper.absolutePos(DEVICE.below());
        helper.assertTrue(!placeItem(player, OBItems.FAN.get(), support, Direction.EAST)
                && placeItem(player, OBItems.FAN.get(), support, Direction.UP), "The Fan must mount on the top of solid support");
        final var fan = (OBFanBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(DEVICE));
        helper.assertTrue(fan.yaw() == 33.0F, "The Fan snapped the placement angle to a cardinal direction");
        helper.useBlock(DEVICE, player);
        helper.assertTrue(fan.yaw() == 43.0F, "Use did not turn the Fan by ten degrees");
        player.setShiftKeyDown(true);
        helper.useBlock(DEVICE, player);
        final var restored = new OBFanBlockEntity(fan.getBlockPos(), fan.getBlockState());
        restored.load(fan.saveWithoutMetadata());
        helper.assertTrue(restored.yaw() == 33.0F, "Sneak-use or yaw persistence failed");
        helper.getLevel().destroyBlock(support, false);
        helper.assertBlockPresent(Blocks.AIR, DEVICE);
        helper.assertTrue(countItems(helper, OBItems.FAN.get()) == 1, "The Fan did not drop once when its support was removed");

        player.setShiftKeyDown(false);
        player.setYRot(-90.0F);
        helper.setBlock(DEVICE.below(), Blocks.STONE);
        helper.assertTrue(!placeItem(player, OBItems.BEARTRAP.get(), support, Direction.EAST)
                && placeItem(player, OBItems.BEARTRAP.get(), support, Direction.UP), "The Bear Trap must mount on solid ground");
        helper.assertTrue(helper.getBlockState(DEVICE).getValue(OBBearTrapBlock.AXIS) == Direction.Axis.X, "The Bear Trap hinge ignored placement direction");
        helper.getLevel().destroyBlock(support, false);
        helper.assertBlockPresent(Blocks.AIR, DEVICE);
        helper.assertTrue(countItems(helper, OBItems.BEARTRAP.get()) == 1, "The Bear Trap did not drop once when its support was removed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fanScalesHorizontalAirflowByRedstoneDistanceAndCone(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var fan = placeFan(helper);
        final var abs = fan.getBlockPos();
        final var center = Vec3.atCenterOf(abs);
        final var near = item(helper, center.add(0, 0, -2));
        final var far = item(helper, center.add(0, 0, -5));
        final var back = item(helper, center.add(0, 0, 3));
        final var wide = item(helper, center.add(3, 0, -1));
        final boolean originalRedstone = OBServerConfig.FAN_REDSTONE.get();
        final double originalForce = OBServerConfig.FAN_FORCE.get();
        final double originalRange = OBServerConfig.FAN_RANGE.get();
        OBServerConfig.FAN_REDSTONE.set(true);
        OBServerConfig.FAN_FORCE.set(0.05);
        OBServerConfig.FAN_RANGE.set(10.0);
        try {
            OBFanBlockEntity.tick(level, abs, fan.getBlockState(), fan);
            helper.assertTrue(fan.power() == 0 && near.getDeltaMovement().z == 0, "An unpowered Fan pushed an item");
            helper.setBlock(DEVICE.east(), Blocks.DAYLIGHT_DETECTOR.defaultBlockState().setValue(DaylightDetectorBlock.POWER, 5));
            OBFanBlockEntity.tick(level, abs, fan.getBlockState(), fan);
            final double expectedNear = -0.05 * 5 / 15 * (1 - 3.1 / 10);
            final double expectedFar = -0.05 * 5 / 15 * (1 - 6.1 / 10);
            helper.assertTrue(fan.power() == 5 && Math.abs(near.getDeltaMovement().z - expectedNear) < 1.0E-8
                    && Math.abs(far.getDeltaMovement().z - expectedFar) < 1.0E-8 && near.getDeltaMovement().y == 0.07
                    && back.getDeltaMovement().z == 0 && wide.getDeltaMovement().x == 0,
                    "Airflow did not scale by strength and distance or pushed items outside the cone");

            near.setDeltaMovement(0, 0.07, 0);
            far.setDeltaMovement(0, 0.07, 0);
            helper.setBlock(DEVICE.east(), Blocks.AIR);
            OBServerConfig.FAN_REDSTONE.set(false);
            OBServerConfig.FAN_RANGE.set(4.0);
            OBFanBlockEntity.tick(level, abs, fan.getBlockState(), fan);
            helper.assertTrue(fan.power() == 15 && Math.abs(near.getDeltaMovement().z - (-0.05 * (1 - 3.1 / 4))) < 1.0E-8
                    && far.getDeltaMovement().z == 0, "The always-on or range setting did not take effect");
        } finally {
            OBServerConfig.FAN_REDSTONE.set(originalRedstone);
            OBServerConfig.FAN_FORCE.set(originalForce);
            OBServerConfig.FAN_RANGE.set(originalRange);
            near.discard();
            far.discard();
            back.discard();
            wide.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fanMovesARealTickingItemAlongItsYaw(final GameTestHelper helper) {
        final var fan = placeFan(helper);
        fan.setYaw(90);
        helper.setBlock(DEVICE.west(), Blocks.REDSTONE_BLOCK);
        final Vec3 origin = Vec3.atCenterOf(fan.getBlockPos()).add(2, 0, 0);
        final var item = item(helper, origin);
        item.setDeltaMovement(Vec3.ZERO);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(fan.power() == 15 && item.getX() > origin.x + 0.7 && Math.abs(item.getZ() - origin.z) < 1.0E-6,
                    "An east-facing Fan failed to move a ticking item east");
            helper.setBlock(DEVICE.west(), Blocks.AIR);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(fan.power() == 0 && fan.bladeRotation(1) == fan.bladeRotation(0), "Removing power did not stop the Fan and blades");
                item.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void bearTrapWaitsForOpeningAndPinsAMobWithoutDamage(final GameTestHelper helper) {
        final var trap = placeTrap(helper);
        final var center = trapCenter(trap);
        final var cow = cow(helper, center.add(-1, 0, 0));
        helper.runAfterDelay(2, () -> {
            cow.move(MoverType.SELF, new Vec3(0.75, 0, 0));
            helper.assertTrue(!trap.isShut() && trap.comparatorOutput() == 0, "The trap caught a mob while its jaws were opening");
            cow.setPos(center.x - 1, center.y, center.z);
        });
        helper.runAfterDelay(18, () -> {
            cow.move(MoverType.SELF, new Vec3(0.75, 0, 0));
            helper.assertTrue(trap.isShut() && trap.comparatorOutput() == 1 && cow.position().distanceTo(center) < 1.0E-8,
                    "A normal mob crossing did not close the trap, center the mob, or power the comparator");
            cow.setDeltaMovement(0.2, 0, 0.1);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(cow.position().distanceTo(center) < 1.0E-8 && cow.getDeltaMovement().lengthSqr() == 0
                        && cow.getHealth() == cow.getMaxHealth(), "The trapped cow moved or took damage");
                helper.useBlock(DEVICE, helper.makeMockSurvivalPlayer());
                helper.assertTrue(!trap.isShut() && trap.comparatorOutput() == 0, "Use did not release the mob and clear the comparator");
                cow.move(MoverType.SELF, new Vec3(0.6, 0, 0));
                helper.assertTrue(cow.getX() > center.x + 0.5, "The released mob could not move freely");
                cow.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void bearTrapPersistsTheCaptureAndRedstoneReleasesAndLocksIt(final GameTestHelper helper) {
        final var trap = placeTrap(helper);
        final var center = trapCenter(trap);
        final var cow = cow(helper, center.add(-1, 0, 0));
        helper.runAfterDelay(18, () -> {
            cow.move(MoverType.SELF, new Vec3(0.75, 0, 0));
            final var saved = trap.saveWithoutMetadata();
            helper.assertTrue(saved.hasUUID("captive") && saved.getUUID("captive").equals(cow.getUUID()), "The trap did not store the captured entity ID");
            trap.load(saved);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(trap.isShut() && trap.comparatorOutput() == 1 && cow.position().distanceTo(center) < 1.0E-8,
                        "A reloaded trap lost its capturable entity or comparator output");
                helper.setBlock(DEVICE.below(), Blocks.REDSTONE_BLOCK);
                helper.assertTrue(!trap.isShut() && trap.comparatorOutput() == 0, "Power did not immediately release the trapped mob");
                helper.runAfterDelay(18, () -> {
                    cow.move(MoverType.SELF, new Vec3(0.1, 0, 0));
                    helper.assertTrue(!trap.isShut(), "The powered trap caught a mob while locked open");
                    helper.setBlock(DEVICE.below(), Blocks.STONE);
                    cow.move(MoverType.SELF, new Vec3(0.1, 0, 0));
                    helper.assertTrue(trap.isShut(), "Removing redstone did not rearm the trap");
                    cow.discard();
                    helper.runAfterDelay(2, () -> {
                        helper.assertTrue(!trap.isShut() && trap.comparatorOutput() == 0, "A removed mob left the trap shut");
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = "empty")
    public static void bearTrapIgnoresPlayersItemsAndMobsAboveItsJaws(final GameTestHelper helper) {
        final var trap = placeTrap(helper);
        final var center = trapCenter(trap);
        final var cow = cow(helper, center.add(-1, 0.6, 0));
        final var player = helper.makeMockSurvivalPlayer();
        player.setPos(center.x - 1, center.y, center.z);
        final var dropped = item(helper, center.add(-1, 0, 0));
        dropped.setDeltaMovement(Vec3.ZERO);
        helper.runAfterDelay(18, () -> {
            cow.move(MoverType.SELF, new Vec3(0.75, 0, 0));
            player.move(MoverType.SELF, new Vec3(0.75, 0, 0));
            dropped.move(MoverType.SELF, new Vec3(0.75, 0, 0));
            helper.assertTrue(!trap.isShut() && trap.comparatorOutput() == 0, "A player, dropped item, or mob above the jaws triggered the trap");
            cow.discard();
            dropped.discard();
            helper.succeed();
        });
    }

    private static OBFanBlockEntity placeFan(final GameTestHelper helper) {
        placeDevice(helper, OBBlocks.FAN.get());
        return (OBFanBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(DEVICE));
    }

    private static OBBearTrapBlockEntity placeTrap(final GameTestHelper helper) {
        placeDevice(helper, OBBlocks.BEARTRAP.get());
        return (OBBearTrapBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(DEVICE));
    }

    private static void placeDevice(final GameTestHelper helper, final Block block) {
        helper.setBlock(DEVICE.below(), Blocks.STONE);
        helper.setBlock(DEVICE, block);
    }

    private static Vec3 trapCenter(final OBBearTrapBlockEntity trap) {
        return Vec3.atBottomCenterOf(trap.getBlockPos()).add(0, OBBearTrapBlock.BASE_HEIGHT, 0);
    }

    private static Cow cow(final GameTestHelper helper, final Vec3 pos) {
        final var entity = new Cow(EntityType.COW, helper.getLevel());
        entity.setPos(pos.x, pos.y, pos.z);
        entity.setNoAi(true);
        entity.setNoGravity(true);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static ItemEntity item(final GameTestHelper helper, final Vec3 pos) {
        final var entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(Items.IRON_INGOT), 0, 0.07, 0);
        entity.setNoGravity(true);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static boolean placeItem(final Player player, final Item item, final BlockPos support, final Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        final var hit = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, support, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }

    private static int countItems(final GameTestHelper helper, final Item item) {
        final var bounds = new AABB(helper.absolutePos(DEVICE)).inflate(1);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds, entity -> entity.getItem().is(item)).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }
}
