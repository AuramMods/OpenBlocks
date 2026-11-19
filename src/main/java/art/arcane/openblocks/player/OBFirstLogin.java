package art.arcane.openblocks.player;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBFirstLogin {

    private static final String OPENBLOCKS_PLAYER_DATA = "OpenBlocks";
    private static final String GIVEN_MANUAL = "givenManual";

    private OBFirstLogin() {}

    @SubscribeEvent
    public static void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || !OBServerConfig.GIVE_INFO_BOOK.get()) return;

        final var data = player.getPersistentData();
        final var persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        final var openBlocks = persisted.getCompound(OPENBLOCKS_PLAYER_DATA);
        if (openBlocks.getBoolean(GIVEN_MANUAL)) return;

        final var manual = new ItemStack(OBItems.INFO_BOOK.get());
        if (!player.getInventory().add(manual)) {
            final var dropped = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), manual);
            dropped.setTarget(player.getUUID());
            dropped.setDefaultPickUpDelay();
            if (!player.serverLevel().addFreshEntity(dropped)) return;
        }

        openBlocks.putBoolean(GIVEN_MANUAL, true);
        persisted.put(OPENBLOCKS_PLAYER_DATA, openBlocks);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
