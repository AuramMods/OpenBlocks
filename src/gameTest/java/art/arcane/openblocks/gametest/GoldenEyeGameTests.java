package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBGoldenEye;
import art.arcane.openblocks.item.OBGoldenEyeItem;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class GoldenEyeGameTests {

    private static final BlockPos CENTER = new BlockPos(2, 2, 2);

    @GameTest(template = "empty")
    public static void goldenEyeOriginalRecipeProducesAnUnchargedEye(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.GOLD_NUGGET, Items.GOLD_NUGGET, Items.GOLD_NUGGET,
                Items.GOLD_NUGGET, Items.ENDER_EYE, Items.GOLD_NUGGET, Items.GOLD_NUGGET, Items.GOLD_NUGGET, Items.GOLD_NUGGET);
        final var eye = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(eye.is(OBItems.GOLDEN_EYE.get()) && eye.getMaxDamage() == 100 && eye.getMaxStackSize() == 1
                && eye.getDamageValue() == 100, "The original recipe must produce a depleted single eye");
        final var targets = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).getTagOrEmpty(OBGoldenEyeItem.TARGETS);
        final var keys = StreamSupport.stream(targets.spliterator(), false).flatMap(holder -> holder.unwrapKey().stream()).toList();
        helper.assertTrue(keys.containsAll(List.of(BuiltinStructures.STRONGHOLD, BuiltinStructures.WOODLAND_MANSION,
                BuiltinStructures.OCEAN_MONUMENT, BuiltinStructures.MINESHAFT, BuiltinStructures.MINESHAFT_MESA,
                BuiltinStructures.VILLAGE_PLAINS, BuiltinStructures.VILLAGE_DESERT, BuiltinStructures.VILLAGE_SAVANNA,
                BuiltinStructures.VILLAGE_SNOWY, BuiltinStructures.VILLAGE_TAIGA, BuiltinStructures.DESERT_PYRAMID,
                BuiltinStructures.JUNGLE_TEMPLE, BuiltinStructures.SWAMP_HUT, BuiltinStructures.IGLOO,
                BuiltinStructures.FORTRESS, BuiltinStructures.END_CITY)),
                "The target tag omitted a legacy structure family");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void goldenEyePearlsRechargePerSlotAndPreserveTheLock(final GameTestHelper helper) {
        final var eye = locked(helper.absolutePos(CENTER).east(50), 45);
        eye.getOrCreateTag().putString("Dimension", "minecraft:overworld");
        eye.setHoverName(Component.literal("My survey eye"));
        final var grid = ComponentRecipeGameTests.input(helper);
        grid.setItem(0, eye);
        grid.setItem(1, new ItemStack(Items.ENDER_PEARL, 5));
        grid.setItem(4, new ItemStack(Items.ENDER_PEARL));
        final var result = ComponentRecipeGameTests.craft(helper, grid);
        final var expected = eye.copy();
        expected.setDamageValue(25);
        helper.assertTrue(ItemStack.isSameItemSameTags(expected, result) && eye.getDamageValue() == 45
                && result.getCount() == 1, "Two occupied pearl slots must add twenty charges and retain the lock and name");

        grid.setItem(2, eye.copy());
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "The recharge recipe accepted a second eye");
        grid.setItem(2, ItemStack.EMPTY);
        eye.setDamageValue(19);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "An overfilled recharge accepted two pearls");
        eye.setDamageValue(20);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).getDamageValue() == 0, "An exact refill failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void goldenEyeFailedUsesKeepTheEyeAndItsCharges(final GameTestHelper helper) {
        final var player = player(helper);
        final var blank = new ItemStack(OBItems.GOLDEN_EYE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, blank);
        helper.assertTrue(use(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && player.getMainHandItem() == blank,
                "An unlocked eye was thrown");
        final var eye = locked(helper.absolutePos(CENTER).east(50), 100);
        player.setItemInHand(InteractionHand.MAIN_HAND, eye);
        helper.assertTrue(use(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && eye.getDamageValue() == 100,
                "A depleted eye was thrown");
        eye.setDamageValue(30);
        eye.getOrCreateTag().putString("Dimension", "minecraft:the_nether");
        helper.assertTrue(use(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && eye.getDamageValue() == 30,
                "An eye pointed toward coordinates in a different dimension");
        player.setItemInHand(InteractionHand.OFF_HAND, eye.copy());
        helper.assertTrue(use(player, InteractionHand.OFF_HAND) == InteractionResult.PASS, "An offhand eye intercepted a use");

        player.setShiftKeyDown(true);
        final var before = eye.getTag().copy();
        final var expected = helper.getLevel().findNearestMapStructure(OBGoldenEyeItem.TARGETS, player.blockPosition(), 100, false);
        helper.assertTrue(use(player, InteractionHand.MAIN_HAND).consumesAction(), "Sneak-use did not search for a structure");
        final var learned = eye.getTag();
        if (expected == null) {
            helper.assertTrue(before.equals(learned), "An unsuccessful search erased the previous lock");
        } else {
            helper.assertTrue(learned.getInt("X") == expected.getX() && learned.getInt("Y") == expected.getY()
                    && learned.getInt("Z") == expected.getZ() && eye.getDamageValue() == 30
                    && learned.getString("Dimension").equals(helper.getLevel().dimension().location().toString()),
                    "The Golden Eye did not record the vanilla locate result and its dimension without spending a charge");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void goldenEyeFlightsUseTheSavedDirectionAndReturnTheLastCharge(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var player = player(helper);
        final var origin = player.getEyePosition();
        final BlockPos target = BlockPos.containing(origin).east(40);
        final var eye = locked(target, 99);
        eye.setHoverName(Component.literal("Last charge"));
        player.setItemInHand(InteractionHand.MAIN_HAND, eye);
        helper.assertTrue(use(player, InteractionHand.MAIN_HAND).consumesAction() && player.getMainHandItem().isEmpty(),
                "The charged eye never left the player's hand");

        final var projectile = flight(helper, origin);
        tick(projectile, 61);
        final var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(origin, origin.add(6, 0, 0)).inflate(1),
                entity -> entity.getItem().is(OBItems.GOLDEN_EYE.get()));
        helper.assertTrue(projectile.isRemoved() && drops.size() == 1, "Expiration failed to return one intact eye");
        final var returned = drops.get(0);
        helper.assertTrue(returned.getX() > origin.x + 5.9 && Math.abs(returned.getY() - origin.y) < 0.01,
                "A far structure did not send the eye six blocks in its horizontal direction");
        final ItemStack spent = eye.copy();
        spent.setDamageValue(100);
        helper.assertTrue(ItemStack.isSameItemSameTags(spent, returned.getItem()), "The final charge destroyed the eye or lost metadata");
        returned.discard();

        final var downward = OBEntities.GOLDEN_EYE.get().create(level);
        downward.launch(player, BlockPos.containing(origin), locked(target, 83));
        level.addFreshEntity(downward);
        tick(downward, 20);
        helper.assertTrue(downward.getY() < origin.y - 2.5 && Math.abs(downward.getX() - origin.x) < 0.01,
                "A nearby structure did not send the eye downward");
        downward.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void goldenEyeRetainsItsItemOnSpawnDenialAndResumesSavedFlight(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var player = player(helper);
        final var origin = player.getEyePosition();
        final var eye = locked(BlockPos.containing(origin).east(40), 21);
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, eye);
        final Consumer<EntityJoinLevelEvent> denyFlight = event -> {
            if (event.getEntity() instanceof OBGoldenEye && event.getEntity().distanceToSqr(origin) < 2) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(denyFlight);
        try {
            helper.assertTrue(use(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL
                    && player.getMainHandItem() == eye && eye.getDamageValue() == 21, "Canceled spawn lost an item or charge");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(denyFlight);
        }

        helper.assertTrue(use(player, InteractionHand.MAIN_HAND).consumesAction() && player.getMainHandItem().isEmpty(),
                "Creative use did not transfer exactly one item into the projectile");
        final var projectile = flight(helper, origin);
        tick(projectile, 3);
        final var snapshot = projectile.saveWithoutId(new CompoundTag());
        projectile.discard();
        final var reloaded = OBEntities.GOLDEN_EYE.get().create(level);
        reloaded.load(snapshot);
        helper.assertTrue(level.addFreshEntity(reloaded) && reloaded.getItem().getDamageValue() == 22,
                "An in-flight snapshot lost its spent charge");
        final double savedX = reloaded.getX();
        tick(reloaded, 57);
        helper.assertTrue(!reloaded.isRemoved() && reloaded.getX() > savedX,
                "A resumed eye dropped before its remaining flight or lost its direction");

        final Consumer<EntityJoinLevelEvent> denyDrop = event -> {
            if (event.getEntity() instanceof ItemEntity drop && drop.getItem().is(OBItems.GOLDEN_EYE.get())
                    && drop.distanceToSqr(reloaded) < 1) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(denyDrop);
        try {
            tick(reloaded, 1);
            helper.assertTrue(!reloaded.isRemoved() && reloaded.getItem().getDamageValue() == 22, "A canceled return discarded the spent eye");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(denyDrop);
        }
        tick(reloaded, 11);
        final var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(reloaded.blockPosition()).inflate(1),
                entity -> entity.getItem().is(OBItems.GOLDEN_EYE.get()));
        helper.assertTrue(reloaded.isRemoved() && drops.size() == 1 && drops.get(0).getItem().getDamageValue() == 22,
                "The retried return failed to yield exactly one spent eye");
        drops.get(0).discard();

        player.setItemInHand(InteractionHand.MAIN_HAND, eye.copyWithCount(3));
        helper.assertTrue(use(player, InteractionHand.MAIN_HAND).consumesAction(), "An eye from an older stack could not be thrown");
        final var legacyProjectile = flight(helper, origin);
        helper.assertTrue(player.getMainHandItem().getCount() == 2 && player.getMainHandItem().getDamageValue() == 21
                && legacyProjectile.getItem().getCount() == 1 && legacyProjectile.getItem().getDamageValue() == 22,
                "Throwing from an older stacked slot changed the remaining eyes or duplicated one in Creative");
        legacyProjectile.discard();
        helper.succeed();
    }

    private static ItemStack locked(final BlockPos target, final int damage) {
        final var eye = new ItemStack(OBItems.GOLDEN_EYE.get());
        eye.setDamageValue(damage);
        final var tag = eye.getOrCreateTag();
        tag.putString("Structure", "Village");
        tag.putInt("X", target.getX());
        tag.putInt("Y", target.getY());
        tag.putInt("Z", target.getZ());
        return eye;
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "GoldenEyeQA"));
        final var origin = Vec3.atBottomCenterOf(helper.absolutePos(CENTER).above(4));
        player.moveTo(origin.x, origin.y, origin.z, 0, 0);
        return player;
    }

    private static InteractionResult use(final FakePlayer player, final InteractionHand hand) {
        return player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand);
    }

    private static OBGoldenEye flight(final GameTestHelper helper, final Vec3 origin) {
        return helper.getLevel().getEntitiesOfClass(OBGoldenEye.class, new AABB(origin, origin).inflate(2)).stream().findFirst().orElseThrow();
    }

    private static void tick(final OBGoldenEye eye, final int ticks) {
        for (int t = 0; t < ticks; t++) eye.tick();
    }
}
