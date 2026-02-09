package art.arcane.openblocks.advancement;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBAdvancementHooks {

    private OBAdvancementHooks() {}

    @SubscribeEvent
    public static void onItemUseFinished(final LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItem().is(OBItems.TASTY_CLAY.get())) return;

        OBCapabilities.getBowelBrickCount(player).ifPresent((count) ->
                OBCapabilities.setBowelBrickCount(player, count + 1));
    }

    @SubscribeEvent
    public static void onItemToss(final ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!event.getEntity().getItem().is(Items.BRICK)) return;

        if (player.isCreative()) {
            OBCriterions.BRICK_DROPPED.trigger(player);
            return;
        }

        OBCapabilities.getBowelBrickCount(player).ifPresent((count) -> {
            if (count <= 0) return;
            OBCapabilities.setBowelBrickCount(player, count - 1);
            OBCriterions.BRICK_DROPPED.trigger(player);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        final int depth = approximateDevNullDepth(player);
        if (depth > 0) OBCriterions.DEV_NULL_STACK.trigger(player, depth);
    }

    // Placeholder while nested dev-null inventory semantics are not ported.
    private static int approximateDevNullDepth(final ServerPlayer player) {
        int depth = 0;

        for (final ItemStack stack : player.getInventory().items) {
            if (stack.is(OBItems.DEV_NULL.get())) depth += stack.getCount();
        }

        for (final ItemStack stack : player.getInventory().offhand) {
            if (stack.is(OBItems.DEV_NULL.get())) depth += stack.getCount();
        }

        for (final ItemStack stack : player.getInventory().armor) {
            if (stack.is(OBItems.DEV_NULL.get())) depth += stack.getCount();
        }

        return depth;
    }
}
