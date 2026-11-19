package art.arcane.openblocks.world;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBMobBlacklist {

    private OBMobBlacklist() {}

    @SubscribeEvent
    public static void onJoin(final EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity() instanceof Player) return;
        final var blacklist = OBServerConfig.DISABLED_MOBS.get();
        if (blacklist.isEmpty()) return;

        final var id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (blacklist.contains(id.toString()) || (id.getNamespace().equals("minecraft") && blacklist.contains(id.getPath()))) {
            event.setCanceled(true);
        }
    }
}
