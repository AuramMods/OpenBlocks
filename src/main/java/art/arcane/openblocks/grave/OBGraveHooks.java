package art.arcane.openblocks.grave;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.command.OBInventoryStore;
import art.arcane.openblocks.world.OBGameRules;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBGraveHooks {

    private OBGraveHooks() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(final LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return;
        if (event.isCanceled()) return;

        final var level = player.serverLevel();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        if (!level.getGameRules().getBoolean(OBGameRules.SPAWN_GRAVES)) return;

        final List<ItemStack> drops = new ArrayList<>();
        for (final ItemEntity itemEntity : event.getDrops()) {
            final ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) drops.add(stack.copy());
        }

        if (drops.isEmpty()) return;

        try {
            final String id = OBInventoryStore.storeDroppedItems(player, "grave", drops);
            player.sendSystemMessage(Component.literal(
                    "OpenBlocks stored your grave backup as '" + id + "'."));
        } catch (final IOException e) {
            player.sendSystemMessage(Component.literal(
                    "OpenBlocks failed to store grave backup: " + e.getMessage()));
        }
    }
}
