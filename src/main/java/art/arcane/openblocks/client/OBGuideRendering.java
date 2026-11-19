package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBGuideBlock;
import art.arcane.openblocks.block.entity.OBGuideBlockEntity;
import art.arcane.openblocks.config.OBClientConfig;
import art.arcane.openblocks.guide.OBGuideControls;
import art.arcane.openblocks.guide.OBGuideOrientation;
import art.arcane.openblocks.registry.OBBlockEntities;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class OBGuideRendering {

    private static final ResourceLocation GUIDE = OpenBlocks.id("block/guide");
    private static final ResourceLocation BUILDER = OpenBlocks.id("block/builder_guide");
    private static final ResourceLocation CORE = OpenBlocks.id("block/guide_core");
    private static final ResourceLocation ENDER_CORE = OpenBlocks.id("block/builder_guide_core");
    private static final ResourceLocation MARKER = OpenBlocks.id("block/guide_marker");

    private OBGuideRendering() {}

    @Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void models(final ModelEvent.RegisterAdditional event) {
            for (final ResourceLocation model : List.of(GUIDE, BUILDER, CORE, ENDER_CORE, MARKER)) event.register(model);
        }

        @SubscribeEvent
        public static void renderers(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(OBBlockEntities.GUIDE.get(), context -> new Renderer());
            event.registerBlockEntityRenderer(OBBlockEntities.BUILDER_GUIDE.get(), context -> new Renderer());
        }
    }

    @Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT)
    public static final class Highlight {
        @SubscribeEvent
        public static void button(final RenderHighlightEvent.Block event) {
            final Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null) return;
            final var hit = event.getTarget();
            final BlockPos pos = hit.getBlockPos();
            final var state = client.level.getBlockState(pos);
            if (!(state.getBlock() instanceof OBGuideBlock guide)
                    || guide.builder() && client.player.getMainHandItem().getItem() instanceof BlockItem) return;
            final OBGuideOrientation orientation = state.getValue(OBGuideBlock.ORIENTATION);
            final OBGuideControls.Button button = OBGuideControls.find(orientation, hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
            if (button == null) return;

            final Vec3 camera = event.getCamera().getPosition();
            final AABB outline = orientation.toWorld(button.bounds()).move(pos).move(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()), outline, 0, 0, 0, 0.65F);
            event.setCanceled(true);
        }
    }

    private static final class Renderer implements BlockEntityRenderer<OBGuideBlockEntity> {

        private static final RenderType TRANSLUCENT = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
        private static final int VERTEX_SIZE = DefaultVertexFormat.BLOCK.getIntegerSize();
        private static final Direction[] FACES = Direction.values();
        private static final Map<OBGuideOrientation, Quaternionf> ROTATIONS = new java.util.EnumMap<>(OBGuideOrientation.class);
        private final Map<OBGuideBlockEntity, Preview> previews = new WeakHashMap<>();
        private BakedModel marker;
        private List<MarkerVertex> markerVertices = List.of();

        @Override public boolean shouldRenderOffScreen(final OBGuideBlockEntity entity) { return true; }

        @Override
        public int getViewDistance() {
            return (int) Math.ceil(Math.sqrt(OBClientConfig.GUIDE_RENDER_DISTANCE_SQ.get()));
        }

        @Override
        public void render(final OBGuideBlockEntity guide, final float partial, final PoseStack poses, final MultiBufferSource buffers,
                final int light, final int overlay) {
            final var level = guide.getLevel();
            if (level == null) return;
            final var client = Minecraft.getInstance();
            final Frustum frustum = client.levelRenderer.getFrustum();
            final BlockPos origin = guide.getBlockPos();
            if (frustum == null || frustum.isVisible(new AABB(origin))) renderHousing(guide, partial, poses, buffers, light, overlay);
            if (!guide.active()) {
                previews.remove(guide);
                return;
            }
            updateMarkerModel(client);
            final var preview = previews.computeIfAbsent(guide, ignored -> new Preview());
            final double now = level.getGameTime() + partial;
            preview.update(guide.points(), now);
            final float transition = Mth.clamp((float) ((now - preview.changedAt) / 10), 0, 1);
            final VertexConsumer vertices = buffers.getBuffer(TRANSLUCENT);
            final int rgb = guide.color();
            renderMarkers(preview.current, origin, frustum, transition, rgb, poses.last(), vertices, overlay);
            if (transition < 1) renderMarkers(preview.previous, origin, frustum, 1 - transition, rgb, poses.last(), vertices, overlay);
        }

        private void updateMarkerModel(final Minecraft client) {
            final BakedModel model = client.getModelManager().getModel(MARKER);
            if (model == marker) return;
            marker = model;
            final var vertices = new ArrayList<MarkerVertex>();
            final var random = RandomSource.create(0);
            addQuads(vertices, model.getQuads(null, null, random, ModelData.EMPTY, null));
            for (final Direction face : FACES) addQuads(vertices, model.getQuads(null, face, random, ModelData.EMPTY, null));
            markerVertices = List.copyOf(vertices);
        }

        private static void addQuads(final List<MarkerVertex> vertices, final List<BakedQuad> quads) {
            for (final BakedQuad quad : quads) {
                final int[] data = quad.getVertices();
                final Direction face = quad.getDirection();
                for (int j = 0; j < 4; j++) {
                    final int i = j * VERTEX_SIZE;
                    vertices.add(new MarkerVertex(Float.intBitsToFloat(data[i]) - 0.5F, Float.intBitsToFloat(data[i + 1]) - 0.5F,
                            Float.intBitsToFloat(data[i + 2]) - 0.5F, Float.intBitsToFloat(data[i + 4]), Float.intBitsToFloat(data[i + 5]), face));
                }
            }
        }

        private void renderMarkers(final List<Section> sections, final BlockPos origin, final Frustum frustum, final float scale, final int rgb,
                final PoseStack.Pose pose, final VertexConsumer vertices, final int overlay) {
            if (scale <= 0) return;
            final float red = (rgb >> 16 & 255) / 255F;
            final float green = (rgb >> 8 & 255) / 255F;
            final float blue = (rgb & 255) / 255F;
            final var position = new Vector4f();
            final var matrix = pose.pose();
            final var normals = new Vector3f[FACES.length];
            for (final Direction face : FACES) {
                normals[face.ordinal()] = pose.normal().transform(new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ()));
            }
            for (final Section section : sections) {
                if (frustum != null && !frustum.isVisible(section.bounds().move(origin))) continue;
                for (final BlockPos point : section.points()) {
                    final float x = point.getX() + 0.5F;
                    final float y = point.getY() + 0.5F;
                    final float z = point.getZ() + 0.5F;
                    for (final MarkerVertex vertex : markerVertices) {
                        matrix.transform(position.set(x + scale * vertex.x(), y + scale * vertex.y(), z + scale * vertex.z(), 1));
                        final Vector3f normal = normals[vertex.face().ordinal()];
                        // The packed quad format writes all vertex attributes together in BufferBuilder.
                        vertices.vertex(position.x(), position.y(), position.z(), red, green, blue, 1, vertex.u(), vertex.v(),
                                overlay, LightTexture.FULL_BRIGHT, normal.x(), normal.y(), normal.z());
                    }
                }
            }
        }

        private static void renderHousing(final OBGuideBlockEntity guide, final float partial, final PoseStack poses,
                final MultiBufferSource buffers, final int light, final int overlay) {
            final var client = Minecraft.getInstance();
            final var renderer = client.getBlockRenderer();
            final var state = guide.getBlockState();
            final boolean builder = guide.builder();
            poses.pushPose();
            poses.translate(0.5, 0.5, 0.5);
            poses.mulPose(ROTATIONS.computeIfAbsent(guide.orientation(), Renderer::rotation));
            poses.translate(-0.5, -0.5, -0.5);
            renderer.getModelRenderer().renderModel(poses.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), state,
                    client.getModelManager().getModel(builder ? ENDER_CORE : CORE), 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
            renderer.getModelRenderer().renderModel(poses.last(), buffers.getBuffer(TRANSLUCENT), state,
                    client.getModelManager().getModel(builder ? BUILDER : GUIDE), 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.translucent());
            if (builder) renderOrbit(guide, partial, poses, buffers, overlay);
            poses.popPose();
        }

        private static Quaternionf rotation(final OBGuideOrientation orientation) {
            final Direction x = orientation.x();
            final Direction y = orientation.y();
            final Direction z = orientation.z();
            return new Quaternionf().setFromNormalized(new Matrix3f(
                    x.getStepX(), x.getStepY(), x.getStepZ(),
                    y.getStepX(), y.getStepY(), y.getStepZ(),
                    z.getStepX(), z.getStepY(), z.getStepZ()));
        }

        private static void renderOrbit(final OBGuideBlockEntity guide, final float partial, final PoseStack poses,
                final MultiBufferSource buffers, final int overlay) {
            final var level = guide.getLevel();
            if (level == null) return;
            final var renderer = Minecraft.getInstance().getBlockRenderer();
            final float time = (level.getGameTime() % 6000) + partial;
            for (int i = 0; i < 3; i++) {
                poses.pushPose();
                poses.translate(0.5, 0.5, 0.5);
                poses.mulPose(Axis.YP.rotationDegrees(360 * Mth.sin((0.0413F + i * 0.009F) * time + i)));
                poses.mulPose(Axis.ZP.rotationDegrees(360 * Mth.sin((0.0765F + i * 0.013F) * time + 2 * i)));
                poses.translate(0.4, 0, 0);
                poses.scale(0.06F, 0.06F, 0.06F);
                poses.translate(-0.5, -0.5, -0.5);
                renderer.renderSingleBlock(Blocks.BLACK_CONCRETE.defaultBlockState(), poses, buffers, LightTexture.FULL_BRIGHT, overlay);
                poses.popPose();
            }
        }
    }

    private record MarkerVertex(float x, float y, float z, float u, float v, Direction face) {}

    private record Section(AABB bounds, List<BlockPos> points) {}

    private static final class Preview {
        private List<BlockPos> points;
        private List<Section> current = List.of();
        private List<Section> previous = List.of();
        private double changedAt;

        private void update(final List<BlockPos> next, final double now) {
            if (next == points) return;
            changedAt = points == null ? now - 10 : now;
            points = next;
            previous = current;
            final Map<Long, List<BlockPos>> sections = new HashMap<>();
            for (final BlockPos point : next) {
                final long key = SectionPos.asLong(point.getX() >> 4, point.getY() >> 4, point.getZ() >> 4);
                sections.computeIfAbsent(key, ignored -> new ArrayList<>()).add(point);
            }
            current = sections.entrySet().stream().map(entry -> {
                final int x = SectionPos.x(entry.getKey()) << 4;
                final int y = SectionPos.y(entry.getKey()) << 4;
                final int z = SectionPos.z(entry.getKey()) << 4;
                return new Section(new AABB(x, y, z, x + 16, y + 16, z + 16), entry.getValue());
            }).toList();
        }
    }
}
