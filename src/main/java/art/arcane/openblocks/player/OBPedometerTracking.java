package art.arcane.openblocks.player;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBPedometerTracking {

    private OBPedometerTracking() {}

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive()) return;
        player.getCapability(OBCapabilities.PEDOMETER_STATE).ifPresent(state -> {
            if (state.isRunning() && hasPedometerReady(player)) state.tick(player);
        });
    }

    @SubscribeEvent
    public static void onDimensionChange(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(OBCapabilities.PEDOMETER_STATE).ifPresent(OBCapabilities.PedometerState::reset);
        }
    }

    private static boolean hasPedometerReady(final Player player) {
        if (player.getOffhandItem().is(OBItems.PEDOMETER.get())) return true;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (player.getInventory().getItem(slot).is(OBItems.PEDOMETER.get())) return true;
        }
        return false;
    }
}
