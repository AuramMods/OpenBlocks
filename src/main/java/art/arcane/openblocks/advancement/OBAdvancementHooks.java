package art.arcane.openblocks.advancement;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBAdvancementHooks {

    private OBAdvancementHooks() {}

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        tickPedometer(player);
    }

    private static void tickPedometer(final ServerPlayer player) {
        player.getCapability(OBCapabilities.PEDOMETER_STATE).ifPresent((state) -> {
            if (hasPedometerInHotbar(player) && state.isRunning()) {
                state.tick(player);
            }
        });
    }

    private static boolean hasPedometerInHotbar(final ServerPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(OBItems.PEDOMETER.get())) return true;
        }

        return false;
    }
}
