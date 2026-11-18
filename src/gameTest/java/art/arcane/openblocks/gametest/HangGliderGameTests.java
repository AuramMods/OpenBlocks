package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.entity.OBHangGlider;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class HangGliderGameTests {

    @GameTest(template = "empty")
    public static void hangGliderUsesTheSameHandToEquipAndStow(final GameTestHelper helper) {
        final FakePlayer player = player(helper);
        final ItemStack main = new ItemStack(OBItems.HANG_GLIDER.get(), 2);
        final ItemStack off = new ItemStack(OBItems.HANG_GLIDER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        player.setOnGround(true);

        use(player, InteractionHand.OFF_HAND);
        final OBHangGlider glider = OBHangGlider.activeFor(player);
        helper.assertTrue(glider != null && glider.holder() == player && glider.hand() == InteractionHand.OFF_HAND && !glider.isFlying(),
                "A grounded deployment did not equip a folded glider from the offhand");
        helper.assertTrue(OBHangGlider.isHeldGlider(player, off) && !OBHangGlider.isHeldGlider(player, main)
                && !OBHangGlider.isHeldGlider(player, off.copy()), "The model override did not identify the exact held stack");
        helper.assertTrue(!glider.save(new CompoundTag()), "An equipped glider was saved as a world entity");

        use(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(OBHangGlider.activeFor(player) == glider, "Using the other hand replaced the armed glider");
        use(player, InteractionHand.OFF_HAND);
        helper.assertTrue(glider.isRemoved() && OBHangGlider.activeFor(player) == null && main.getCount() == 2 && off.getCount() == 1,
                "Stowing the glider changed the inventory or left an entity behind");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hangGliderOnlyBindsAfterSpawnAndClearsOnStateChanges(final GameTestHelper helper) {
        final FakePlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.HANG_GLIDER.get()));
        final Consumer<EntityJoinLevelEvent> reject = event -> {
            if (event.getEntity() instanceof OBHangGlider) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, reject);
        try {
            use(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(OBHangGlider.activeFor(player) == null && player.getMainHandItem().getCount() == 1,
                    "A rejected spawn left a phantom or consumed the item");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(reject);
        }

        use(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(OBHangGlider.activeFor(player) != null, "A rejected spawn prevented the next valid equip");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        tick(player);
        helper.assertTrue(OBHangGlider.activeFor(player) == null, "Switching away from the glider left it equipped");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.HANG_GLIDER.get()));
        use(player, InteractionHand.MAIN_HAND);
        player.startFallFlying();
        tick(player);
        use(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(OBHangGlider.activeFor(player) == null, "The glider overlapped a vanilla Elytra flight");
        player.stopFallFlying();
        use(player, InteractionHand.MAIN_HAND);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, Level.OVERWORLD, Level.NETHER));
        helper.assertTrue(OBHangGlider.activeFor(player) == null, "Changing dimensions left a glider armed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hangGliderRespondsToDescentSneakingAndLanding(final GameTestHelper helper) {
        final boolean oldSetting = OBServerConfig.HANG_GLIDER_THERMALS.get();
        OBServerConfig.HANG_GLIDER_THERMALS.set(false);
        final FakePlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.HANG_GLIDER.get()));
        try {
            use(player, InteractionHand.MAIN_HAND);
            final OBHangGlider glider = OBHangGlider.activeFor(player);
            player.setOnGround(false);
            player.setDeltaMovement(0, 0.35, 0);
            tick(player);
            helper.assertTrue(glider.isFlying() && player.getDeltaMovement().y == 0.35,
                    "Equipping the wings canceled the upward part of a jump");

            player.setYRot(0);
            player.setDeltaMovement(0, -0.3, 0);
            player.fallDistance = 17;
            player.connection.aboveGroundTickCount = 80;
            tick(player);
            helper.assertTrue(close(player.getDeltaMovement().y, -0.052) && close(player.getDeltaMovement().z, 0.03)
                    && player.fallDistance == 0 && player.connection.aboveGroundTickCount == 0,
                    "Normal glide did not use the original descent or prevent fall damage and floating kicks");

            player.setShiftKeyDown(true);
            player.setYRot(90);
            player.setDeltaMovement(0.02, -0.15, 0.01);
            tick(player);
            helper.assertTrue(close(player.getDeltaMovement().x, -0.08) && close(player.getDeltaMovement().y, -0.176)
                    && close(player.getDeltaMovement().z, 0.01), "Sneaking did not dive faster in the player's direction");

            player.setOnGround(true);
            player.setDeltaMovement(0, -0.08, 0);
            player.connection.aboveGroundTickCount = 1;
            tick(player);
            helper.assertTrue(!glider.isFlying() && OBHangGlider.activeFor(player) == glider && player.getDeltaMovement().y == -0.08
                    && player.connection.aboveGroundTickCount == 1,
                    "Landing did not fold the glider or interfered with ground movement");
        } finally {
            OBServerConfig.HANG_GLIDER_THERMALS.set(oldSetting);
            final OBHangGlider glider = OBHangGlider.activeFor(player);
            if (glider != null) glider.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hangGliderThermalsDependOnAltitudeDaylightAndWeather(final GameTestHelper helper) {
        helper.assertTrue(close(OBHangGlider.thermalStrength(0.4, 110, 6_000, false, true), 0.4)
                && close(OBHangGlider.thermalStrength(0.4, 90, 500, false, true), 0.1)
                && OBHangGlider.thermalStrength(0.4, 70, 6_000, false, true) == 0
                && OBHangGlider.thermalStrength(0.4, 136, 6_000, false, true) == 0
                && OBHangGlider.thermalStrength(0.4, 110, 18_000, false, true) == 0,
                "Daytime thermals did not follow the legacy height and daylight curve");
        helper.assertTrue(OBHangGlider.thermalStrength(0.4, 110, 6_000, true, true) == -0.5
                && OBHangGlider.thermalStrength(0.4, 110, 6_000, true, false) == 0
                && OBHangGlider.thermalStrength(0.8, 175, 6_000, true, true) > 0.7,
                "Rain handling or the rare strong thermal changed");
        helper.assertTrue(close(OBHangGlider.verticalSpeed(false, 0.4), 0.108)
                && close(OBHangGlider.verticalSpeed(true, -0.5), -0.32), "Thermal lift or the dive's downward cap changed");
        helper.succeed();
    }

    private static boolean close(final double actual, final double expected) {
        return Math.abs(actual - expected) < 0.00001;
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "GliderGameTest"));
        final Vec3 position = helper.absoluteVec(new Vec3(3.5, 4, 3.5));
        player.moveTo(position.x, position.y, position.z, 0, 0);
        return player;
    }

    private static void use(final FakePlayer player, final InteractionHand hand) {
        player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand);
    }

    private static void tick(final FakePlayer player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
}
