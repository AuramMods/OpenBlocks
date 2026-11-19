package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.player.OBBrickActions;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBSounds;
import art.arcane.openblocks.registry.OBStats;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class BrickActionGameTests {

    @GameTest(template = "empty")
    public static void tastyClayCraftsAndDigestsAtFullHungerWhenEnabled(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.CLAY_BALL, Items.MILK_BUCKET, Items.COCOA_BEANS);
        final var level = helper.getLevel();
        final var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
        final var clay = recipe.assemble(grid, level.registryAccess());
        helper.assertTrue(clay.is(OBItems.TASTY_CLAY.get()) && clay.getCount() == 2 && recipe.getRemainingItems(grid).get(1).is(Items.BUCKET),
                "The legacy recipe did not yield two Tasty Clay and return its milk bucket");
        grid.setItem(2, new ItemStack(Items.BROWN_DYE));
        helper.assertTrue(!recipe.matches(grid, level), "Brown dye substituted for cocoa beans");

        final var player = new FoodTestPlayer(level);
        position(helper, player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "The player must have full hunger for this test");
        inSeriousMode(false, () -> player.eatOne(clay));
        helper.assertTrue(storedBricks(player) == 1 && clay.getCount() == 1, "Eating at full hunger did not store one brick");

        inSeriousMode(true, () -> player.eatOne(clay));
        helper.assertTrue(storedBricks(player) == 1 && clay.isEmpty(), "Serious mode should allow eating without storing a brick");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void booThrowsBehindAndAwardsOnlyTheFirstSurvivalDrop(final GameTestHelper helper) {
        final var player = serverPlayer(helper, false);
        final var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(OpenBlocks.id("oops"));
        final var progress = player.getAdvancements().getOrStartProgress(advancement);
        player.setYRot(90);
        OBCapabilities.setBowelBrickCount(player, 1);
        final var noises = new AtomicInteger();
        final Consumer<PlayLevelSoundEvent.AtPosition> sound = event -> {
            if (event.getLevel() == player.level() && event.getSound().value() == OBSounds.FEATURE_FART.get()
                    && event.getSource() == SoundSource.PLAYERS) noises.incrementAndGet();
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayLevelSoundEvent.AtPosition.class, sound);
        try {
            inSeriousMode(false, () -> {
                MinecraftForge.EVENT_BUS.post(new ItemTossEvent(
                        new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.BRICK)), player));
                helper.assertTrue(storedBricks(player) == 1 && !progress.isDone() && brickStat(player) == 0,
                        "A normal inventory toss affected Boo progression");
                OBBrickActions.boo(player);
                final var dropped = bricksNear(player);
                helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1,
                        "One Boo action must spawn exactly one brick");
                final var brick = dropped.get(0);
                helper.assertTrue(brick.position().distanceTo(player.position().add(0.75, 0.5, 0)) < 1.0E-6
                        && brick.getDeltaMovement().distanceTo(new Vec3(0.5, 0.2, 0)) < 1.0E-6 && brick.hasPickUpDelay(),
                        "The brick lost its backward offset, launch velocity or pickup delay");
                helper.assertTrue(storedBricks(player) == 0 && progress.isDone() && brickStat(player) == 1,
                        "A released brick did not spend one charge and award the stat and advancement");
                OBBrickActions.boo(player);
                helper.assertTrue(bricksNear(player).size() == 1 && noises.get() == 2 && brickStat(player) == 1,
                        "An empty Boo action should play the sound without spawning or counting another brick");
                brick.discard();
            });
        } finally {
            MinecraftForge.EVENT_BUS.unregister(sound);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void booRequiresServerOptInAndCreativeDoesNotSpendCharges(final GameTestHelper helper) {
        final var player = serverPlayer(helper, true);
        OBCapabilities.setBowelBrickCount(player, 2);
        inSeriousMode(true, () -> OBBrickActions.boo(player));
        helper.assertTrue(bricksNear(player).isEmpty() && brickStat(player) == 0 && storedBricks(player) == 2,
                "The server allowed Boo while Serious Mode was enabled");

        inSeriousMode(false, () -> OBBrickActions.boo(player));
        final var drops = bricksNear(player);
        helper.assertTrue(drops.size() == 1 && brickStat(player) == 1 && storedBricks(player) == 2,
                "Creative Boo should release a brick without using a stored charge");
        drops.forEach(ItemEntity::discard);
        OBCapabilities.setBowelBrickCount(player, 0);
        inSeriousMode(false, () -> OBBrickActions.boo(player));
        final var moreDrops = bricksNear(player);
        helper.assertTrue(moreDrops.size() == 1 && brickStat(player) == 2, "Creative Boo required a stored charge");
        moreDrops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cancelledBrickSpawnPreservesChargeAndProgress(final GameTestHelper helper) {
        final var player = serverPlayer(helper, false);
        final var advancement = player.serverLevel().getServer().getAdvancements().getAdvancement(OpenBlocks.id("oops"));
        final var progress = player.getAdvancements().getOrStartProgress(advancement);
        OBCapabilities.setBowelBrickCount(player, 1);

        final Consumer<EntityJoinLevelEvent> cancel = event -> {
            if (event.getLevel() == player.level() && event.getEntity() instanceof ItemEntity item
                    && item.getItem().is(Items.BRICK) && item.distanceToSqr(player) < 4) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, cancel);
        try {
            inSeriousMode(false, () -> OBBrickActions.boo(player));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
        }
        helper.assertTrue(bricksNear(player).isEmpty() && storedBricks(player) == 1 && !progress.isDone() && brickStat(player) == 0,
                "A cancelled drop consumed a charge or awarded progression");

        inSeriousMode(false, () -> OBBrickActions.boo(player));
        final var dropped = bricksNear(player);
        helper.assertTrue(dropped.size() == 1 && storedBricks(player) == 0 && progress.isDone() && brickStat(player) == 1,
                "The player could not use the restored charge");
        dropped.forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deathDropsAtMostSixteenBricksAndClearsTheStoredCount(final GameTestHelper helper) {
        final var player = new FoodTestPlayer(helper.getLevel());
        position(helper, player);
        player.setYRot(180);
        OBCapabilities.setBowelBrickCount(player, 21);
        final var drops = new ArrayList<ItemEntity>();
        inSeriousMode(false, () -> MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(player,
                player.damageSources().generic(), drops, 0, false)));
        helper.assertTrue(drops.size() == 16 && drops.stream().allMatch(item -> item.getItem().is(Items.BRICK)
                && item.getItem().getCount() == 1 && item.getDeltaMovement().distanceTo(new Vec3(0, 0.2, 0.5)) < 1.0E-6)
                && storedBricks(player) == 0, "Death lost the legacy limit, direction or one-time transfer");
        helper.succeed();
    }

    private static void inSeriousMode(final boolean enabled, final Runnable action) {
        final boolean original = OBServerConfig.SERIOUS_MODE.get();
        OBServerConfig.SERIOUS_MODE.set(enabled);
        try {
            action.run();
        } finally {
            OBServerConfig.SERIOUS_MODE.set(original);
        }
    }

    private static ServerPlayer serverPlayer(final GameTestHelper helper, final boolean creative) {
        final var level = helper.getLevel();
        final var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "brick_qa")) {
            @Override public boolean isCreative() { return creative; }
        };
        position(helper, player);
        return player;
    }

    private static void position(final GameTestHelper helper, final ServerPlayer player) {
        final var origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 3, 3)));
        player.setPos(origin.x, origin.y, origin.z);
    }

    private static List<ItemEntity> bricksNear(final ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(2),
                item -> item.getItem().is(Items.BRICK));
    }

    private static int storedBricks(final ServerPlayer player) {
        return OBCapabilities.getBowelBrickCount(player).orElseThrow();
    }

    private static int brickStat(final ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(OBStats.BRICKS_DROPPED.get()));
    }

    private static final class FoodTestPlayer extends FakePlayer {

        private FoodTestPlayer(final ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "clay_qa"));
        }

        private void eatOne(final ItemStack clay) {
            setItemInHand(InteractionHand.MAIN_HAND, clay);
            clay.use(level(), this, InteractionHand.MAIN_HAND);
            completeUsingItem();
        }
    }
}
