package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBGoldenEggBlockEntity;
import art.arcane.openblocks.block.entity.OBGoldenEggBlockEntity.Stage;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.entity.OBMiniMe;
import art.arcane.openblocks.entity.OBMountedBlock;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class GoldenEggGameTests {

    private static final BlockPos EGG = new BlockPos(2, 2, 2);

    @GameTest(template = "empty")
    public static void goldenEggCraftsAndRecordsThePlacer(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT,
                Items.GOLD_INGOT, Items.EGG, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT);
        final var stack = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(stack.is(OBItems.GOLDEN_EGG.get()), "Golden Egg did not craft from eight ingots and one egg");

        final var actor = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "EggQA"));
        helper.setBlock(EGG.below(), Blocks.STONE);
        final var pos = helper.absolutePos(EGG);
        final var support = pos.below();
        actor.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5, 0, 0);
        actor.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final var hit = new BlockHitResult(Vec3.atCenterOf(support).relative(Direction.UP, 0.5), Direction.UP, support, false);
        helper.assertTrue(actor.gameMode.useItemOn(actor, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit).consumesAction()
                && stack.isEmpty(), "The Golden Egg did not place through the normal item interaction");

        final var egg = (OBGoldenEggBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(egg.owner() != null && actor.getUUID().equals(egg.owner().getId()) && egg.stage() == Stage.INERT,
                "The placed Golden Egg failed to remember its owner or skipped its inert stage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void goldenEggResumesAStageAndRetriesDeniedHatching(final GameTestHelper helper) {
        final var profile = new GameProfile(UUID.randomUUID(), "EggOwner");
        final var egg = place(helper, Stage.FLOATING, 399, profile);
        final var pos = egg.getBlockPos();
        final var level = helper.getLevel();
        final var loaded = (OBGoldenEggBlockEntity) BlockEntity.loadStatic(pos, egg.getBlockState(), egg.saveWithFullMetadata());
        helper.assertTrue(loaded != null && loaded.stageAge() == 399 && loaded.stage() == Stage.FLOATING
                && loaded.owner().getId().equals(profile.getId()), "A raised egg lost its stage, age, or owner on load");
        helper.assertTrue(loaded.offset(1) == 5, "The egg did not rise five blocks");

        tick(helper, egg, 1);
        helper.assertTrue(egg.stage() == Stage.FALLING && egg.offset(0) == 5 && egg.offset(1) < 5, "The egg failed to start dropping");
        tick(helper, egg, 9);
        helper.assertTrue(egg.stage() == Stage.FALLING, "The egg hatched before the ten-tick drop completed");

        final Consumer<EntityJoinLevelEvent> denySpawn = event -> {
            if (event.getEntity() instanceof OBMiniMe && event.getEntity().blockPosition().equals(pos)) event.setCanceled(true);
        };
        final AtomicInteger explosions = new AtomicInteger();
        final Consumer<ExplosionEvent.Start> observeBlast = event -> {
            if (BlockPos.containing(event.getExplosion().getPosition()).equals(pos)) {
                explosions.incrementAndGet();
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(denySpawn);
        MinecraftForge.EVENT_BUS.addListener(observeBlast);
        try {
            try {
                tick(helper, egg, 1);
                helper.assertTrue(egg.stage() == Stage.EXPLODING && level.getBlockEntity(pos) == egg
                        && explosions.get() == 0, "A canceled companion spawn consumed the egg or exploded");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(denySpawn);
            }
            tick(helper, egg, 20);
            final var companions = level.getEntitiesOfClass(OBMiniMe.class, new AABB(pos).inflate(3));
            helper.assertTrue(level.isEmptyBlock(pos) && explosions.get() == 1 && companions.size() == 1
                    && companions.get(0).owner().getId().equals(profile.getId()) && companions.get(0).getHealth() == 10.0F,
                    "The retried hatch failed to produce one living companion with its owner's profile and one explosion");
            companions.forEach(OBMiniMe::discard);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(observeBlast);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 90)
    public static void goldenEggReleasesStoredCargoAfterBreaking(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var egg = place(helper, Stage.FLOATING, 1, new GameProfile(UUID.randomUUID(), "EggLifter"));
        final var source = helper.absolutePos(EGG.east());
        helper.setBlock(EGG.east().below(), Blocks.OBSIDIAN);
        helper.setBlock(EGG.east(), Blocks.CHEST);
        final var chest = (ChestBlockEntity) level.getBlockEntity(source);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 7));

        final var enabled = OBServerConfig.GOLDEN_EGG_PICK_BLOCKS;
        final boolean previous = enabled.get();
        try {
            enabled.set(false);
            helper.assertTrue(!egg.lift(level, source) && level.getBlockEntity(source) == chest, "The disabled lift moved a chest");
            enabled.set(true);
            final Consumer<BlockEvent.BreakEvent> veto = event -> {
                if (event.getPos().equals(source)) event.setCanceled(true);
            };
            MinecraftForge.EVENT_BUS.addListener(veto);
            try {
                helper.assertTrue(!egg.lift(level, source) && level.getBlockEntity(source) == chest, "A protected chest was lifted");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(veto);
            }
            helper.assertTrue(egg.lift(level, source) && level.isEmptyBlock(source), "The egg could not lift a supported, permitted chest");
        } finally {
            enabled.set(previous);
        }
        final var lifted = level.getEntitiesOfClass(OBMountedBlock.class, new AABB(source).inflate(1)).stream().findFirst().orElseThrow();
        lifted.tick();
        helper.assertTrue(lifted.getY() > source.getY() && !lifted.isPickable(), "Lifted cargo did not float away from the surface");
        final var cargoTag = lifted.saveWithoutId(new CompoundTag());
        final var reloaded = OBEntities.MOUNTED_BLOCK.get().create(level);
        lifted.discard();
        reloaded.load(cargoTag);
        level.addFreshEntity(reloaded);
        final double height = reloaded.getY();
        reloaded.tick();
        helper.assertTrue(reloaded.getY() > height && !reloaded.isPickable(), "A saved lift detached from its egg prematurely");
        level.removeBlock(egg.getBlockPos(), false);

        helper.succeedWhen(() -> {
            final var returned = level.getBlockEntity(source);
            helper.assertTrue(returned instanceof ChestBlockEntity box && box.getItem(0).is(Items.DIAMOND) && box.getItem(0).getCount() == 7,
                    "Falling cargo did not return all seven diamonds to one chest");
            helper.assertTrue(reloaded.isRemoved() && level.getEntitiesOfClass(ItemEntity.class, new AABB(source).inflate(2)).isEmpty(),
                    "Cargo placement left a duplicate entity or spilled inventory");
        });
    }

    @GameTest(template = "empty")
    public static void miniMeSavesProfileAndDismountCooldown(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var profile = new GameProfile(UUID.randomUUID(), null);
        final var mini = OBEntities.MINI_ME.get().create(level);
        final var rider = EntityType.PIG.create(level);
        mini.setOwner(profile);
        helper.assertTrue(rider.startRiding(mini, true), "Mini-Me did not allow a passenger");
        rider.stopRiding();
        helper.assertTrue(mini.pickupCooldown() >= 1199 && mini.getPassengersRidingOffset() > 1,
                "Mini-Me did not impose the legacy one-minute cooldown or carry the rider overhead");
        final var saved = mini.saveWithoutId(new CompoundTag());
        final var loaded = OBEntities.MINI_ME.get().create(level);
        loaded.load(saved);
        helper.assertTrue(loaded.owner().getId().equals(profile.getId()) && loaded.pickupCooldown() == mini.pickupCooldown()
                && loaded.isBaby() && NbtUtils.readGameProfile(loaded.appearance()).getId().equals(profile.getId()),
                "The companion lost its profile, riding cooldown, model age, or client skin data after loading");

        loaded.setCustomName(Component.literal("OtherPlayer"));
        loaded.tick();
        helper.assertTrue(loaded.owner().getName().equals("OtherPlayer")
                && NbtUtils.readGameProfile(loaded.appearance()).getName().equals("OtherPlayer"),
                "Renaming did not update the companion's skin profile");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void miniMeForagesVanillaFlowersAndTorches(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var mini = OBEntities.MINI_ME.get().create(level);
        final var center = helper.absolutePos(EGG);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            final var pos = center.offset(x, 0, z);
            level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
            if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                level.setBlockAndUpdate(pos, Blocks.GLASS.defaultBlockState());
                level.setBlockAndUpdate(pos.above(), Blocks.GLASS.defaultBlockState());
            } else if (x != 0 || z != 0) {
                level.setBlockAndUpdate(pos, (x + z) % 2 == 0 ? Blocks.POPPY.defaultBlockState() : Blocks.TORCH.defaultBlockState());
            }
        }
        mini.getRandom().setSeed(432L);
        mini.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 0);
        level.addFreshEntity(mini);
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(3)).stream()
                    .anyMatch(item -> item.getItem().is(Items.POPPY) || item.getItem().is(Items.TORCH)),
                    "Mini-Me failed to navigate to and harvest a flower or torch");
            mini.discard();
        });
    }

    private static OBGoldenEggBlockEntity place(final GameTestHelper helper, final Stage stage, final int age, final GameProfile profile) {
        helper.setBlock(EGG, OBBlocks.GOLDEN_EGG.get());
        final var egg = (OBGoldenEggBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(EGG));
        final var tag = NbtUtils.writeGameProfile(new CompoundTag(), profile);
        final var data = new CompoundTag();
        data.put("Owner", tag);
        data.putString("Stage", stage.name());
        data.putInt("StageAge", age);
        egg.load(data);
        return egg;
    }

    private static void tick(final GameTestHelper helper, final OBGoldenEggBlockEntity egg, final int count) {
        for (int tick = 0; tick < count; tick++) {
            OBGoldenEggBlockEntity.tick(helper.getLevel(), egg.getBlockPos(), egg.getBlockState(), egg);
        }
    }
}
