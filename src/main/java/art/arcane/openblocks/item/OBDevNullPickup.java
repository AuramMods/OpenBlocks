package art.arcane.openblocks.item;

import art.arcane.openblocks.OpenBlocks;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBDevNullPickup {

    private OBDevNullPickup() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void collect(final EntityItemPickupEvent event) {
        final Player player = event.getEntity();
        final var entity = event.getItem();
        final ItemStack picked = entity.getItem();
        if (player.level().isClientSide || player.isSpectator() || event.getResult() != Event.Result.DEFAULT
                || !entity.isAlive() || picked.isEmpty() || entity.hasPickUpDelay()
                || (entity.target != null && !entity.target.equals(player.getUUID()))) return;

        final int count = picked.getCount();
        boolean matched = false;
        int remaining = count;
        final var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            final ItemStack device = inventory.getItem(slot);
            if (!(device.getItem() instanceof OBDevNullItem)) continue;
            final ItemStack stored = OBDevNullItem.getStored(device);
            if (stored.isEmpty() || !ItemStack.isSameItemSameTags(stored, picked)) continue;

            matched = true;
            final int room = Math.max(0, Math.min(inventory.getMaxStackSize(), stored.getMaxStackSize()) - stored.getCount());
            final int accepted = Math.min(remaining, room);
            if (accepted > 0) {
                stored.grow(accepted);
                remaining -= accepted;
                OBDevNullItem.setStored(device, stored);
            }
        }

        if (!matched) return;
        event.setCanceled(true);
        final int collected = count - remaining;
        if (collected > 0) {
            inventory.setChanged();
            final ItemStack accepted = picked.copyWithCount(collected);
            ForgeEventFactory.firePlayerItemPickupEvent(player, entity, accepted);
            player.awardStat(Stats.ITEM_PICKED_UP.get(accepted.getItem()), collected);
            player.onItemPickup(entity);
        }
        player.take(entity, count);
        entity.discard();
    }
}
