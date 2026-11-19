package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBProjectorBlock;
import art.arcane.openblocks.block.entity.OBProjectorBlockEntity;
import art.arcane.openblocks.config.OBClientConfig;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBMenuTypes;
import art.arcane.openblocks.world.OBHeightMapData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBProjectorRendering {

    private static final ResourceLocation INNER = OpenBlocks.id("block/projector_spinner_inner");
    private static final ResourceLocation OUTER = OpenBlocks.id("block/projector_spinner_outer");
    private static final ResourceLocation CONE = OpenBlocks.id("block/projector_cone");
    private static final Map<OBHeightMapData, ProjectionMesh> MESHES = new WeakHashMap<>();

    private OBProjectorRendering() {}

    @SubscribeEvent
    public static void registerModels(final ModelEvent.RegisterAdditional event) {
        event.register(INNER);
        event.register(OUTER);
        event.register(CONE);
    }

    @SubscribeEvent
    public static void registerRenderer(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.PROJECTOR.get(), context -> OBProjectorRendering::render);
    }

    @SubscribeEvent
    public static void registerScreen(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(OBMenuTypes.PROJECTOR.get(), OBProjectorScreen::new));
    }

    public static void receive(final ResourceLocation dimension, final BlockPos pos, final int map, @Nullable final CompoundTag data) {
        final var level = Minecraft.getInstance().level;
        if (data != null && level != null && level.dimension().location().equals(dimension)
                && level.getBlockEntity(pos) instanceof OBProjectorBlockEntity projector) projector.acceptProjection(map, data);
    }

    private static void render(final OBProjectorBlockEntity projector, final float partial, final PoseStack poses,
            final MultiBufferSource buffers, final int light, final int overlay) {
        final BlockState state = projector.getBlockState();
        final var level = projector.getLevel();
        if (level == null || !state.hasProperty(OBProjectorBlock.ACTIVE)) return;
        final boolean active = state.getValue(OBProjectorBlock.ACTIVE);
        final float angle = active ? ((level.getGameTime() % 160) + partial) * 4.5F : 0;
        final var cutout = buffers.getBuffer(Sheets.cutoutBlockSheet());
        spinner(state, INNER, angle, poses, cutout, light, overlay);
        spinner(state, OUTER, angle * 0.5F, poses, cutout, light, overlay);
        if (!active) return;
        if (OBClientConfig.PROJECTOR_CONE.get()) {
            final int brightness = OBClientConfig.PROJECTOR_BRIGHTNESS.get();
            final int glow = (brightness * 15 + 127) / 255;
            drawModel(state, CONE, poses, buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),
                    brightness < 0 ? light : LightTexture.pack(glow, glow), overlay, RenderType.translucent());
        }
        final OBHeightMapData data = projector.map();
        if (data == null) return;
        poses.pushPose();
        poses.translate(0.5, 1, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees(-90 * projector.rotation()));
        poses.translate(-0.5, 0, -0.5);
        mesh(data).render(poses, buffers.getBuffer(RenderType.debugQuads()));
        poses.popPose();
    }

    private static void spinner(final BlockState state, final ResourceLocation id, final float angle, final PoseStack poses,
            final VertexConsumer vertices, final int light, final int overlay) {
        poses.pushPose();
        poses.translate(0.5, 0, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees(angle));
        poses.translate(-0.5, 0, -0.5);
        drawModel(state, id, poses, vertices, light, overlay, RenderType.cutout());
        poses.popPose();
    }

    private static void drawModel(final BlockState state, final ResourceLocation id, final PoseStack poses, final VertexConsumer vertices,
            final int light, final int overlay, final RenderType type) {
        final var client = Minecraft.getInstance();
        client.getBlockRenderer().getModelRenderer().renderModel(poses.last(), vertices, state, client.getModelManager().getModel(id),
                1, 1, 1, light, overlay, ModelData.EMPTY, type);
    }

    static ProjectionMesh mesh(final OBHeightMapData data) {
        return MESHES.computeIfAbsent(data, ProjectionMesh::new);
    }

    static void rectangle(final VertexConsumer vertices, final Matrix4f pose, final float x0, final float z0,
            final float x1, final float z1, final float y, final int color) {
        vertex(vertices, pose, x0, y, z0, color);
        vertex(vertices, pose, x0, y, z1, color);
        vertex(vertices, pose, x1, y, z1, color);
        vertex(vertices, pose, x1, y, z0, color);
    }

    private static void vertex(final VertexConsumer vertices, final Matrix4f pose, final float x, final float y, final float z, final int color) {
        vertices.vertex(pose, x, y, z).color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24).endVertex();
    }

    static final class ProjectionMesh {
        private static final float CELL = 1F / OBHeightMapData.SIZE;
        private final List<Strip> strips = new ArrayList<>();
        private float bottom = Float.POSITIVE_INFINITY;
        private float top = Float.NEGATIVE_INFINITY;

        private record Strip(float x0, float x1, float z, float y, int color) {}

        private ProjectionMesh(final OBHeightMapData data) {
            for (int layer = 0; layer < data.layerCount(); layer++) {
                final int alpha = data.layerAlpha(layer);
                if (alpha == 0) continue;
                for (int z = 0; z < OBHeightMapData.SIZE; z++) {
                    int x = 0;
                    while (x < OBHeightMapData.SIZE) {
                        final int color = data.layerColor(layer, x, z);
                        if (color == 0 || color >= 64) {
                            x++;
                            continue;
                        }
                        final int height = data.layerHeight(layer, x, z);
                        int end = x + 1;
                        while (end < OBHeightMapData.SIZE && data.layerColor(layer, end, z) == color
                                && data.layerHeight(layer, end, z) == height) end++;
                        final float y = Mth.clamp(height - (long) data.minHeight(), 0, 4064) * CELL + layer * 0.0001F;
                        strips.add(new Strip(x * CELL, end * CELL, z * CELL, y, alpha << 24 | MapColor.byId(color).col));
                        bottom = Math.min(bottom, y);
                        top = Math.max(top, y);
                        x = end;
                    }
                }
            }
        }

        boolean isEmpty() { return strips.isEmpty(); }
        float midpoint() { return isEmpty() ? 2 : (bottom + top) / 2; }
        float height() { return isEmpty() ? 0 : top - bottom; }

        void render(final PoseStack poses, final VertexConsumer vertices) {
            final Matrix4f matrix = poses.last().pose();
            for (final Strip strip : strips) {
                rectangle(vertices, matrix, strip.x0, strip.z, strip.x1, strip.z + CELL, strip.y, strip.color);
            }
        }
    }
}
