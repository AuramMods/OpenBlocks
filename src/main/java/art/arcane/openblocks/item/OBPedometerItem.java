package art.arcane.openblocks.item;

import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.registry.OBSounds;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class OBPedometerItem extends Item {

    public OBPedometerItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());

        level.playSound(null, serverPlayer.blockPosition(), OBSounds.PEDOMETER_USE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        serverPlayer.getCapability(OBCapabilities.PEDOMETER_STATE).ifPresent((state) -> {
            if (serverPlayer.isShiftKeyDown()) {
                state.reset();
                send(serverPlayer, "openblocks.misc.pedometer.tracking_reset");
                return;
            }

            if (!state.isRunning()) {
                state.start(serverPlayer);
                send(serverPlayer, "openblocks.misc.pedometer.tracking_started");
                return;
            }

            state.tick(serverPlayer);
            final OBCapabilities.PedometerReport report = state.createReport(serverPlayer);
            if (report == null) return;

            send(serverPlayer, "openblocks.misc.pedometer.start_point", formatPoint(report.startX(), report.startY(), report.startZ()));
            send(serverPlayer, "openblocks.misc.pedometer.speed", formatSpeed(report.currentSpeed()));
            send(serverPlayer, "openblocks.misc.pedometer.avg_speed", formatSpeed(report.averageSpeed()));
            send(serverPlayer, "openblocks.misc.pedometer.total_distance", formatDistance(report.totalDistance()));
            send(serverPlayer, "openblocks.misc.pedometer.straght_line_distance", formatDistance(report.straightLineDistance()));
            send(serverPlayer, "openblocks.misc.pedometer.straigh_line_speed", formatSpeed(report.straightLineSpeed()));
            send(serverPlayer, "openblocks.misc.pedometer.last_check_speed", formatSpeed(report.lastCheckSpeed()));
            send(serverPlayer, "openblocks.misc.pedometer.last_check_distance", formatDistance(report.lastCheckDistance()));
            send(serverPlayer, "openblocks.misc.pedometer.last_check_time", report.lastCheckTime());
            send(serverPlayer, "openblocks.misc.pedometer.total_time", report.totalTime());
        });

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void send(final ServerPlayer player, final String key, final Object... args) {
        player.sendSystemMessage(Component.translatable(key, args));
    }

    private static String formatPoint(final double x, final double y, final double z) {
        return String.format(Locale.ROOT, "%.1f %.1f %.1f", x, y, z);
    }

    private static String formatDistance(final double distance) {
        return String.format(Locale.ROOT, "%.3f m", distance);
    }

    private static String formatSpeed(final double speed) {
        return String.format(Locale.ROOT, "%.3f m/t", speed);
    }
}
