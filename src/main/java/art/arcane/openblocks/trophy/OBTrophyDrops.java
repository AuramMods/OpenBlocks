package art.arcane.openblocks.trophy;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBTrophyItem;
import com.mojang.logging.LogUtils;
import info.openmods.calc.ExprType;
import info.openmods.calc.SingleExprEvaluator;
import info.openmods.calc.types.fp.DoubleCalculatorFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBTrophyDrops {

    private static final SingleExprEvaluator<Double, ExprType> DROP_CHANCE = SingleExprEvaluator.create(DoubleCalculatorFactory.createDefault());
    private static String loadedFormula;

    private OBTrophyDrops() {}

    @SubscribeEvent
    public static void dropTrophy(final LivingDropsEvent event) {
        final var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !event.isRecentlyHit()
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

        final var type = OBTrophyType.of(entity.getType());
        if (type == null) return;
        if (!(roll(level.random, event.getLootingLevel(), OBServerConfig.TROPHY_DROP_CHANCE.get()) > 0)) return;

        final var drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), OBTrophyItem.create(type, 0));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    private static double roll(final RandomSource random, final int looting, final double chance) {
        final String formula = OBServerConfig.TROPHY_DROP_FORMULA.get();
        if (!formula.equals(loadedFormula)) {
            loadedFormula = formula;
            DROP_CHANCE.setExpr(ExprType.INFIX, formula);
            if (!DROP_CHANCE.isExprValid()) LogUtils.getLogger().warn("Invalid trophy drop formula. Using the original drop calculation.");
        }

        final double result = DROP_CHANCE.evaluate(environment -> {
            environment.setGlobalSymbol("looting", (double) looting);
            environment.setGlobalSymbol("chance", chance);
        }, () -> defaultRoll(random, looting, chance));
        return Double.isFinite(result) ? result : defaultRoll(random, looting, chance);
    }

    private static double defaultRoll(final RandomSource random, final int looting, final double chance) {
        return (looting + random.nextDouble() / 4) * chance - random.nextDouble();
    }
}
