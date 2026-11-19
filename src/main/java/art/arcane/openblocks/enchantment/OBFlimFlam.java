package art.arcane.openblocks.enchantment;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.capability.OBCapabilities.LuckData;
import art.arcane.openblocks.command.OBFlimFlamEffects;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBEnchantments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBFlimFlam {

    private static final int LUCK_MARGIN = -30;
    private static final int EFFECT_DELAY = 20 * 15;

    private OBFlimFlam() {}

    @SubscribeEvent
    public static void onCombatHurt(final LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer defender) || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || defender == attacker || !(event.getAmount() > 0) || !OBServerConfig.FLIM_FLAM_ENABLED.get()) return;

        final var enchantment = OBEnchantments.FLIM_FLAM.get();
        final int weapon = EnchantmentHelper.getItemEnchantmentLevel(enchantment, attacker.getMainHandItem());
        int armor = 0;
        for (final var stack : defender.getArmorSlots()) armor += EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        final int advantage = weapon - armor / 3;
        if (advantage == 0) return;

        final ServerPlayer victim = advantage > 0 ? defender : attacker;
        victim.getCapability(OBCapabilities.LUCK).ifPresent(luck -> {
            for (int i = 0; i < Math.abs(advantage); i++) {
                final int roll = victim.getRandom().nextInt(20) + 1;
                luck.addLuck(-roll);
                if (roll == 20) luck.setForceNext(true);
            }
            if (luck.getLuck() < LUCK_MARGIN) luck.setForceNext(true);
        });
    }

    @SubscribeEvent
    public static void onKarmaTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer player)
                || !player.isAlive() || !OBServerConfig.FLIM_FLAM_ENABLED.get()) return;
        player.getCapability(OBCapabilities.LUCK).ifPresent(luck -> {
            if (shouldDeliver(player, luck)) OBFlimFlamEffects.deliver(player, luck);
        });
    }

    private static boolean shouldDeliver(final ServerPlayer player, final LuckData luck) {
        if (luck.isForceNext()) {
            luck.setForceNext(false);
            luck.setCooldown(EFFECT_DELAY);
            return true;
        }
        if (luck.getLuck() > -LUCK_MARGIN) return false;
        if (luck.getCooldown() > 0) {
            luck.setCooldown(luck.getCooldown() - 1);
            return false;
        }
        luck.setCooldown(EFFECT_DELAY);
        final double probability = 1.5 * Math.abs(Math.atan(luck.getLuck() / 250.0) / Math.PI);
        return player.getRandom().nextDouble() < probability;
    }
}
