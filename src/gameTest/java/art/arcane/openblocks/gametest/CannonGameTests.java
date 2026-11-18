package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBVacuumHopperBlock;
import art.arcane.openblocks.block.entity.OBCannonBlockEntity;
import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity;
import art.arcane.openblocks.entity.OBItemProjectile;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class CannonGameTests {

    private static final BlockPos CANNON = new BlockPos(3, 3, 3);
    private static final BlockPos POWER = CANNON.north();

    @GameTest(template = "empty")
    public static void cannonAndPointerCraftAndCannonMountsWithoutCollision(final GameTestHelper helper) {
        final var cannonRecipe = ComponentRecipeGameTests.input(helper, Items.AIR, Items.DISPENSER, Items.AIR,
                Items.AIR, Items.IRON_BARS, Items.AIR, Items.IRON_INGOT, Items.REDSTONE_BLOCK, Items.IRON_INGOT);
        final var pointerRecipe = ComponentRecipeGameTests.input(helper, Items.WHITE_WOOL, Items.AIR, Items.AIR,
                Items.WHITE_WOOL, Items.WHITE_WOOL, Items.AIR, Items.WHITE_WOOL, Items.AIR, Items.AIR);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, cannonRecipe).is(OBItems.CANNON.get())
                && ComponentRecipeGameTests.craft(helper, pointerRecipe).is(OBItems.POINTER.get()), "The legacy recipes did not craft");

        final var player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.CANNON.get()));
        helper.setBlock(CANNON.below(), Blocks.STONE);
        final BlockPos surface = helper.absolutePos(CANNON.below());
        helper.assertTrue(!player.getMainHandItem().useOn(useOn(player, surface, Direction.EAST)).consumesAction(),
                "The cannon mounted on the side of a support block");
        helper.assertTrue(player.getMainHandItem().useOn(useOn(player, surface, Direction.UP)).consumesAction(),
                "Normal cannon placement on a support failed");
        final var state = helper.getBlockState(CANNON);
        helper.assertTrue(state.getCollisionShape(helper.getLevel(), helper.absolutePos(CANNON)).isEmpty()
                && cannon(helper).getBlockPos().equals(helper.absolutePos(CANNON)), "The cannon was solid or lacked its block entity");
        helper.getLevel().destroyBlock(surface, false);
        helper.assertBlockPresent(Blocks.AIR, CANNON);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pointerSelectsAndAimsWithTheRealProjectilePhysics(final GameTestHelper helper) {
        placeCannon(helper);
        final var cannon = cannon(helper);
        final BlockPos target = helper.absolutePos(CANNON.south(3));
        helper.setBlock(CANNON.south(3), Blocks.STONE);
        final var player = helper.makeMockSurvivalPlayer();
        final var pointer = new ItemStack(OBItems.POINTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pointer);
        final var cannonHit = useOn(player, cannon.getBlockPos(), Direction.NORTH);
        helper.assertTrue(!pointer.useOn(cannonHit).consumesAction() && !pointer.hasTag(), "A regular click selected the cannon");
        player.setShiftKeyDown(true);
        helper.assertTrue(pointer.useOn(cannonHit).consumesAction() && pointer.hasTag(), "Sneak-use failed to select the cannon");
        helper.assertTrue(pointer.useOn(useOn(player, target, Direction.UP)).consumesAction(), "Sneak-use failed to aim at the target");
        final Vec3 shot = cannon.motion();
        helper.assertTrue(Math.abs(shot.z - 0.15) < 1.0E-8 && shot.y > 0.4, "The pointer set an incorrect heading or launch velocity: " + shot);
        final var projectile = new OBItemProjectile(helper.getLevel(), cannon.origin(), shot, new ItemStack(Items.EMERALD, 7));
        for (int tick = 0; tick < 20; tick++) projectile.tick();
        final Vec3 landing = Vec3.atBottomCenterOf(target.above());
        helper.assertTrue(projectile.position().distanceTo(landing) < 1.0E-6, "The actual item drop missed the targeted block: " + projectile.position());
        final var loaded = new OBItemProjectile(OBEntities.ITEM_PROJECTILE.get(), helper.getLevel());
        loaded.load(projectile.saveWithoutId(new CompoundTag()));
        helper.assertTrue(loaded.getItem().is(Items.EMERALD) && loaded.getItem().getCount() == 7
                && loaded.position().distanceTo(landing) < 1.0E-6, "An in-flight projectile lost its stack or position during reload");
        final var saved = cannon.saveWithoutMetadata();
        cannon.setPitch(Double.NaN);
        cannon.setYaw(Double.POSITIVE_INFINITY);
        helper.assertTrue(saved.equals(cannon.saveWithoutMetadata()), "Non-finite peripheral aim poisoned cannon state");
        final var restored = new OBCannonBlockEntity(cannon.getBlockPos(), cannon.getBlockState());
        restored.load(saved);
        helper.assertTrue(restored.motion().distanceTo(shot) < 1.0E-8, "A saved cannon lost its aim");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cannonPullsOneStackEachSecondWhilePowered(final GameTestHelper helper) {
        placeCannon(helper);
        final var cannon = cannon(helper);
        helper.setBlock(CANNON.west(), Blocks.CHEST);
        final var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CANNON.west()));
        chest.setItem(0, new ItemStack(Items.EMERALD, 64));
        chest.setItem(1, new ItemStack(Items.DIAMOND, 13));
        chest.setItem(2, new ItemStack(Items.IRON_INGOT, 5));
        final List<OBItemProjectile> shots = new ArrayList<>();
        final List<Long> shotTicks = new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> fire = event -> {
            if (event.getLevel() == helper.getLevel() && event.getEntity() instanceof OBItemProjectile shot
                    && shot.position().equals(cannon.origin())) {
                shots.add(shot);
                shotTicks.add(helper.getLevel().getGameTime());
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, fire);
        helper.startSequence().thenIdle(22).thenExecute(() -> {
            helper.assertTrue(shots.isEmpty(), "The unpowered cannon extracted items");
            helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        }).thenWaitUntil(() -> helper.assertTrue(shots.size() >= 2, "Waiting for two redstone-powered shots"))
                .thenExecute(() -> helper.setBlock(POWER, Blocks.AIR)).thenIdle(22).thenExecute(() -> {
                    MinecraftForge.EVENT_BUS.unregister(fire);
                    helper.assertTrue(shots.size() == 2 && shotTicks.get(1) - shotTicks.get(0) == 20,
                            "Cannon firing did not follow its one-second cycle");
                    helper.assertTrue(shots.get(0).getItem().is(Items.EMERALD) && shots.get(0).getItem().getCount() == 64
                            && shots.get(1).getItem().is(Items.DIAMOND) && shots.get(1).getItem().getCount() == 13
                            && chest.getItem(2).getCount() == 5, "The cannon lost, split, or duplicated a stack");
                    shots.forEach(OBItemProjectile::discard);
                }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void cannonRetriesCanceledSpawnFromExtractOnlySource(final GameTestHelper helper) {
        placeCannon(helper);
        final var cannon = cannon(helper);
        helper.setBlock(CANNON.west(), OBBlocks.VACUUM_HOPPER.get().defaultBlockState().setValue(OBVacuumHopperBlock.ENABLED, false));
        final var source = (OBVacuumHopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CANNON.west()));
        source.toggleOutput(OBVacuumHopperBlockEntity.Output.ITEMS, Direction.EAST);
        source.items().setStackInSlot(0, new ItemStack(Items.QUARTZ, 31));
        final List<OBItemProjectile> attempts = new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> watch = event -> {
            if (event.getLevel() == helper.getLevel() && event.getEntity() instanceof OBItemProjectile shot
                    && shot.position().equals(cannon.origin())) {
                attempts.add(shot);
                if (attempts.size() == 1) event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, watch);
        helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(!attempts.isEmpty(), "Waiting for a protected spawn"))
                .thenExecute(() -> {
                    final var data = cannon.saveWithoutMetadata();
                    final var held = ItemStack.of(data.getCompound("pending"));
                    helper.assertTrue(held.is(Items.QUARTZ) && held.getCount() == 31 && source.items().getStackInSlot(0).isEmpty(),
                            "A canceled launch lost a stack pulled through an extract-only face");
                    cannon.load(data);
                }).thenWaitUntil(() -> helper.assertTrue(attempts.size() >= 2, "Waiting for the reserved stack to fire"))
                .thenExecute(() -> {
                    MinecraftForge.EVENT_BUS.unregister(watch);
                    helper.setBlock(POWER, Blocks.AIR);
                    helper.assertTrue(attempts.get(1).getItem().is(Items.QUARTZ) && attempts.get(1).getItem().getCount() == 31
                            && !cannon.saveWithoutMetadata().contains("pending"), "Retry did not release the original stack exactly once");
                    attempts.get(1).discard();
                }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void vacuumHopperIgnoresRisingProjectilesButCatchesReturningStack(final GameTestHelper helper) {
        helper.setBlock(CANNON, OBBlocks.VACUUM_HOPPER.get());
        final BlockPos pos = helper.absolutePos(CANNON);
        final var hopper = (OBVacuumHopperBlockEntity) helper.getLevel().getBlockEntity(pos);
        final Vec3 origin = Vec3.atCenterOf(pos).add(0.0, 1.02, 0.0);
        final var projectile = new OBItemProjectile(helper.getLevel(), origin, new Vec3(0, 0.22, 0), new ItemStack(Items.COPPER_INGOT, 17));
        helper.getLevel().addFreshEntity(projectile);
        helper.runAfterDelay(2, () -> helper.assertTrue(projectile.isAlive() && projectile.getDeltaMovement().y > 0
                && hopper.items().getStackInSlot(0).isEmpty(), "The hopper caught or slowed a rising item"));
        helper.succeedWhen(() -> helper.assertTrue(projectile.isRemoved() && hopper.items().getStackInSlot(0).is(Items.COPPER_INGOT)
                && hopper.items().getStackInSlot(0).getCount() == 17, "Waiting for the descending stack to reach the hopper"));
    }

    private static UseOnContext useOn(final Player player, final BlockPos target, final Direction face) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(target).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, target, false));
    }

    private static void placeCannon(final GameTestHelper helper) {
        helper.setBlock(CANNON.below(), Blocks.STONE);
        helper.setBlock(CANNON, OBBlocks.CANNON.get());
    }

    private static OBCannonBlockEntity cannon(final GameTestHelper helper) {
        return (OBCannonBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CANNON));
    }
}
