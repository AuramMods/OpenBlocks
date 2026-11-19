package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBTrophyBlock;
import art.arcane.openblocks.block.entity.OBTrophyBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBTrophyItem;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.trophy.OBTrophyType;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class TrophyGameTests {

    private static final BlockPos POS = new BlockPos(2, 2, 2);

    @GameTest(template = "empty")
    public static void trophyCatalogPreservesAllVariantsAndLegacyAliases(final GameTestHelper helper) {
        final var variants = OBTrophyType.values();
        helper.assertTrue(variants.length == 44, "The port must keep all 44 legacy trophy variants");
        for (final var variant : variants) {
            final var stack = OBTrophyItem.create(variant, 0);
            helper.assertTrue(stack.is(OBItems.TROPHY.get()) && OBTrophyType.read(stack.getTag()) == variant
                    && OBTrophyType.of(variant.entityType()) == variant, "Trophy item and entity lookups disagree for " + variant);
        }
        helper.assertTrue(OBTrophyType.COW.ambientSound() == SoundEvents.COW_AMBIENT
                && OBTrophyType.of(EntityType.WITHER) == null && OBTrophyType.of(EntityType.ENDER_DRAGON) == null,
                "Ambient sound or the legacy omission of boss trophies changed");

        final var legacy = new CompoundTag();
        legacy.putString("entity_id", "minecraft:zombie_pigman");
        helper.assertTrue(OBTrophyType.read(legacy) == OBTrophyType.ZOMBIFIED_PIGLIN, "The old Pigman entity ID failed to remap");
        legacy.remove("entity_id");
        legacy.putString("entity", "Ozelot");
        helper.assertTrue(OBTrophyType.read(legacy) == OBTrophyType.OCELOT, "The old Ozelot item name failed to remap");
        legacy.remove("entity");
        legacy.putInt("trophyIndex", 41);
        helper.assertTrue(OBTrophyType.read(legacy) == OBTrophyType.EVOKER, "A legacy block ordinal did not keep its exact type");
        legacy.putInt("trophyIndex", -1);
        helper.assertTrue(OBTrophyType.readOrDefault(legacy) == OBTrophyType.DEFAULT, "Invalid metadata failed to use the default trophy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trophyMobDropsHonorLootingAndMobLootRules(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var killer = player(helper);
        final var cow = EntityType.COW.create(level);
        final var source = level.damageSources().playerAttack(killer);
        final var drops = new ArrayList<ItemEntity>();
        final var chance = OBServerConfig.TROPHY_DROP_CHANCE;
        final var formula = OBServerConfig.TROPHY_DROP_FORMULA;
        final double previousChance = chance.get();
        final String previousFormula = formula.get();
        final var mobLoot = level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
        final boolean previousRule = mobLoot.get();
        try {
            mobLoot.set(true, level.getServer());
            formula.set(formula.getDefault());
            chance.set(0.0);
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(cow, source, drops, 3, true));
            helper.assertTrue(drops.isEmpty(), "A zero drop chance added a trophy");
            chance.set(1.0);
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(cow, source, drops, 3, false));
            helper.assertTrue(drops.isEmpty(), "A kill without player credit added a trophy");
            mobLoot.set(false, level.getServer());
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(cow, source, drops, 3, true));
            helper.assertTrue(drops.isEmpty(), "A disabled mob-loot gamerule still added a trophy");

            mobLoot.set(true, level.getServer());
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(cow, source, drops, 3, true));
            helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() == 1 && drops.get(0).hasPickUpDelay()
                    && OBTrophyType.read(drops.get(0).getItem().getTag()) == OBTrophyType.COW, "The guaranteed looting roll added the wrong trophy");

            drops.clear();
            chance.set(0.25);
            formula.set("looting * chance - 0.5");
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(cow, source, drops, 2, true));
            helper.assertTrue(drops.isEmpty(), "A trophy formula result of zero awarded a drop");
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(cow, source, drops, 3, true));
            helper.assertTrue(drops.size() == 1, "The live formula did not receive looting and chance");

            drops.clear();
            chance.set(1.0);
            formula.set("let(");
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(cow, source, drops, 3, true));
            helper.assertTrue(drops.size() == 1, "An invalid formula did not fall back to the default roll");
        } finally {
            chance.set(previousChance);
            formula.set(previousFormula);
            mobLoot.set(previousRule, level.getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placedTrophyKeepsTypeFacingAndCooldownThroughBreakAndReload(final GameTestHelper helper) {
        final var player = player(helper);
        final var level = helper.getLevel();
        final var pos = helper.absolutePos(POS);
        helper.setBlock(POS.below(), Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, OBTrophyItem.create(OBTrophyType.CHICKEN, 0));
        final var click = top(pos.below());
        final var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, click);
        final var trophy = (OBTrophyBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(result.consumesAction() && player.getMainHandItem().isEmpty() && trophy != null
                && trophy.trophy() == OBTrophyType.CHICKEN && trophy.getBlockState().getValue(OBTrophyBlock.FACING) == Direction.NORTH,
                "Normal placement did not set the Chicken Trophy and its presentation toward the player");

        final var denied = use(player, trophy, InteractionHand.OFF_HAND);
        helper.assertTrue(denied == InteractionResult.PASS && trophy.cooldown() == 0, "Offhand use activated a reward");
        use(player, trophy, InteractionHand.MAIN_HAND);
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(trophy.cooldown() == 10000 && items(helper, Items.EGG).size() == 1, "Repeated use did not respect the chicken cooldown");
        for (int tick = 0; tick < 3; tick++) OBTrophyBlockEntity.tick(level, pos, trophy.getBlockState(), trophy);
        final var saved = (OBTrophyBlockEntity) BlockEntity.loadStatic(pos, trophy.getBlockState(), trophy.saveWithFullMetadata());
        helper.assertTrue(saved != null && saved.trophy() == OBTrophyType.CHICKEN && saved.cooldown() == 9997,
                "Reload lost the variant or elapsed cooldown");
        final var picked = trophy.getBlockState().getCloneItemStack(top(pos), level, pos, player);
        helper.assertTrue(OBTrophyType.read(picked.getTag()) == OBTrophyType.CHICKEN
                && picked.getTag().getInt(OBTrophyType.COOLDOWN) == 0, "Pick block did not return a fresh trophy of the right type");

        level.destroyBlock(pos, true);
        final var broken = items(helper, OBItems.TROPHY.get());
        helper.assertTrue(broken.size() == 1 && OBTrophyType.read(broken.get(0).getItem().getTag()) == OBTrophyType.CHICKEN
                && broken.get(0).getItem().getTag().getInt(OBTrophyType.COOLDOWN) == 9997, "Breaking lost or duplicated a variant or cooldown");
        final var returned = broken.get(0).getItem().copy();
        broken.get(0).discard();
        player.setItemInHand(InteractionHand.MAIN_HAND, returned);
        player.gameMode.useItemOn(player, level, returned, InteractionHand.MAIN_HAND, click);
        final var replaced = (OBTrophyBlockEntity) level.getBlockEntity(pos);
        use(player, replaced, InteractionHand.MAIN_HAND);
        helper.assertTrue(replaced.trophy() == OBTrophyType.CHICKEN && replaced.cooldown() == 9997
                && items(helper, Items.EGG).size() == 1, "Replacing a trophy reset its reward cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trophyRewardsStayAvailableWhenTheirItemSpawnIsDenied(final GameTestHelper helper) {
        final var trophy = place(helper, OBTrophyType.VINDICATOR);
        final var player = player(helper);
        final Consumer<EntityJoinLevelEvent> veto = event -> {
            if (event.getEntity() instanceof ItemEntity drop && drop.getItem().is(Items.IRON_AXE)
                    && drop.distanceToSqr(player) < 1) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(veto);
        try {
            use(player, trophy, InteractionHand.MAIN_HAND);
            helper.assertTrue(trophy.cooldown() == 0 && items(helper, Items.IRON_AXE).isEmpty(), "A canceled reward consumed the cooldown");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(veto);
        }
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(trophy.cooldown() == 20000 && items(helper, Items.IRON_AXE).size() == 1, "A permitted retry failed to give one axe");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trophyTerrainEffectsRespectForgeProtection(final GameTestHelper helper) {
        final var trophy = place(helper, OBTrophyType.MOOSHROOM);
        final var player = player(helper);
        final var level = helper.getLevel();
        final var center = trophy.getBlockPos();
        final var deny = center.east();
        for (final var pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, -1, 1))) {
            level.setBlockAndUpdate(pos, Blocks.MYCELIUM.defaultBlockState());
        }
        final Consumer<BlockEvent.EntityPlaceEvent> veto = event -> {
            if (event.getEntity() == player && event.getPos().equals(deny)) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(veto);
        try {
            use(player, trophy, InteractionHand.MAIN_HAND);
            helper.assertTrue(level.isEmptyBlock(deny) && level.getBlockState(center.west()).is(Blocks.BROWN_MUSHROOM)
                    && trophy.cooldown() == 100, "Mooshroom growth ignored a Forge denial or failed an allowed placement");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(veto);
        }

        trophy.setTrophy(OBTrophyType.SNOW_GOLEM, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        final var snow = level.getBlockState(deny);
        helper.assertTrue(trophy.cooldown() == 10 && snow.is(Blocks.SNOW) && snow.getValue(SnowLayerBlock.LAYERS) >= 1,
                "The Snow Golem Trophy did not spread a valid snow layer");

        trophy.setTrophy(OBTrophyType.SQUID, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        final var water = level.getBlockState(center.above());
        helper.assertTrue(trophy.cooldown() == 10 && water.is(Blocks.WATER) && water.getValue(LiquidBlock.LEVEL) == 1,
                "The Squid Trophy did not place flowing water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trophyEffectsUseTheOriginalMobsAndCooldowns(final GameTestHelper helper) {
        final var trophy = place(helper, OBTrophyType.SHULKER);
        final var player = player(helper);
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(trophy.cooldown() == 100 && player.getEffect(MobEffects.LEVITATION).getAmplifier() == 1,
                "Shulker levitation or its cooldown changed");
        trophy.setTrophy(OBTrophyType.CAVE_SPIDER, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == 200 && player.getEffect(MobEffects.POISON).getAmplifier() == 3,
                "Cave Spider poison changed");
        trophy.setTrophy(OBTrophyType.WITCH, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getEffect(MobEffects.BLINDNESS).getDuration() == 70, "Witch blindness changed");
        trophy.setTrophy(OBTrophyType.BLAZE, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getRemainingFireTicks() == 80, "The Blaze Trophy did not light the player for four seconds");

        final var area = new AABB(trophy.getBlockPos()).inflate(2);
        final var level = helper.getLevel();
        trophy.setTrophy(OBTrophyType.STRAY, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        final var arrows = level.getEntitiesOfClass(Arrow.class, area);
        helper.assertTrue(arrows.size() == 1 && arrows.get(0).getBaseDamage() == 0.1 && arrows.get(0).getDeltaMovement().y > 0.5,
                "Stray did not fire the original upward, low-damage arrow");
        arrows.forEach(Arrow::discard);
        trophy.setTrophy(OBTrophyType.ENDERMAN, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        final var pearls = level.getEntitiesOfClass(ThrownEnderpearl.class, area);
        helper.assertTrue(pearls.size() == 1 && pearls.get(0).getOwner() == player && trophy.cooldown() == 10,
                "Enderman pearl lost its player owner or cooldown");
        pearls.forEach(ThrownEnderpearl::discard);
        trophy.setTrophy(OBTrophyType.LLAMA, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(level.getEntitiesOfClass(LlamaSpit.class, area).size() == 1, "Llama did not spit");

        trophy.setTrophy(OBTrophyType.CREEPER, 0);
        final var neighbor = trophy.getBlockPos().east();
        level.setBlockAndUpdate(neighbor, Blocks.WHITE_WOOL.defaultBlockState());
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(level.getBlockEntity(trophy.getBlockPos()) == trophy && level.getBlockState(neighbor).is(Blocks.WHITE_WOOL),
                "The Creeper Trophy exploded with terrain damage");
        trophy.setTrophy(OBTrophyType.GUARDIAN, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(trophy.cooldown() == 100, "Guardian visual activation changed its cooldown");
        trophy.setTrophy(OBTrophyType.EVOKER, 0);
        use(player, trophy, InteractionHand.MAIN_HAND);
        helper.assertTrue(trophy.cooldown() == 100, "Evoker visual activation changed its cooldown");
        helper.succeed();
    }

    private static OBTrophyBlockEntity place(final GameTestHelper helper, final OBTrophyType type) {
        helper.setBlock(POS, OBBlocks.TROPHY.get());
        final var trophy = (OBTrophyBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(POS));
        trophy.setTrophy(type, 0);
        return trophy;
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "TrophyQA"));
        final var origin = Vec3.atBottomCenterOf(helper.absolutePos(POS.north(2)));
        player.moveTo(origin.x, origin.y, origin.z, 0, 0);
        return player;
    }

    private static List<ItemEntity> items(final GameTestHelper helper, final net.minecraft.world.item.Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(POS)).inflate(3),
                drop -> drop.getItem().is(item));
    }

    private static InteractionResult use(final FakePlayer player, final OBTrophyBlockEntity trophy, final InteractionHand hand) {
        return trophy.getBlockState().use(player.level(), player, hand, top(trophy.getBlockPos()));
    }

    private static BlockHitResult top(final BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos).relative(Direction.UP, 0.5), Direction.UP, pos, false);
    }
}
