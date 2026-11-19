package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.command.OBFlimFlamEffects;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class FlimFlamWorldGameTests {

    @GameTest(template = "empty")
    public static void snowAndPearlLaunchWithTheirOriginalOwnersAndVelocities(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var target = player(helper);
        final Vec3 snowOrigin = target.position().add(0, 4, 0);
        final AABB snowBox = AABB.ofSize(snowOrigin, 1, 1, 1);
        helper.assertTrue(OBFlimFlamEffects.execute("snowballs", target), "The snow shower should spawn");
        final var snowballs = level.getEntitiesOfClass(Snowball.class, snowBox);
        helper.assertTrue(snowballs.size() == 200 && snowballs.stream().allMatch(ball -> ball.getOwner() == null
                        && ball.position().equals(snowOrigin) && ball.getDeltaMovement().y == 1),
                "All 200 snowballs must launch upward, ownerless, four blocks above the player");
        snowballs.forEach(Entity::discard);

        final Vec3 pearlOrigin = target.position().add(0, 1, 0);
        helper.assertTrue(OBFlimFlamEffects.execute("teleport", target), "The teleport prank should launch a pearl");
        final var pearls = level.getEntitiesOfClass(ThrownEnderpearl.class, AABB.ofSize(pearlOrigin, 1, 1, 1));
        helper.assertTrue(pearls.size() == 1 && pearls.get(0).getOwner() == target && pearls.get(0).position().equals(pearlOrigin)
                        && pearls.get(0).getDeltaMovement().y == 0.5,
                "The ender pearl must launch from one block above its owner and use normal impact teleportation");
        pearls.forEach(Entity::discard);

        final Consumer<EntityJoinLevelEvent> deny = event -> {
            if (event.getLevel() == level && (event.getEntity().position().equals(snowOrigin) && event.getEntity() instanceof Snowball
                    || event.getEntity() instanceof ThrownEnderpearl pearl && pearl.getOwner() == target)) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, deny);
        try {
            helper.assertTrue(!OBFlimFlamEffects.execute("snowballs", target) && !OBFlimFlamEffects.execute("teleport", target),
                    "Completely denied projectile spawns must fail both pranks");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mountUsesNormalRidingAndExcludesCreepersAndSquid(final GameTestHelper helper) {
        final var target = player(helper);
        target.setPos(target.getX(), 260, target.getZ());
        final var creeper = spawn(helper, EntityType.CREEPER, target.position().add(1, 0, 0));
        final var squid = spawn(helper, EntityType.SQUID, target.position().add(0, 0, 1));
        final var armorStand = spawn(helper, EntityType.ARMOR_STAND, target.position().add(-1, 0, 0));
        helper.assertTrue(!OBFlimFlamEffects.execute("mount", target), "Prank rides must exclude creepers, squid and armor stands");
        final var pig = spawn(helper, EntityType.PIG, target.position().add(0, 0, -1));
        target.setShiftKeyDown(true);
        helper.assertTrue(!OBFlimFlamEffects.execute("mount", target), "Sneaking must prevent a normal mount");
        target.setShiftKeyDown(false);
        helper.assertTrue(OBFlimFlamEffects.execute("mount", target) && target.getVehicle() == pig && pig.hasPassenger(target),
                "The eligible mob must take the player as a passenger");
        target.stopRiding();
        List.of(creeper, squid, armorStand, pig).forEach(Entity::discard);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void encasePreservesOccupiedCellsAndRollsBackDeniedPlacement(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var target = player(helper);
        final BlockPos feet = target.blockPosition();
        final BlockPos wall = feet.west();
        final BlockPos existing = feet.east();
        final BlockPos caveAir = feet.south();
        final BlockPos pane = wall.west();
        level.setBlockAndUpdate(existing, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(caveAir, Blocks.CAVE_AIR.defaultBlockState());
        level.setBlockAndUpdate(pane, Blocks.GLASS_PANE.defaultBlockState());
        final Consumer<BlockEvent.EntityMultiPlaceEvent> deny = event -> {
            if (event.getEntity() == target) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityMultiPlaceEvent.class, deny);
        try {
            helper.assertTrue(!OBFlimFlamEffects.execute("encase", target) && level.isEmptyBlock(wall)
                            && level.isEmptyBlock(feet.above(2)) && level.getBlockState(caveAir).is(Blocks.CAVE_AIR),
                    "Denied placement must restore the enclosure cells including the original air variant");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }

        helper.assertTrue(OBFlimFlamEffects.execute("encase", target) && level.getBlockState(wall).is(Blocks.DIRT)
                        && level.getBlockState(feet.above(2)).is(Blocks.DIRT) && level.getBlockState(existing).is(Blocks.CHEST)
                        && level.isEmptyBlock(feet) && level.isEmptyBlock(feet.above())
                        && level.getBlockState(pane).getValue(CrossCollisionBlock.EAST),
                "The dirt walls and roof must surround open feet and head space, preserving occupied blocks");
        level.setBlockAndUpdate(feet, Blocks.STONE.defaultBlockState());
        helper.assertTrue(OBFlimFlamEffects.execute("encase", target) && level.getBlockState(feet.above()).is(Blocks.TORCH),
                "A supported torch fits one block above the player's feet");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chargedPrankCreepersDetonateWithoutDamage(final GameTestHelper helper) {
        final var target = player(helper);
        helper.assertTrue(OBFlimFlamEffects.execute("creepers", target), "The charged creepers should spawn");
        final var bounds = new AABB(target.getX() - 10, target.getY() + 5, target.getZ() - 10,
                target.getX() + 10, target.getY() + 10, target.getZ() + 10);
        final var creepers = helper.getLevel().getEntitiesOfClass(Creeper.class, bounds);
        helper.assertTrue(creepers.size() == 15, "The prank must spawn fifteen creepers within the original spread");
        for (final Creeper creeper : creepers) {
            final var saved = new CompoundTag();
            creeper.addAdditionalSaveData(saved);
            helper.assertTrue(creeper.isPowered() && !creeper.isNoAi() && !creeper.hasCustomName()
                            && saved.contains("ExplosionRadius") && saved.getByte("ExplosionRadius") == 0,
                    "Each creeper must keep normal AI, a charged aura and zero blast radius in its saved data");
        }

        final Creeper survivor = creepers.get(0);
        final var savedEntity = new CompoundTag();
        helper.assertTrue(survivor.save(savedEntity), "A prank creeper must use the standard entity save format");
        final var reloaded = EntityType.loadEntityRecursive(savedEntity, helper.getLevel(), Function.identity());
        helper.assertTrue(reloaded instanceof Creeper loaded && loaded.isPowered()
                        && loaded.saveWithoutId(new CompoundTag()).getByte("ExplosionRadius") == 0,
                "The charged aura and zero radius must survive an entity save and load");
        creepers.subList(1, creepers.size()).forEach(Entity::discard);
        final BlockPos feet = target.blockPosition();
        final var level = helper.getLevel();
        level.setBlockAndUpdate(feet.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(feet.east(), Blocks.GLASS.defaultBlockState());
        survivor.setPos(target.getX(), target.getY(), target.getZ());
        survivor.setNoAi(true);
        final var witness = spawn(helper, EntityType.PIG, target.position());
        witness.setNoAi(true);
        final float health = witness.getHealth();
        survivor.ignite();
        helper.runAfterDelay(34, () -> {
            helper.assertTrue(survivor.isRemoved() && witness.getHealth() == health && level.getBlockState(feet.east()).is(Blocks.GLASS),
                    "An ignited prank creeper must disappear without damaging nearby blocks or mobs");
            witness.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void randomPotionsUseTwoDistinctLegacyRangesAndRespectVetoes(final GameTestHelper helper) {
        final var target = player(helper);
        final Map<MobEffect, EffectBounds> ranges = Map.of(
                MobEffects.BLINDNESS, new EffectBounds(1, 1, 300, 1200),
                MobEffects.CONFUSION, new EffectBounds(1, 1, 300, 1200),
                MobEffects.DIG_SLOWDOWN, new EffectBounds(50, 100, 300, 1200),
                MobEffects.JUMP, new EffectBounds(30, 50, 100, 300),
                MobEffects.MOVEMENT_SPEED, new EffectBounds(50, 100, 100, 300),
                MobEffects.MOVEMENT_SLOWDOWN, new EffectBounds(4, 7, 100, 300),
                MobEffects.LEVITATION, new EffectBounds(1, 1, 100, 300));
        helper.assertTrue(OBFlimFlamEffects.execute("effect", target) && target.getActiveEffectsMap().size() == 2,
                "The prank must apply two distinct effects");
        for (final var effect : target.getActiveEffects()) {
            final var range = ranges.get(effect.getEffect());
            helper.assertTrue(range != null && effect.getAmplifier() >= range.minLevel() && effect.getAmplifier() <= range.maxLevel()
                            && effect.getDuration() >= range.minTicks() && effect.getDuration() <= range.maxTicks(),
                    "Each effect must keep the original amplifier and duration limits");
        }
        target.removeAllEffects();
        final Consumer<MobEffectEvent.Applicable> deny = event -> {
            if (event.getEntity() == target) event.setResult(Event.Result.DENY);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, MobEffectEvent.Applicable.class, deny);
        try {
            helper.assertTrue(!OBFlimFlamEffects.execute("effect", target) && target.getActiveEffects().isEmpty(),
                    "The prank must respect Forge potion vetoes");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skyblockUsesLegacyCeilingAndHonorsPlacementAndTeleportVetoes(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var target = player(helper);
        target.setPos(target.getX(), 140, target.getZ());
        final BlockPos center = target.blockPosition().atY(250);
        final BlockPos floor = center.below();
        level.setBlockAndUpdate(center.west(), Blocks.OBSIDIAN.defaultBlockState());
        helper.assertTrue(!OBFlimFlamEffects.execute("skyblock", target) && level.isEmptyBlock(floor)
                        && level.getBlockState(center.west()).is(Blocks.OBSIDIAN),
                "An obstructed trap cell must reject the whole build and preserve existing blocks");
        level.setBlockAndUpdate(center.west(), Blocks.AIR.defaultBlockState());
        final Consumer<BlockEvent.EntityMultiPlaceEvent> denyBuild = event -> {
            if (event.getEntity() == target) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityMultiPlaceEvent.class, denyBuild);
        try {
            helper.assertTrue(!OBFlimFlamEffects.execute("skyblock", target) && level.isEmptyBlock(floor) && level.isEmptyBlock(center.west()),
                    "A denied ice placement must roll back the whole platform");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(denyBuild);
        }

        final var destination = new AtomicReference<Vec3>();
        final Consumer<EntityTeleportEvent> watchTeleport = event -> {
            if (event.getEntity() == target) {
                if (destination.getAndSet(event.getTarget()) == null) event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityTeleportEvent.class, watchTeleport);
        try {
            helper.assertTrue(!OBFlimFlamEffects.execute("skyblock", target) && level.isEmptyBlock(floor),
                    "A canceled teleport must leave the sky empty");
            helper.assertTrue(OBFlimFlamEffects.execute("skyblock", target) && level.getBlockState(floor).is(Blocks.ICE)
                            && level.isEmptyBlock(center) && Vec3.atBottomCenterOf(center.above()).equals(destination.get()),
                    "The platform must use the original cap and teleport one block above its center");
            for (final Direction side : Direction.Plane.HORIZONTAL) {
                helper.assertTrue(level.getBlockState(center.relative(side)).is(Blocks.ICE), "The trap must have four ice walls");
            }
        } finally {
            MinecraftForge.EVENT_BUS.unregister(watchTeleport);
            level.setBlockAndUpdate(floor, Blocks.AIR.defaultBlockState());
            for (final Direction side : Direction.Plane.HORIZONTAL) {
                level.setBlockAndUpdate(center.relative(side), Blocks.AIR.defaultBlockState());
            }
        }
        helper.succeed();
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PrankQA"));
        player.setGameMode(GameType.SURVIVAL);
        final Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        player.setPos(position.x, position.y, position.z);
        return player;
    }

    private static <T extends Entity> T spawn(final GameTestHelper helper, final EntityType<T> type, final Vec3 pos) {
        final T entity = type.create(helper.getLevel());
        entity.setPos(pos.x, pos.y, pos.z);
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Could not spawn the test entity");
        return entity;
    }

    private record EffectBounds(int minLevel, int maxLevel, int minTicks, int maxTicks) {}
}
