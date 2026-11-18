package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.entity.OBHangGlider;
import art.arcane.openblocks.registry.OBSounds;
import com.mojang.blaze3d.platform.InputConstants;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT)
public final class OBVariometer {

    private static final String CATEGORY = "key.categories.open_blocks";
    private static final KeyMapping TOGGLE = new KeyMapping("key.open_blocks.vario_switch", GLFW.GLFW_KEY_V, CATEGORY);
    private static final KeyMapping VOLUME_UP = new KeyMapping("key.open_blocks.vario_vol_up", InputConstants.UNKNOWN.getValue(), CATEGORY);
    private static final KeyMapping VOLUME_DOWN = new KeyMapping("key.open_blocks.vario_vol_down", InputConstants.UNKNOWN.getValue(), CATEGORY);
    private static final int SAMPLES_PER_UPDATE = 4;
    private static final int MAX_VOLUME = 20;
    private static final float LOW_SAMPLE_PITCH = 600;
    private static final float HIGH_SAMPLE_PITCH = 1200;

    private static boolean enabled;
    private static int volume = 8;
    private static int samples;
    private static double liftSum;
    private static float pitchHz;
    private static float targetHz;
    private static double pulsesPerSecond;
    @Nullable private static Tone tone;

    private OBVariometer() {}

    @Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Bindings {

        @SubscribeEvent
        public static void register(final RegisterKeyMappingsEvent event) {
            event.register(TOGGLE);
            event.register(VOLUME_UP);
            event.register(VOLUME_DOWN);
        }
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        final Minecraft client = Minecraft.getInstance();
        final var player = client.player;
        final SoundManager sounds = client.getSoundManager();
        if (player == null || !OBServerConfig.HANG_GLIDER_THERMALS.get()) {
            clearSignal(sounds);
            return;
        }

        if (TOGGLE.consumeClick()) {
            enabled = !enabled;
            player.displayClientMessage(Component.translatable(enabled ? "open_blocks.variometer.on" : "open_blocks.variometer.off"), true);
        }
        final int oldVolume = volume;
        if (VOLUME_UP.consumeClick()) volume = Math.min(volume + 2, MAX_VOLUME);
        if (VOLUME_DOWN.consumeClick()) volume = Math.max(volume - 2, 2);
        if (oldVolume != volume) player.displayClientMessage(Component.translatable("open_blocks.variometer.volume", 100 * volume / MAX_VOLUME), true);

        final OBHangGlider glider = OBHangGlider.activeFor(player);
        if (!enabled || client.options.getSoundSourceVolume(SoundSource.MASTER) == 0
                || client.options.getSoundSourceVolume(SoundSource.PLAYERS) == 0
                || glider == null || !glider.isFlying() || Double.isNaN(glider.glideRate())) {
            clearSignal(sounds);
            return;
        }

        liftSum += glider.glideRate();
        if (++samples == SAMPLES_PER_UPDATE) {
            final double lift = Mth.clamp(liftSum / samples, OBHangGlider.FASTEST_DESCENT, OBHangGlider.MAX_THERMAL_LIFT);
            samples = 0;
            liftSum = 0;
            if (lift <= 0) {
                targetHz = (float) (LOW_SAMPLE_PITCH + 300 * lift / -OBHangGlider.FASTEST_DESCENT);
                pulsesPerSecond = 0;
            } else {
                final double proportion = lift / OBHangGlider.MAX_THERMAL_LIFT;
                targetHz = (float) (LOW_SAMPLE_PITCH + 1400 * proportion);
                pulsesPerSecond = 10 * proportion;
            }
        }
        if (targetHz == 0) return;
        pitchHz = pitchHz == 0 ? targetHz : Mth.approach(pitchHz, targetHz, 20);
        final boolean high = pitchHz > HIGH_SAMPLE_PITCH;
        if (tone == null || tone.high != high || !sounds.isActive(tone)) {
            if (tone != null) sounds.stop(tone);
            tone = new Tone(high);
            sounds.play(tone);
        }
    }

    private static void clearSignal(final SoundManager sounds) {
        if (tone != null) sounds.stop(tone);
        tone = null;
        targetHz = pitchHz = 0;
        pulsesPerSecond = liftSum = 0;
        samples = 0;
    }

    private static final class Tone extends AbstractTickableSoundInstance {

        private final boolean high;
        private double pulse;

        private Tone(final boolean high) {
            super((high ? OBSounds.VARIOMETER_HIGH : OBSounds.VARIOMETER_LOW).get(), SoundSource.PLAYERS, RandomSource.create());
            this.high = high;
            looping = true;
            relative = true;
            attenuation = Attenuation.NONE;
            tick();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            pitch = pitchHz / (high ? HIGH_SAMPLE_PITCH : LOW_SAMPLE_PITCH);
            volume = pulse < 0.5 ? OBVariometer.volume / 128F : 0;
            pulse = pulsesPerSecond == 0 ? 0 : (pulse + pulsesPerSecond / 20) % 1;
        }
    }
}
