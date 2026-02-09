package art.arcane.openblocks.command;

import art.arcane.openblocks.OpenBlocks;
import java.io.IOException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBInventoryHooks {

    private OBInventoryHooks() {}

    @SubscribeEvent
    public static void onLivingDeath(final LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return;

        try {
            final String id = OBInventoryStore.storePlayerInventory(player, "death");
            player.sendSystemMessage(Component.literal(
                    "OpenBlocks stored your death inventory as '" + id + "'. Use /ob_inventory restore "
                            + player.getGameProfile().getName() + " " + id));
        } catch (final IOException e) {
            player.sendSystemMessage(Component.literal("OpenBlocks failed to store death inventory: " + e.getMessage()));
        }
    }
}
