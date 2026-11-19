package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.player.OBExperience;
import art.arcane.openblocks.registry.OBEnchantments;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class ArmorEnchantmentGameTests {

    @GameTest(template = "empty")
    public static void armorEnchantmentsRetainLegacySlotsAndRarity(final GameTestHelper helper) {
        final var unstable = OBEnchantments.EXPLOSIVE.get();
        final var lastStand = OBEnchantments.LAST_STAND.get();
        helper.assertTrue(unstable.getRarity() == Enchantment.Rarity.RARE && unstable.getMaxLevel() == 3
                && lastStand.getRarity() == Enchantment.Rarity.UNCOMMON && lastStand.getMaxLevel() == 2,
                "Unstable and Last Stand must retain their rarity and highest levels");
        final var boots = Items.IRON_BOOTS.getDefaultInstance();
        final var pickaxe = Items.IRON_PICKAXE.getDefaultInstance();
        helper.assertTrue(unstable.canEnchant(boots) && !unstable.canEnchant(pickaxe)
                && lastStand.canEnchant(boots) && !lastStand.canEnchant(pickaxe), "Both are armor enchantments");
        final var table = EnchantmentHelper.getAvailableEnchantmentResults(29, boots, false);
        helper.assertTrue(table.stream().anyMatch(instance -> instance.enchantment == unstable && instance.level == 2)
                && table.stream().anyMatch(instance -> instance.enchantment == lastStand && instance.level == 2)
                && unstable.getMinCost(3) == 100, "Level II must be available from ordinary enchanting while Unstable III remains out of its range");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lastStandSpendsXpAcrossLevelsAndKeepsHalfAHeart(final GameTestHelper helper) {
        final var player = player(helper);
        equipLastStand(player);
        player.setHealth(4.0F);
        player.giveExperiencePoints(52);
        player.hurt(player.damageSources().fellOutOfWorld(), 5.0F);
        helper.assertTrue(player.getHealth() == 1.0F && OBExperience.points(player) == 2 && player.totalExperience == 2,
                "Two equipped levels must spend 50 XP points to keep the wearer at half a heart");

        player.setHealth(4.0F);
        final var reachesHalfAHeart = new LivingHurtEvent(player, player.damageSources().fall(), 3.0F);
        MinecraftForge.EVENT_BUS.post(reachesHalfAHeart);
        helper.assertTrue(!reachesHalfAHeart.isCanceled() && OBExperience.points(player) == 2,
                "Damage that already leaves half a heart must not charge XP");
        final var insufficient = new LivingHurtEvent(player, player.damageSources().fall(), 5.0F);
        MinecraftForge.EVENT_BUS.post(insufficient);
        helper.assertTrue(!insufficient.isCanceled() && OBExperience.points(player) == 2,
                "Insufficient XP must leave the hit intact without a partial charge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lastStandUsesTheConfiguredFormulaAndFallsBackOnInvalidInput(final GameTestHelper helper) {
        final String original = OBServerConfig.LAST_STAND_FORMULA.get();
        try {
            OBServerConfig.LAST_STAND_FORMULA.set("max(1, min(xp, (dmg + hp) * 2 / ench))");
            final var custom = player(helper);
            equipLastStand(custom);
            custom.setHealth(4.0F);
            custom.giveExperiencePoints(40);
            custom.hurt(custom.damageSources().fellOutOfWorld(), 5.0F);
            helper.assertTrue(custom.getHealth() == 1.0F && OBExperience.points(custom) == 31,
                    "The updated OpenCalc expression must see health, damage, combined levels, and available XP");

            OBServerConfig.LAST_STAND_FORMULA.set("max(1,");
            final var invalid = player(helper);
            equipLastStand(invalid);
            invalid.setHealth(4.0F);
            invalid.giveExperiencePoints(52);
            invalid.hurt(invalid.damageSources().fellOutOfWorld(), 5.0F);
            helper.assertTrue(invalid.getHealth() == 1.0F && OBExperience.points(invalid) == 2,
                    "An invalid formula must use the original 50 XP price for this hit");
        } finally {
            OBServerConfig.LAST_STAND_FORMULA.set(original);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lastStandRespectsCanceledOrAlteredXpTransactions(final GameTestHelper helper) {
        final var player = player(helper);
        equipLastStand(player);
        player.setHealth(4.0F);
        player.giveExperiencePoints(80);
        final var guard = new XpGuard(player);
        MinecraftForge.EVENT_BUS.register(guard);
        try {
            final var hit = new LivingHurtEvent(player, player.damageSources().fall(), 5.0F);
            MinecraftForge.EVENT_BUS.post(hit);
            helper.assertTrue(!hit.isCanceled() && player.getHealth() == 4.0F && OBExperience.points(player) == 80,
                    "A canceled XP charge must leave the damage and XP untouched");
            guard.cancel = false;
            final var discounted = new LivingHurtEvent(player, player.damageSources().fall(), 5.0F);
            MinecraftForge.EVENT_BUS.post(discounted);
            helper.assertTrue(!discounted.isCanceled() && player.getHealth() == 4.0F && OBExperience.points(player) == 80,
                    "A reduced XP offer must not purchase a partial Last Stand");
            helper.assertTrue(guard.calls == 2, "The Last Stand payment must go through Forge exactly once per attempt");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(guard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unstableBootsBounceAndConsumeOneChargeInSurvival(final GameTestHelper helper) {
        final var player = player(helper);
        final var boots = equip(player, EquipmentSlot.FEET, Items.IRON_BOOTS, OBEnchantments.EXPLOSIVE.get(), 1);
        final var powder = new ItemStack(Items.GUNPOWDER, 2);
        player.getInventory().setItem(0, powder);
        final var observer = new BlastObserver(player);
        MinecraftForge.EVENT_BUS.register(observer);
        try {
            final var shortFall = new LivingFallEvent(player, 4.0F, 1.0F);
            MinecraftForge.EVENT_BUS.post(shortFall);
            player.setShiftKeyDown(true);
            final var sneaking = new LivingFallEvent(player, 9.0F, 1.0F);
            MinecraftForge.EVENT_BUS.post(sneaking);
            player.setShiftKeyDown(false);
            helper.assertTrue(!shortFall.isCanceled() && !sneaking.isCanceled() && observer.starts == 0,
                    "Short or sneaking falls must use the usual fall handling");

            player.setDeltaMovement(new Vec3(0.1, -0.6, 0.02));
            helper.assertTrue(!player.causeFallDamage(9.0F, 1.0F, player.damageSources().fall()) && player.getHealth() == 20.0F,
                    "A charged landing must cancel fall damage");
            final Vec3 bounce = player.getDeltaMovement();
            helper.assertTrue(Math.abs(bounce.x - 1.55) < 0.001 && Math.abs(bounce.y - 1.302) < 0.001
                    && Math.abs(bounce.z - 0.31) < 0.001 && player.hurtMarked, "Bounce velocity must use both legacy multipliers and synchronize");
            helper.assertTrue(powder.getCount() == 1 && boots.getDamageValue() == 1 && observer.starts == 1 && observer.detonations == 1 && !observer.grief,
                    "One level-I bounce must spend one powder and a durability point without breaking blocks");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(observer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unstableArmorPrioritizesChestAndOnlyReactsToLegacyHits(final GameTestHelper helper) {
        final var victim = player(helper);
        final var attacker = player(helper);
        final var enchantment = OBEnchantments.EXPLOSIVE.get();
        final var chest = equip(victim, EquipmentSlot.CHEST, Items.IRON_CHESTPLATE, enchantment, 2);
        final var legs = equip(victim, EquipmentSlot.LEGS, Items.IRON_LEGGINGS, enchantment, 1);
        final var powder = new ItemStack(Items.GUNPOWDER, 4);
        victim.getInventory().setItem(0, powder);
        final var observer = new BlastObserver(victim);
        MinecraftForge.EVENT_BUS.register(observer);
        try {
            MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(victim, victim.damageSources().generic(), 2.0F));
            helper.assertTrue(powder.getCount() == 4 && observer.starts == 0, "Environmental damage must not trigger an armor blast");

            final var melee = new LivingAttackEvent(victim, victim.damageSources().playerAttack(attacker), 2.0F);
            MinecraftForge.EVENT_BUS.post(melee);
            helper.assertTrue(!melee.isCanceled() && powder.getCount() == 2 && chest.getDamageValue() == 1
                    && legs.getDamageValue() == 0 && observer.detonations == 1 && !observer.grief,
                    "On melee damage the chestplate must spend two powder, leave the leggings intact, and let the original hit continue");

            victim.setGameMode(GameType.CREATIVE);
            MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(victim, victim.damageSources().playerAttack(attacker), 2.0F));
            helper.assertTrue(powder.getCount() == 2 && chest.getDamageValue() == 1 && observer.detonations == 2,
                    "Creative armor must react if charged and keep its gunpowder and durability");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(observer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unstableLevelThreeHonorsProtectionAndGriefConfig(final GameTestHelper helper) {
        final var player = player(helper);
        final var boots = equip(player, EquipmentSlot.FEET, Items.IRON_BOOTS, OBEnchantments.EXPLOSIVE.get(), 3);
        final var firstStack = new ItemStack(Items.GUNPOWDER, 3);
        final var secondStack = new ItemStack(Items.GUNPOWDER, 1);
        player.getInventory().setItem(0, firstStack);
        player.getInventory().setItem(1, secondStack);
        final var observer = new BlastObserver(player);
        final boolean original = OBServerConfig.UNSTABLE_GRIEF.get();
        MinecraftForge.EVENT_BUS.register(observer);
        try {
            final var split = new LivingFallEvent(player, 7.0F, 1.0F);
            MinecraftForge.EVENT_BUS.post(split);
            helper.assertTrue(!split.isCanceled() && observer.starts == 0, "Level III requires four powder in a single inventory stack");

            firstStack.setCount(4);
            OBServerConfig.UNSTABLE_GRIEF.set(true);
            observer.cancel = true;
            final var denied = new LivingFallEvent(player, 7.0F, 1.0F);
            MinecraftForge.EVENT_BUS.post(denied);
            helper.assertTrue(!denied.isCanceled() && observer.grief && observer.detonations == 0 && firstStack.getCount() == 4
                    && boots.getDamageValue() == 0, "Forge must be able to deny a destructive blast without charging or bouncing the player");

            OBServerConfig.UNSTABLE_GRIEF.set(false);
            observer.cancel = false;
            final var nonDestructive = new LivingFallEvent(player, 7.0F, 1.0F);
            MinecraftForge.EVENT_BUS.post(nonDestructive);
            helper.assertTrue(nonDestructive.isCanceled() && !observer.grief && observer.detonations == 1 && firstStack.isEmpty()
                    && secondStack.getCount() == 1 && boots.getDamageValue() == 1, "Disabling grief must retain the level-III blast with its normal cost");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(observer);
            OBServerConfig.UNSTABLE_GRIEF.set(original);
        }
        helper.succeed();
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ArmorEnchantQA")) {
            @Override public boolean isInvulnerableTo(final DamageSource source) { return false; }
        };
        player.setGameMode(GameType.SURVIVAL);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        final Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static void equipLastStand(final FakePlayer player) {
        final var enchantment = OBEnchantments.LAST_STAND.get();
        equip(player, EquipmentSlot.HEAD, Items.IRON_HELMET, enchantment, 1);
        equip(player, EquipmentSlot.CHEST, Items.IRON_CHESTPLATE, enchantment, 1);
    }

    private static ItemStack equip(final FakePlayer player, final EquipmentSlot slot, final Item item,
            final Enchantment enchantment, final int level) {
        final var stack = new ItemStack(item);
        stack.enchant(enchantment, level);
        player.setItemSlot(slot, stack);
        return stack;
    }

    private static final class XpGuard {
        private final FakePlayer player;
        private boolean cancel = true;
        private int calls;

        XpGuard(final FakePlayer player) { this.player = player; }

        @SubscribeEvent
        public void onChange(final PlayerXpEvent.XpChange event) {
            if (event.getEntity() != player) return;
            calls++;
            if (cancel) event.setCanceled(true);
            else event.setAmount(-5);
        }
    }

    private static final class BlastObserver {
        private final FakePlayer player;
        private boolean grief;
        private boolean cancel;
        private int starts;
        private int detonations;

        BlastObserver(final FakePlayer player) { this.player = player; }

        @SubscribeEvent
        public void onStart(final ExplosionEvent.Start event) {
            if (event.getExplosion().getDirectSourceEntity() != player) return;
            starts++;
            grief = event.getExplosion().interactsWithBlocks();
            if (cancel) event.setCanceled(true);
        }

        @SubscribeEvent
        public void onDetonate(final ExplosionEvent.Detonate event) {
            if (event.getExplosion().getDirectSourceEntity() != player) return;
            detonations++;
            event.getAffectedEntities().clear();
        }
    }
}
