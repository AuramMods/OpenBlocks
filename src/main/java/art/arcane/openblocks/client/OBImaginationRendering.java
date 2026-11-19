package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBImaginaryBlock;
import art.arcane.openblocks.block.OBImaginaryBlock.Shape;
import art.arcane.openblocks.block.entity.OBImaginaryBlockEntity;
import art.arcane.openblocks.config.OBClientConfig;
import art.arcane.openblocks.item.OBImaginaryItem;
import art.arcane.openblocks.item.OBImaginationGlasses;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBImaginationRendering {

    private static final Map<Shape, ResourceLocation> PENCIL_MODELS = models("pencil");
    private static final Map<Shape, ResourceLocation> CRAYON_MODELS = models("crayon");
    private static final ResourceLocation CRAYON_LENSES = OpenBlocks.id("textures/model/glasses_crayon_overlay.png");

    private OBImaginationRendering() {}

    private static Map<Shape, ResourceLocation> models(final String medium) {
        return Map.of(
                Shape.BLOCK, OpenBlocks.id("block/imaginary_" + medium + "_block"),
                Shape.PANEL, OpenBlocks.id("block/imaginary_" + medium + "_panel"),
                Shape.HALF_PANEL, OpenBlocks.id("block/imaginary_" + medium + "_half"),
                Shape.STAIRS, OpenBlocks.id("block/imaginary_" + medium + "_stairs"));
    }

    @SubscribeEvent
    public static void registerModels(final ModelEvent.RegisterAdditional event) {
        PENCIL_MODELS.values().forEach(event::register);
        CRAYON_MODELS.values().forEach(event::register);
    }

    @SubscribeEvent
    public static void registerProperties(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(OBItems.IMAGINARY.get(), OpenBlocks.id("crayon"),
                    (stack, level, holder, seed) -> OBImaginaryItem.color(stack) == null ? 0 : 1);
            ItemProperties.register(OBItems.IMAGINARY.get(), OpenBlocks.id("mode"),
                    (stack, level, holder, seed) -> OBImaginaryItem.mode(stack).ordinal());
        });
    }

    @SubscribeEvent
    public static void registerColors(final RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            final var rgb = OBImaginaryItem.color(stack);
            return layer == 1 && rgb != null ? rgb : -1;
        }, OBItems.IMAGINARY.get());
        event.register((stack, layer) -> OBImaginationGlasses.color(stack), OBItems.CRAYON_GLASSES.get());
    }

    @SubscribeEvent
    public static void registerRenderer(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.IMAGINARY.get(), context -> new DrawingRenderer());
    }

    @SubscribeEvent
    public static void registerArmorLayers(final EntityRenderersEvent.AddLayers event) {
        for (final String skin : event.getSkins()) addLensLayer(event.getPlayerSkin(skin), event);
        for (final var type : ForgeRegistries.ENTITY_TYPES) addLensLayer(event.getEntityRenderer(type), event);
    }

    // The event exposes unrelated renderer model types. Only vanilla-style humanoid armor has these UVs.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLensLayer(final EntityRenderer<?> entityRenderer, final EntityRenderersEvent.AddLayers event) {
        if (entityRenderer instanceof LivingEntityRenderer<?, ?> renderer && renderer.getModel() instanceof HumanoidModel<?>) {
            final var helmet = new HumanoidModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
            ((LivingEntityRenderer) renderer).addLayer(new LensLayer(renderer, helmet));
        }
    }

    private static final class LensLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
        private final HumanoidModel<T> helmet;

        private LensLayer(final LivingEntityRenderer<T, M> renderer, final HumanoidModel<T> helmet) {
            super(renderer);
            this.helmet = helmet;
            helmet.setAllVisible(false);
            helmet.head.visible = true;
        }

        @Override
        public void render(final PoseStack pose, final MultiBufferSource buffers, final int light, final T wearer,
                final float swing, final float amount, final float partial, final float age, final float yaw, final float pitch) {
            final ItemStack glasses = wearer.getItemBySlot(EquipmentSlot.HEAD);
            if (!glasses.is(OBItems.CRAYON_GLASSES.get())) return;

            getParentModel().copyPropertiesTo(helmet);
            final int rgb = OBImaginationGlasses.color(glasses);
            helmet.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CRAYON_LENSES)), light,
                    OverlayTexture.NO_OVERLAY, red(rgb), green(rgb), blue(rgb), 1.0F);
        }
    }

    private static float red(final int rgb) { return (rgb >> 16 & 255) / 255.0F; }
    private static float green(final int rgb) { return (rgb >> 8 & 255) / 255.0F; }
    private static float blue(final int rgb) { return (rgb & 255) / 255.0F; }

    private static final class DrawingRenderer implements BlockEntityRenderer<OBImaginaryBlockEntity> {
        private static final float[] BRIGHTNESS = {1, 1, 1, 1};
        private static final int FULL = LightTexture.FULL_BRIGHT;
        private static final int[] LIGHT = {FULL, FULL, FULL, FULL};
        private final EnumMap<Shape, List<BakedQuad>> pencilQuads = new EnumMap<>(Shape.class);
        private final EnumMap<Shape, List<BakedQuad>> crayonQuads = new EnumMap<>(Shape.class);
        private final Map<OBImaginaryBlockEntity, Fade> fades = new WeakHashMap<>();

        @Override
        public void render(final OBImaginaryBlockEntity block, final float partial, final PoseStack pose, final MultiBufferSource buffers,
                final int light, final int overlay) {
            final var client = Minecraft.getInstance();
            final var player = client.player;
            if (player == null) return;
            final boolean visible = block.visibleTo(player);
            final var fade = visible ? fades.computeIfAbsent(block, key -> new Fade()) : fades.get(block);
            if (fade == null) return;
            final float alpha = fade.approach(visible);
            if (alpha <= 0.0F) {
                if (!visible) fades.remove(block);
                return;
            }

            final Integer color = block.color();
            final boolean pencil = color == null;
            final var quads = (pencil ? pencilQuads : crayonQuads).computeIfAbsent(block.shape(), shape -> {
                final var model = client.getModelManager().getModel((pencil ? PENCIL_MODELS : CRAYON_MODELS).get(shape));
                final var random = RandomSource.create(0);
                final List<BakedQuad> faces = new ArrayList<>(model.getQuads(null, null, random));
                for (final Direction face : Direction.values()) faces.addAll(model.getQuads(null, face, random));
                return faces;
            });
            pose.pushPose();
            if (block.shape() == Shape.STAIRS) {
                final int turns = (block.getBlockState().getValue(OBImaginaryBlock.FACING).get2DDataValue() + 2) % 4;
                pose.translate(0.5, 0, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(-90.0F * turns));
                pose.translate(-0.5, 0, -0.5);
            }

            final int rgb = pencil ? 0xFFFFFF : color;
            final var output = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            for (final var quad : quads) {
                output.putBulkData(pose.last(), quad, BRIGHTNESS, red(rgb), green(rgb), blue(rgb), alpha, LIGHT, overlay, false);
            }
            pose.popPose();
        }
    }

    private static final class Fade {
        private float alpha;
        private long updated = Util.getMillis();

        private float approach(final boolean visible) {
            final long now = Util.getMillis();
            final float speed = OBClientConfig.IMAGINARY_FADE.get().floatValue() * 0.06F;
            final float change = (now - updated) * speed;
            updated = now;
            alpha = Mth.clamp(alpha + (visible ? change : -change), 0.0F, 1.0F);
            return alpha;
        }
    }
}
