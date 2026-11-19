package art.arcane.openblocks.trophy;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBTrophyItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBTrophyDrops {

    private OBTrophyDrops() {}

    @SubscribeEvent
    public static void dropTrophy(final LivingDropsEvent event) {
        final var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !event.isRecentlyHit()
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

        final var type = OBTrophyType.of(entity.getType());
        if (type == null) return;
        final double chance = OBServerConfig.TROPHY_DROP_CHANCE.get();
        final double bias = level.random.nextDouble() / 4;
        if ((event.getLootingLevel() + bias) * chance <= level.random.nextDouble()) return;

        final var drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), OBTrophyItem.create(type, 0));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }
}
