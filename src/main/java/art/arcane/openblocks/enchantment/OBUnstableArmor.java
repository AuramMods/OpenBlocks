package art.arcane.openblocks.enchantment;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBEnchantments;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBUnstableArmor {

    private static final EquipmentSlot[] UPPER_ARMOR = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD};
    private static final Tuning[] LEVELS = {
            new Tuning(0.1, 5.0, 5, 1, 1),
            new Tuning(0.75, 7.5, 10, 2, 2),
            new Tuning(1.0, 10.0, 5, 4, 4)
    };

    private record Tuning(double multiplier, double maxHeightBonus, float jumpPower, float hitPower, int powder) {}
    private record Charge(Tuning tuning, EquipmentSlot slot, ItemStack armor, int powderSlot, ItemStack powder) {}

    private OBUnstableArmor() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLanding(final LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !OBServerConfig.UNSTABLE_ENABLED.get()
                || player.isSpectator() || player.isShiftKeyDown() || !(event.getDistance() > 4.0F)) return;
        final Charge charge = findCharge(player, EquipmentSlot.FEET);
        if (charge == null || !explode(player, charge, charge.tuning().jumpPower())) return;

        // Use Minecraft's jump speed and synchronized player motion instead of forcing a client key.
        player.jumpFromGround();
        final double boost = charge.tuning().multiplier() + Math.min(Math.sqrt(event.getDistance()), charge.tuning().maxHeightBonus());
        player.setDeltaMovement(player.getDeltaMovement().multiply(5 * boost, boost, 5 * boost));
        player.hurtMarked = true;
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttacked(final LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !OBServerConfig.UNSTABLE_ENABLED.get()
                || player.isSpectator() || !player.isAlive() || !(event.getAmount() > 0)) return;
        final var source = event.getSource();
        if (!source.is(DamageTypes.ARROW) && !source.is(DamageTypes.PLAYER_ATTACK)
                && !source.is(DamageTypes.MOB_ATTACK) && !source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) return;
        for (final EquipmentSlot slot : UPPER_ARMOR) {
            final Charge charge = findCharge(player, slot);
            if (charge != null) {
                explode(player, charge, charge.tuning().hitPower());
                return;
            }
        }
    }

    private static Charge findCharge(final ServerPlayer player, final EquipmentSlot slot) {
        final ItemStack armor = player.getItemBySlot(slot);
        if (!(armor.getItem() instanceof ArmorItem)) return null;
        final int level = EnchantmentHelper.getItemEnchantmentLevel(OBEnchantments.EXPLOSIVE.get(), armor);
        if (level < 1 || level > LEVELS.length) return null;
        final Tuning tuning = LEVELS[level - 1];

        final var inventory = player.getInventory().items;
        for (int i = 0; i < inventory.size(); i++) {
            final ItemStack powder = inventory.get(i);
            if (powder.is(Items.GUNPOWDER) && powder.getCount() >= tuning.powder()) return new Charge(tuning, slot, armor, i, powder);
        }
        return null;
    }

    private static boolean explode(final ServerPlayer player, final Charge charge, final float power) {
        final var level = player.serverLevel();
        final boolean grief = charge.tuning() == LEVELS[2] && OBServerConfig.UNSTABLE_GRIEF.get();
        final var interaction = !grief ? Explosion.BlockInteraction.KEEP
                : level.getGameRules().getBoolean(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY)
                        ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY;
        final var pos = player.position();
        final var blast = new Explosion(level, player, pos.x, pos.y, pos.z, power, false, interaction);
        if (ForgeEventFactory.onExplosionStart(level, blast) || player.getItemBySlot(charge.slot()) != charge.armor()
                || player.getInventory().items.get(charge.powderSlot()) != charge.powder()
                || charge.powder().getCount() < charge.tuning().powder()) return false;

        if (!player.isCreative()) {
            charge.powder().shrink(charge.tuning().powder());
            charge.armor().hurtAndBreak(1, player, wearer -> wearer.broadcastBreakEvent(charge.slot()));
        }
        blast.explode();
        blast.finalizeExplosion(false);
        if (!blast.interactsWithBlocks()) blast.clearToBlow();
        // Broadcast the same effect and knockback packet as ServerLevel.explode after the charge is committed.
        for (final ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(pos) >= 4096.0) continue;
            viewer.connection.send(new ClientboundExplodePacket(pos.x, pos.y, pos.z, power, blast.getToBlow(), blast.getHitPlayers().get(viewer)));
        }
        return true;
    }
}
