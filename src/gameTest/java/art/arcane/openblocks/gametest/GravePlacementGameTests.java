package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.api.OBGraveDropsEvent;
import art.arcane.openblocks.api.OBGraveSpawnEvent;
import art.arcane.openblocks.block.OBGraveBlock;
import art.arcane.openblocks.block.entity.OBGraveBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class GravePlacementGameTests {

    @GameTest(template = "empty")
    public static void gravePlacementEventCanRedirectAndCancelWithoutSpendingItems(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos origin = helper.absolutePos(new BlockPos(1, 3, 1));
        final BlockPos destination = helper.absolutePos(new BlockPos(3, 3, 3));
        level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(destination, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(destination.below(), Blocks.AIR.defaultBlockState());
        final var player = player(helper, origin);
        final var diamonds = new ItemStack(Items.DIAMOND, 13);
        final var gravestone = new ItemStack(OBItems.GRAVE.get(), 2);
        final var diamondDrop = drop(helper, origin, diamonds);
        final var graveDrop = drop(helper, origin, gravestone);
        final var sawEvent = new AtomicBoolean();
        final var cancelSpawn = new AtomicBoolean(true);
        final var cancelBase = new AtomicBoolean(true);
        final Consumer<OBGraveSpawnEvent> redirect = event -> {
            if (event.getPlayer() != player) return;
            sawEvent.set(true);
            event.setLocation(destination);
            event.setEpitaph("Here lies a tester");
            event.setDeathMessage(Component.literal("A deliberately styled message"));
            event.getLoot().get(0).setCount(8);
            if (cancelSpawn.get()) event.setCanceled(true);
        };
        final Consumer<OBGraveDropsEvent> separateGraveItem = event -> {
            if (event.getPlayer() != player) return;
            event.getDrops().stream().filter(entry -> entry.getItem() == graveDrop)
                    .forEach(entry -> entry.setAction(OBGraveDropsEvent.Action.DROP));
            event.addItem(diamondDrop);
        };
        final Consumer<BlockEvent.EntityMultiPlaceEvent> denyBase = event -> {
            if (event.getEntity() == player && cancelBase.get()) event.setCanceled(true);
        };

        final boolean required = OBServerConfig.GRAVE_REQUIRES_ITEM.get();
        final boolean addBase = OBServerConfig.GRAVE_ADD_BASE.get();
        final boolean backups = OBServerConfig.GRAVE_BACKUPS.get();
        OBServerConfig.GRAVE_REQUIRES_ITEM.set(true);
        OBServerConfig.GRAVE_ADD_BASE.set(true);
        OBServerConfig.GRAVE_BACKUPS.set(false);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OBGraveSpawnEvent.class, redirect);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OBGraveDropsEvent.class, separateGraveItem);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityMultiPlaceEvent.class, denyBase);
        try {
            helper.assertTrue(death(helper, player, diamondDrop).size() == 1 && !sawEvent.get(),
                    "A death without the required grave item must leave its drops alone");
            helper.assertTrue(death(helper, player, diamondDrop, graveDrop).size() == 2 && sawEvent.get()
                            && diamonds.getCount() == 13 && gravestone.getCount() == 2 && level.isEmptyBlock(destination),
                    "Canceling the spawn event must keep the actual drops and required item intact");
            cancelSpawn.set(false);
            helper.assertTrue(death(helper, player, diamondDrop, graveDrop).size() == 2 && gravestone.getCount() == 2
                            && level.isEmptyBlock(destination) && level.isEmptyBlock(destination.below()),
                    "Denying the placement batch must roll back both the grave and its dirt base");
            cancelBase.set(false);
            final var remaining = death(helper, player, diamondDrop, graveDrop);
            helper.assertTrue(remaining.equals(List.of(graveDrop)) && gravestone.getCount() == 1 && level.isEmptyBlock(origin)
                            && level.getBlockState(destination).getValue(OBGraveBlock.BASE) && level.getBlockState(destination.below()).is(Blocks.DIRT),
                    "A redirected death must build the grave and base together and consume exactly one carried grave");
            final var grave = (OBGraveBlockEntity) level.getBlockEntity(destination);
            final var saved = grave.saveWithoutMetadata();
            final var loot = saved.getList("Loot", Tag.TAG_COMPOUND);
            final var packet = grave.getUpdateTag();
            helper.assertTrue(loot.size() == 1 && ItemStack.of(loot.getCompound(0)).getCount() == 8
                            && grave.epitaph().equals("Here lies a tester") && saved.getString("DeathMessageComponent").contains("deliberately styled")
                            && packet.size() == 2 && packet.getBoolean("Occupied") && packet.getString("Epitaph").equals(grave.epitaph()),
                    "The event must affect the saved grave once and the update packet must contain only visible metadata");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(redirect);
            MinecraftForge.EVENT_BUS.unregister(separateGraveItem);
            MinecraftForge.EVENT_BUS.unregister(denyBase);
            OBServerConfig.GRAVE_REQUIRES_ITEM.set(required);
            OBServerConfig.GRAVE_ADD_BASE.set(addBase);
            OBServerConfig.GRAVE_BACKUPS.set(backups);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void graveHeightAndDestructiveFallbackRespectProtection(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos center = helper.absolutePos(new BlockPos(2, 3, 2));
        final var player = player(helper, center.above(9));
        final var loot = drop(helper, center, new ItemStack(Items.DIAMOND));
        final int min = OBServerConfig.GRAVE_MIN_Y.get();
        final int max = OBServerConfig.GRAVE_MAX_Y.get();
        final int range = OBServerConfig.GRAVE_SPAWN_RANGE.get();
        final boolean destructive = OBServerConfig.GRAVE_DESTRUCTIVE.get();
        final boolean backups = OBServerConfig.GRAVE_BACKUPS.get();
        OBServerConfig.GRAVE_MIN_Y.set(center.getY());
        OBServerConfig.GRAVE_MAX_Y.set(center.getY());
        OBServerConfig.GRAVE_SPAWN_RANGE.set(0);
        OBServerConfig.GRAVE_BACKUPS.set(false);
        try {
            level.setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(center.below(), Blocks.STONE.defaultBlockState());
            OBServerConfig.GRAVE_DESTRUCTIVE.set(false);
            helper.assertTrue(death(helper, player, loot).size() == 1 && level.getBlockState(center).is(Blocks.STONE),
                    "A zero-radius polite search must keep occupied terrain and return the drop");

            OBServerConfig.GRAVE_DESTRUCTIVE.set(true);
            level.setBlockAndUpdate(center, Blocks.CHEST.defaultBlockState());
            helper.assertTrue(death(helper, player, loot).size() == 1 && level.getBlockState(center).is(Blocks.CHEST),
                    "Destructive grave placement must protect existing block entities");
            level.setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
            final Consumer<BlockEvent.BreakEvent> deny = event -> {
                if (event.getPlayer() == player && event.getPos().equals(center)) event.setCanceled(true);
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, deny);
            try {
                helper.assertTrue(death(helper, player, loot).size() == 1 && level.getBlockState(center).is(Blocks.STONE),
                        "A Forge break denial must leave the stone and drop intact");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(deny);
            }

            helper.assertTrue(death(helper, player, loot).isEmpty() && level.getBlockState(center).is(OBBlocks.GRAVE.get())
                            && !level.getBlockState(center).getValue(OBGraveBlock.BASE),
                    "The height clamp must send the valid fallback grave to the configured Y and leave off the dirt model over stone");
        } finally {
            OBServerConfig.GRAVE_MIN_Y.set(min);
            OBServerConfig.GRAVE_MAX_Y.set(max);
            OBServerConfig.GRAVE_SPAWN_RANGE.set(range);
            OBServerConfig.GRAVE_DESTRUCTIVE.set(destructive);
            OBServerConfig.GRAVE_BACKUPS.set(backups);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void namedGraveFacesThePlacerAndKeepsLootDuringBlasts(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var player = player(helper, pos.east());
        player.setYRot(90);
        final var block = (OBGraveBlock) OBBlocks.GRAVE.get();
        final var stack = new ItemStack(OBItems.GRAVE.get());
        stack.setHoverName(Component.literal("A named grave"));
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        final var state = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false)));
        level.setBlockAndUpdate(pos, state);
        block.setPlacedBy(level, pos, state, player, stack);
        final var grave = (OBGraveBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(state.getValue(OBGraveBlock.FACING) == Direction.EAST && state.getValue(OBGraveBlock.BASE)
                        && state.rotate(Rotation.CLOCKWISE_90).getValue(OBGraveBlock.FACING) == Direction.SOUTH
                        && grave.epitaph().equals("A named grave") && !grave.hasLoot(),
                "Manual placement must face the player, use its custom name and start with an empty memorial");
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        final var bare = level.getBlockState(pos);
        final AABB hitbox = bare.getShape(level, pos, CollisionContext.empty()).bounds();
        helper.assertTrue(!bare.getValue(OBGraveBlock.BASE) && hitbox.maxX == 0.125 && hitbox.maxY == 0.9375
                        && level.getBlockEntity(pos) == grave && bare.getPistonPushReaction() == PushReaction.BLOCK,
                "A support change must update the model and hitbox without replacing or making the grave movable");
        final var pig = EntityType.PIG.create(level);
        helper.assertTrue(block.getExplosionResistance() == 2000.0F && !block.canEntityDestroy(bare, level, pos, pig)
                        && Block.getDrops(bare, level, pos, grave).isEmpty(),
                "Graves must keep the original resistance, entity protection and empty block drops");
        grave.initializeFromDeath(player, List.of(new ItemStack(Items.EMERALD)), "A named grave", Component.literal("A grave"));
        final var blast = new Explosion(level, null, pos.getX(), pos.getY(), pos.getZ(), 10, false, Explosion.BlockInteraction.DESTROY);
        block.onBlockExploded(bare, level, pos, blast);
        helper.assertTrue(level.getBlockEntity(pos) == grave && grave.hasLoot(), "Even a direct blast must keep occupied grave contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void graveAmbientTickSpawnsBothMobsWithProvidedSpaceAndHonorsVetoes(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var state = OBBlocks.GRAVE.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        final var grave = (OBGraveBlockEntity) level.getBlockEntity(pos);
        final AABB center = AABB.ofSize(Vec3.atCenterOf(pos), 1, 1, 1);
        final var types = new HashSet<EntityType<?>>();
        final var checked = new AtomicBoolean();
        final var deny = new AtomicBoolean(true);
        final Consumer<MobSpawnEvent.SpawnPlacementCheck> allowPosition = event -> {
            if (event.getLevel() == level && event.getPos().equals(pos) && event.getSpawnType() == MobSpawnType.MOB_SUMMONED) {
                checked.set(true);
                event.setResult(Event.Result.ALLOW);
            }
        };
        final Consumer<MobSpawnEvent.PositionCheck> gate = event -> {
            if (event.getLevel() == level && center.contains(event.getEntity().position()) && event.getSpawnType() == MobSpawnType.MOB_SUMMONED) {
                event.setResult(deny.get() ? Event.Result.DENY : Event.Result.ALLOW);
            }
        };
        final var spawnRule = level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING);
        final boolean oldSpawning = spawnRule.get();
        final boolean oldAmbient = OBServerConfig.GRAVE_AMBIENT_MOBS.get();
        final double oldRate = OBServerConfig.GRAVE_AMBIENT_RATE.get();
        final Difficulty difficulty = level.getDifficulty();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        spawnRule.set(true, level.getServer());
        OBServerConfig.GRAVE_AMBIENT_MOBS.set(true);
        OBServerConfig.GRAVE_AMBIENT_RATE.set(1.0);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, MobSpawnEvent.SpawnPlacementCheck.class, allowPosition);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, MobSpawnEvent.PositionCheck.class, gate);
        final List<Mob> nearby = new ArrayList<>();
        try {
            OBGraveBlockEntity.serverTick(level, pos, state, grave);
            helper.assertTrue(checked.get() && level.getEntitiesOfClass(Mob.class, center).isEmpty(),
                    "The grave must honor the Forge spawn veto");
            deny.set(false);
            for (int i = 0; i < 64 && types.size() < 2; i++) {
                OBGraveBlockEntity.serverTick(level, pos, state, grave);
                for (final Mob spawned : level.getEntitiesOfClass(Mob.class, center)) {
                    types.add(spawned.getType());
                    spawned.discard();
                }
            }
            helper.assertTrue(types.equals(Set.of(EntityType.SKELETON, EntityType.BAT)), "Both original ambient mobs must spawn");

            for (int i = 0; i < 5; i++) {
                final var skeleton = EntityType.SKELETON.create(level);
                skeleton.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5);
                level.addFreshEntity(skeleton);
                nearby.add(skeleton);
            }
            checked.set(false);
            OBGraveBlockEntity.serverTick(level, pos, state, grave);
            helper.assertTrue(!checked.get(), "Five nearby hostile mobs must block another attempt");
            nearby.forEach(Entity::discard);
            spawnRule.set(false, level.getServer());
            OBGraveBlockEntity.serverTick(level, pos, state, grave);
            helper.assertTrue(!checked.get(), "The mob spawning game rule must also block the attempt");
        } finally {
            nearby.forEach(Entity::discard);
            MinecraftForge.EVENT_BUS.unregister(allowPosition);
            MinecraftForge.EVENT_BUS.unregister(gate);
            OBServerConfig.GRAVE_AMBIENT_MOBS.set(oldAmbient);
            OBServerConfig.GRAVE_AMBIENT_RATE.set(oldRate);
            spawnRule.set(oldSpawning, level.getServer());
            level.getServer().setDifficulty(difficulty, true);
        }
        helper.succeed();
    }

    private static ServerPlayer player(final GameTestHelper helper, final BlockPos pos) {
        final var level = helper.getLevel();
        final var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "GravePlacementQA")) {
            @Override public void sendSystemMessage(final Component message) {}
        };
        final Vec3 point = Vec3.atBottomCenterOf(pos);
        player.setPos(point.x, point.y, point.z);
        return player;
    }

    private static ItemEntity drop(final GameTestHelper helper, final BlockPos pos, final ItemStack stack) {
        final Vec3 point = Vec3.atCenterOf(pos);
        return new ItemEntity(helper.getLevel(), point.x, point.y, point.z, stack);
    }

    private static List<ItemEntity> death(final GameTestHelper helper, final ServerPlayer player, final ItemEntity... original) {
        final var level = helper.getLevel();
        final var drops = new ArrayList<>(List.of(original));
        MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(player, level.damageSources().generic(), drops, 0, false));
        return drops;
    }
}
