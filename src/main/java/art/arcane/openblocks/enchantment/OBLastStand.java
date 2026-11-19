package art.arcane.openblocks.enchantment;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.player.OBExperience;
import art.arcane.openblocks.registry.OBEnchantments;
import com.mojang.logging.LogUtils;
import info.openmods.calc.ExprType;
import info.openmods.calc.SingleExprEvaluator;
import info.openmods.calc.types.fp.DoubleCalculatorFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBLastStand {

    private static final SingleExprEvaluator<Double, ExprType> COST = SingleExprEvaluator.create(DoubleCalculatorFactory.createDefault());
    private static String loadedFormula;

    private OBLastStand() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(final LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !OBServerConfig.LAST_STAND_ENABLED.get()
                || !(event.getAmount() > 0) || player.getHealth() - event.getAmount() >= 1.0F) return;

        int enchantmentLevels = 0;
        for (final var stack : player.getArmorSlots()) {
            enchantmentLevels += EnchantmentHelper.getItemEnchantmentLevel(OBEnchantments.LAST_STAND.get(), stack);
        }
        if (enchantmentLevels == 0) return;

        final int experience = OBExperience.points(player);
        final float cost = cost(player.getHealth(), event.getAmount(), enchantmentLevels, experience);
        if (cost <= experience && OBExperience.trySpend(player, (int) cost)) {
            player.setHealth(1.0F);
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }

    private static float cost(final double health, final double damage, final int levels, final int experience) {
        final String formula = OBServerConfig.LAST_STAND_FORMULA.get();
        if (!formula.equals(loadedFormula)) {
            loadedFormula = formula;
            COST.setExpr(ExprType.INFIX, formula);
            if (!COST.isExprValid()) LogUtils.getLogger().warn("Invalid Last Stand XP formula. Using the original cost calculation.");
        }

        final double fallback = Math.max(1, 50 * (1 - (health - damage)) / levels);
        final double result = COST.evaluate(environment -> {
            environment.setGlobalSymbol("hp", health);
            environment.setGlobalSymbol("dmg", damage);
            environment.setGlobalSymbol("ench", (double) levels);
            environment.setGlobalSymbol("xp", (double) experience);
        }, () -> fallback);
        return (float) (Double.isFinite(result) && result >= 0 ? result : fallback);
    }
}
