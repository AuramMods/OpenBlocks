package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBClientConfig;
import art.arcane.openblocks.network.OBNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT)
public final class OBBooBinding {

    private static final KeyMapping BOO = new KeyMapping("key.open_blocks.drop_brick", GLFW.GLFW_KEY_B, "key.categories.open_blocks");
    private static boolean keyHeld;

    private OBBooBinding() {}

    @Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {

        private Registration() {}

        @SubscribeEvent
        public static void register(final RegisterKeyMappingsEvent event) {
            event.register(BOO);
        }
    }

    @SubscribeEvent
    public static void onKey(final InputEvent.Key event) {
        final var key = InputConstants.getKey(event.getKey(), event.getScanCode());
        if (event.getAction() == GLFW.GLFW_RELEASE) release(key);
        else if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) press(key);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenKeyPressed(final ScreenEvent.KeyPressed.Pre event) {
        press(InputConstants.getKey(event.getKeyCode(), event.getScanCode()));
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onScreenKeyReleased(final ScreenEvent.KeyReleased.Pre event) {
        release(InputConstants.getKey(event.getKeyCode(), event.getScanCode()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMouseButton(final InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS && BOO.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
            requestBoo();
        }
    }

    private static void press(final InputConstants.Key key) {
        if (keyHeld || !BOO.isActiveAndMatches(key)) return;
        keyHeld = true;
        requestBoo();
    }

    private static void release(final InputConstants.Key key) {
        if (BOO.getKey().equals(key)) keyHeld = false;
    }

    private static void requestBoo() {
        final var client = Minecraft.getInstance();
        if (client.player != null && client.player.isAlive() && !client.player.isSpectator() && client.getOverlay() == null
                && !OBClientConfig.SERIOUS_MODE.get() && (client.screen == null || OBClientConfig.BOO_WHILE_TYPING.get())) {
            OBNetwork.requestBoo();
        }
    }
}
