package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBClientConfig;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT)
public final class OBSonicGlassesOverlay {

    private static final ResourceLocation OBSIDIAN = OpenBlocks.id("textures/misc/glasses_obsidian.png");
    private static final ResourceLocation FALLBACK_SPRITE = OpenBlocks.id("item/sound_generic");
    private static final int MAX_MARKERS = 256;
    private static final Map<ResourceLocation, OBSoundIcons.Icon> ICONS = new HashMap<>();
    private static final ArrayDeque<SoundInstance> PENDING = new ArrayDeque<>();
    private static final ArrayDeque<Marker> MARKERS = new ArrayDeque<>();
    private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
    private static ClientLevel level;
    private static Entity wearer;
    private static Vec3 cameraPosition;
    private static float verticalProjection;

    private OBSonicGlassesOverlay() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSound(final PlaySoundEvent event) {
        final SoundInstance sound = event.getSound();
        if (sound == null) return;
        final Minecraft client = Minecraft.getInstance();
        final ClientLevel sourceLevel = client.level;
        client.execute(() -> {
            if (client.level != sourceLevel || !updateWearer(client)) return;
            if (PENDING.size() >= MAX_MARKERS) PENDING.removeFirst();
            // Vanilla resolves the sample after this event. Read it on the next client tick.
            PENDING.addLast(sound);
        });
    }

    @SubscribeEvent
    public static void tick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        final Minecraft client = Minecraft.getInstance();
        if (!updateWearer(client)) return;
        final SoundManager soundManager = client.getSoundManager();
        MARKERS.removeIf(marker -> marker.advance(soundManager));
        while (!PENDING.isEmpty()) {
            final SoundInstance sound = PENDING.removeFirst();
            final var sample = sound.getSound();
            if (sample == null || sample == SoundManager.EMPTY_SOUND || sample == SoundManager.INTENTIONALLY_EMPTY_SOUND) continue;
            final float diameter = (float) (2 * Math.log1p(sound.getVolume()));
            if (!Float.isFinite(diameter) || diameter <= 0) continue;
            final int duration = Mth.clamp(Mth.floor(20 * sound.getPitch()), 1, 200);
            if (MARKERS.size() >= MAX_MARKERS) MARKERS.removeFirst();
            MARKERS.addLast(new Marker(sound, ICONS.computeIfAbsent(sound.getLocation(), OBSoundIcons::forSound), diameter, duration));
        }
    }

    @SubscribeEvent
    public static void captureView(final RenderLevelStageEvent event) {
        // This stage supplies the actual world view pose on all graphics modes.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || !updateWearer(Minecraft.getInstance())) return;
        cameraPosition = event.getCamera().getPosition();
        verticalProjection = Math.abs(event.getProjectionMatrix().m11());
        VIEW_PROJECTION.set(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());
    }

    private static boolean updateWearer(final Minecraft client) {
        final Entity camera = client.getCameraEntity();
        final Entity next = client.level != null && camera instanceof Player player
                && player.getItemBySlot(EquipmentSlot.HEAD).is(OBItems.SONIC_GLASSES.get()) ? camera : null;
        if (level != client.level || wearer != next) {
            level = client.level;
            wearer = next;
            PENDING.clear();
            MARKERS.clear();
            cameraPosition = null;
        }
        return next != null;
    }

    static void render(final ForgeGui gui, final GuiGraphics graphics, final float partial, final int width, final int height) {
        final Minecraft client = Minecraft.getInstance();
        if (!client.options.getCameraType().isFirstPerson() || !updateWearer(client)) return;
        gui.setupOverlayRenderState(true, false);
        try {
            dimWorld(graphics, width, height);
            if (cameraPosition == null || MARKERS.isEmpty()) return;
            RenderSystem.disableDepthTest();
            final var atlas = client.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            final var clip = new Vector4f();
            for (final Marker marker : MARKERS) {
                final SoundInstance sound = marker.sound;
                final int x;
                final int y;
                final int diameter;
                if (sound.isRelative()) {
                    x = width / 2;
                    y = height * 3 / 4;
                    diameter = Mth.clamp(Math.round(marker.diameter * 12), 6, 48);
                } else {
                    clip.set((float) (sound.getX() - cameraPosition.x), (float) (sound.getY() - cameraPosition.y),
                            (float) (sound.getZ() - cameraPosition.z), 1).mul(VIEW_PROJECTION);
                    if (clip.w <= 0.05F || Math.abs(clip.x) > clip.w || Math.abs(clip.y) > clip.w) continue;
                    x = Math.round((0.5F + 0.5F * clip.x / clip.w) * width);
                    y = Math.round((0.5F - 0.5F * clip.y / clip.w) * height);
                    diameter = Mth.clamp(Math.round(marker.diameter * 0.5F * height * verticalProjection / clip.w), 4, 96);
                }
                for (final var layer : marker.icon.layers()) {
                    final int size = Math.max(1, Math.round(diameter * layer.scale()));
                    var sprite = atlas.apply(layer.sprite());
                    if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) sprite = atlas.apply(FALLBACK_SPRITE);
                    final int rgb = layer.color();
                    graphics.blit(x - size / 2, y - size / 2, 0, size, size, sprite, ((rgb >> 16) & 255) / 255F,
                            ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, marker.alpha(partial));
                }
            }
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    private static void dimWorld(final GuiGraphics graphics, final int width, final int height) {
        final float opacity = OBClientConfig.SONIC_GLASSES_OPACITY.get().floatValue();
        if (opacity <= 0) return;
        if (OBClientConfig.SONIC_GLASSES_USE_TEXTURE.get()) {
            graphics.setColor(1, 1, 1, opacity);
            graphics.blit(OBSIDIAN, 0, 0, width, height, 0, 0, 256, 256, 256, 256);
            graphics.setColor(1, 1, 1, 1);
        } else {
            graphics.fill(0, 0, width, height, (Math.round(opacity * 255) << 24) | 0x161321);
        }
    }

    private static final class Marker {
        private final SoundInstance sound;
        private final OBSoundIcons.Icon icon;
        private final float diameter;
        private final int duration;
        private int remaining;
        private boolean fading;

        private Marker(final SoundInstance sound, final OBSoundIcons.Icon icon, final float diameter, final int duration) {
            this.sound = sound;
            this.icon = icon;
            this.diameter = diameter;
            this.duration = duration;
            remaining = duration;
        }

        private boolean advance(final SoundManager manager) {
            fading = !manager.isActive(sound);
            return fading && --remaining <= 0;
        }

        private float alpha(final float partial) {
            return fading ? Mth.clamp((remaining - partial) / duration, 0, 1) : 1;
        }
    }
}
