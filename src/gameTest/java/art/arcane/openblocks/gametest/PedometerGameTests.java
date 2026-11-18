package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class PedometerGameTests {

    private static final String MESSAGE = "openblocks.misc.pedometer.";

    @GameTest(template = "empty")
    public static void pedometerCraftsWithMixedWoodAndOneClock(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS,
                Items.REDSTONE, Items.CLOCK, Items.REDSTONE, Items.ACACIA_PLANKS, Items.JUNGLE_PLANKS, Items.BAMBOO_PLANKS);
        final var stack = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(stack.is(OBItems.PEDOMETER.get()) && stack.getCount() == 1 && stack.getMaxStackSize() == 1,
                "The Pedometer recipe should use planks, redstone, and a clock");
        grid.setItem(4, new ItemStack(Items.COMPASS));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "A compass replaced the clock in the Pedometer recipe");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pedometerSamplesReadyItemsAndReportsTheRecordedRoute(final GameTestHelper helper) {
        final var player = new MeterPlayer(helper.getLevel());
        final Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        player.setPos(start);
        final var meter = new ItemStack(OBItems.PEDOMETER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);
        use(player, InteractionHand.MAIN_HAND);
        final var state = state(player);
        final long startTick = helper.getLevel().getGameTime();

        helper.assertTrue(state.isRunning() && message(player, "tracking_started") != null, "Use did not start a trip");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(9, meter);
        helper.runAfterDelay(2, () -> {
            player.setPos(start.add(3, 0, 0));
            tick(player);
            helper.assertTrue(state.getTotalDistance() == 0 && state.createReport(player) == null,
                    "A meter in the backpack recorded a sample");

            player.getInventory().setItem(9, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, meter);
            final long firstSample = helper.getLevel().getGameTime();
            final long firstDuration = firstSample - startTick;
            tick(player);
            player.setPos(start.add(4, 0, 0));
            tick(player);
            use(player, InteractionHand.OFF_HAND);
            helper.assertTrue(state.getTotalDistance() == 3 && argument(player, "total_distance").equals("3 m")
                    && argument(player, "last_check_time").equals(firstDuration) && argument(player, "total_time").equals(firstDuration),
                    "Offhand use or the sampled legacy report was incorrect");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.PEDOMETER.get()));
            helper.runAfterDelay(4, () -> {
                final long now = helper.getLevel().getGameTime();
                player.setPos(start.add(3, 4, 0));
                tick(player);
                tick(player);
                final var report = state.createReport(player);
                helper.assertTrue(report.totalDistance() == 7 && report.straightLineDistance() == 5
                        && report.lastCheckDistance() == 4 && report.totalTime() == now - startTick
                        && report.lastCheckTime() == now - firstSample && report.currentSpeed() == 4.0 / (now - firstSample)
                        && report.averageSpeed() == 7.0 / (now - startTick)
                        && report.straightLineSpeed() == 5.0 / (now - startTick), "The two-leg route produced incorrect distance or speed");
                helper.assertTrue(state.createReport(player).lastCheckSpeed() == 0, "A repeated check on the same sample advanced the trip");

                player.setShiftKeyDown(true);
                use(player, InteractionHand.OFF_HAND);
                helper.assertTrue(!state.isRunning() && state.getTotalDistance() == 0
                        && message(player, "tracking_reset") != null, "Sneak-use did not clear the trip");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void pedometerReadsPreviouslySavedTripAndFormatsLegacyUnits(final GameTestHelper helper) {
        final var player = new MeterPlayer(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.PEDOMETER.get()));
        final var oldData = new CompoundTag();
        oldData.putBoolean("running", true);
        oldData.putDouble("total_distance", 7);
        oldData.putDouble("current_speed", 0.125);
        oldData.putLong("start_ticks", 100);
        oldData.putDouble("start_x", -80);
        oldData.putDouble("start_y", 70);
        oldData.putDouble("start_z", 108);
        oldData.putDouble("prev_x", -77);
        oldData.putDouble("prev_y", 74);
        oldData.putDouble("prev_z", 108);
        oldData.putLong("prev_tick_time", 132);
        oldData.putDouble("last_check_x", -77);
        oldData.putDouble("last_check_y", 70);
        oldData.putDouble("last_check_z", 108);
        oldData.putLong("last_check_time", 120);
        state(player).deserializeNBT(oldData);
        helper.assertTrue(state(player).serializeNBT().equals(oldData), "The saved 1.20.1 state changed shape or lost its samples");

        use(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.messages.size() == 11 && player.messages.get(0).getString().isEmpty(), "The report is missing a separator or entries");
        helper.assertTrue(argument(player, "start_point").equals("-80.0 70.0 108.0")
                && argument(player, "speed").equals("0.13 m/tick")
                && argument(player, "avg_speed").equals("0.22 m/tick")
                && argument(player, "total_distance").equals("7 m")
                && argument(player, "straght_line_distance").equals("5 m")
                && argument(player, "straigh_line_speed").equals("0.16 m/tick")
                && argument(player, "last_check_distance").equals("4 m")
                && argument(player, "last_check_speed").equals("0.33 m/tick")
                && argument(player, "last_check_time").equals(12L) && argument(player, "total_time").equals(32L),
                "The old route did not produce the original units and report values");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pedometerResetsOnDeathAndDimensionChange(final GameTestHelper helper) {
        final var original = new MeterPlayer(helper.getLevel());
        final var afterDeath = new MeterPlayer(helper.getLevel());
        state(original).start(original);
        OBCapabilities.addLuck(original, 5);
        original.invalidateCaps();
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(afterDeath, original, true));
        helper.assertTrue(!state(afterDeath).isRunning(), "A replacement player carried a route through death");
        helper.assertTrue(OBCapabilities.getLuck(afterDeath) == 5, "The clone lost luck from the invalidated original");

        state(afterDeath).start(afterDeath);
        OBCapabilities.setBowelBrickCount(afterDeath, 3);
        final var previousHandle = afterDeath.getCapability(OBCapabilities.PEDOMETER_STATE);
        afterDeath.invalidateCaps();
        helper.assertTrue(!previousHandle.isPresent() && !afterDeath.getCapability(OBCapabilities.PEDOMETER_STATE).isPresent(),
                "The old capability handle survived player invalidation");
        afterDeath.reviveCaps();
        helper.assertTrue(state(afterDeath).isRunning() && OBCapabilities.getLuck(afterDeath) == 5
                && OBCapabilities.getBowelBrickCount(afterDeath).orElse(-1) == 3,
                "Reviving the same player lost Pedometer, luck, or stored brick data");
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(afterDeath, Level.OVERWORLD, Level.NETHER));
        helper.assertTrue(!state(afterDeath).isRunning() && state(afterDeath).getTotalDistance() == 0,
                "The Pedometer carried its origin into a different dimension");

        final var fromEnd = new MeterPlayer(helper.getLevel().getServer().getLevel(Level.END));
        final var afterCredits = new MeterPlayer(helper.getLevel());
        state(fromEnd).start(fromEnd);
        OBCapabilities.setBowelBrickCount(fromEnd, 3);
        fromEnd.invalidateCaps();
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(afterCredits, fromEnd, false));
        helper.assertTrue(!state(afterCredits).isRunning(), "Returning from the End carried over the old route");
        helper.assertTrue(OBCapabilities.getBowelBrickCount(afterCredits).orElse(-1) == 3, "Returning from the End lost stored bricks");
        helper.succeed();
    }

    private static void tick(final ServerPlayer player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }

    private static OBCapabilities.PedometerState state(final ServerPlayer player) {
        return player.getCapability(OBCapabilities.PEDOMETER_STATE).orElseThrow(IllegalStateException::new);
    }

    private static void use(final ServerPlayer player, final InteractionHand hand) {
        player.getItemInHand(hand).use(player.level(), player, hand);
    }

    private static TranslatableContents message(final MeterPlayer player, final String key) {
        for (final Component component : player.messages) {
            if (component.getContents() instanceof TranslatableContents contents && contents.getKey().equals(MESSAGE + key)) return contents;
        }
        return null;
    }

    private static Object argument(final MeterPlayer player, final String key) {
        return message(player, key).getArgs()[0];
    }

    private static final class MeterPlayer extends ServerPlayer {

        private final List<Component> messages = new ArrayList<>();

        private MeterPlayer(final ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "meter_qa"));
        }

        @Override
        public void sendSystemMessage(final Component message) {
            messages.add(message);
        }
    }
}
