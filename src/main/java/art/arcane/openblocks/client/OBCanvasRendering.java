package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity.Face;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity.Mask;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity.Layer;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity.Surface;
import art.arcane.openblocks.registry.OBBlocks;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBCanvasRendering {

    private static final int RGB_TINT = 0x1000000;
    private static final int VERTEX_SIZE = DefaultVertexFormat.BLOCK.getIntegerSize();
    private static final float PAINT_STEP = 0.00035F;
    private static final float COVER_OFFSET = 0.024F;
    private static final ModelProperty<Geometry> GEOMETRY = new ModelProperty<>();

    private record Geometry(ModelData modelData, long seed, AABB bounds, int hiddenFaces) {}

    private OBCanvasRendering() {}

    @SubscribeEvent
    public static void bake(final ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id, model) -> {
            if (id instanceof ModelResourceLocation variant && !variant.getVariant().equals("inventory")
                    && id.getNamespace().equals(OpenBlocks.MODID) && (id.getPath().equals("canvas") || id.getPath().equals("canvas_glass"))) {
                return new CanvasModel(model);
            }
            return model;
        });
    }

    @SubscribeEvent
    public static void tint(final RegisterColorHandlersEvent.Block event) {
        final var colors = event.getBlockColors();
        event.register((state, level, pos, tint) -> {
            if (tint >= RGB_TINT) return tint & 0xFFFFFF;
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas && !canvas.original().isAir()) {
                return colors.getColor(canvas.original(), level, pos, tint);
            }
            return -1;
        }, OBBlocks.CANVAS.get(), OBBlocks.CANVAS_GLASS.get());
    }

    private static final class CanvasModel extends BakedModelWrapper<BakedModel> {

        private CanvasModel(final BakedModel base) { super(base); }

        private static BakedModel modelFor(final BlockState state) {
            return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        }

        @Override
        public ModelData getModelData(final BlockAndTintGetter level, final BlockPos pos, final BlockState state, final ModelData data) {
            final Surface surface = data.get(OBCanvasBlockEntity.SURFACE);
            if (surface == null) return data;
            final BlockState original = surface.original();
            final BlockState geometryState = original.isAir() ? state : original;
            final ModelData originalData = original.isAir() ? ModelData.EMPTY
                    : modelFor(original).getModelData(level, pos, original, ModelData.EMPTY);
            final var shape = geometryState.getShape(level, pos);
            final AABB bounds = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
            // Forge can hide an adjacent block's face. Preserve the original state's culling on the canvas side too.
            int hiddenFaces = 0;
            if (!original.isAir()) {
                for (final Direction side : Direction.values()) {
                    if (original.skipRendering(level.getBlockState(pos.relative(side)), side)) {
                        hiddenFaces |= 1 << side.get3DDataValue();
                    }
                }
            }
            return data.derive().with(GEOMETRY, new Geometry(originalData, geometryState.getSeed(pos), bounds, hiddenFaces)).build();
        }

        private static ModelData baseData(final ModelData data) {
            final Geometry geometry = data.get(GEOMETRY);
            return geometry == null ? ModelData.EMPTY : geometry.modelData();
        }

        private ChunkRenderTypeSet originalLayers(final BlockState state, final RandomSource random, final ModelData data, final Surface surface) {
            return surface.original().isAir() ? super.getRenderTypes(state, random, data)
                    : modelFor(surface.original()).getRenderTypes(surface.original(), random, baseData(data));
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(final BlockState state, final RandomSource random, final ModelData data) {
            final Surface surface = data.get(OBCanvasBlockEntity.SURFACE);
            if (surface == null) return super.getRenderTypes(state, random, data);
            final ChunkRenderTypeSet layers = originalLayers(state, random, data, surface);
            return surface.faces().stream().anyMatch(Face::decorated)
                    ? ChunkRenderTypeSet.union(layers, ChunkRenderTypeSet.of(RenderType.cutout())) : layers;
        }

        @Override
        public TextureAtlasSprite getParticleIcon(final ModelData data) {
            final Surface surface = data.get(OBCanvasBlockEntity.SURFACE);
            if (surface == null || surface.original().isAir()) return super.getParticleIcon(data);
            return modelFor(surface.original()).getParticleIcon(baseData(data));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side, final RandomSource random,
                final ModelData data, @Nullable final RenderType layer) {
            final Surface surface = data.get(OBCanvasBlockEntity.SURFACE);
            if (surface == null || state == null) return super.getQuads(state, side, random, data, layer);
            final Geometry geometry = data.get(GEOMETRY);
            if (side != null && geometry != null && (geometry.hiddenFaces() & (1 << side.get3DDataValue())) != 0) return List.of();
            final long seed = geometry == null ? 0 : geometry.seed();
            final ChunkRenderTypeSet layers = originalLayers(state, RandomSource.create(seed), data, surface);
            final List<BakedQuad> result = new ArrayList<>();
            if (layer == null || layers.contains(layer)) {
                for (final BakedQuad quad : originalQuads(state, side, random, data, layer, surface)) {
                    final int color = surface.face(quad.getDirection()).background();
                    result.add(color == OBCanvasBlockEntity.UNPAINTED ? quad : solidPaint(quad, color));
                }
            }
            if (layer != null && layer != RenderType.cutout()) return result;
            if (surface.faces().stream().noneMatch(Face::decorated)) return result;

            // Draw the masks against the original geometry, even if its own layer is translucent.
            for (final RenderType baseLayer : layers) {
                for (final BakedQuad quad : originalQuads(state, side, RandomSource.create(seed), data, baseLayer, surface)) {
                    int order = 1;
                    for (final Layer stamp : surface.face(quad.getDirection()).layers()) {
                        result.add(stamp(quad, stamp, order++ * PAINT_STEP));
                    }
                }
            }
            if (side == null && geometry != null) {
                for (final Direction face : Direction.values()) {
                    final Mask cover = surface.face(face).cover();
                    if (cover != null) result.add(coverQuad(face, geometry.bounds(), cover));
                }
            }
            return result;
        }

        private List<BakedQuad> originalQuads(final BlockState state, @Nullable final Direction side, final RandomSource random,
                final ModelData data, @Nullable final RenderType layer, final Surface surface) {
            final BlockState original = surface.original();
            return original.isAir() ? super.getQuads(state, side, random, data, layer)
                    : modelFor(original).getQuads(original, side, random, baseData(data), layer);
        }
    }

    private static TextureAtlasSprite sprite(final String path) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(OpenBlocks.id(path));
    }

    private static BakedQuad solidPaint(final BakedQuad source, final int rgb) {
        final TextureAtlasSprite white = sprite("block/canvas");
        final int[] vertices = source.getVertices().clone();
        for (int i = 0; i < 4; i++) setUv(vertices, i * VERTEX_SIZE, white, 0.5F, 0.5F, 0);
        return new BakedQuad(vertices, RGB_TINT | rgb, source.getDirection(), white, source.isShade(), source.hasAmbientOcclusion());
    }

    private static BakedQuad stamp(final BakedQuad source, final Layer layer, final float offset) {
        final Mask mask = layer.mask();
        final TextureAtlasSprite texture = sprite("block/stencil/" + mask.pattern().getSerializedName());
        final Direction face = source.getDirection();
        final int[] vertices = source.getVertices().clone();
        for (int i = 0; i < 4; i++) {
            final int index = i * VERTEX_SIZE;
            final float x = Float.intBitsToFloat(vertices[index]);
            final float y = Float.intBitsToFloat(vertices[index + 1]);
            final float z = Float.intBitsToFloat(vertices[index + 2]);
            final float u = switch (face) {
                case NORTH -> 1 - x;
                case EAST -> 1 - z;
                case WEST -> z;
                default -> x;
            };
            final float v = switch (face) {
                case UP -> z;
                case DOWN -> 1 - z;
                default -> 1 - y;
            };
            setPosition(vertices, index, x + face.getStepX() * offset, y + face.getStepY() * offset, z + face.getStepZ() * offset);
            setUv(vertices, index, texture, u, v, mask.rotation());
        }
        return new BakedQuad(vertices, RGB_TINT | layer.color(), face, texture, source.isShade(), source.hasAmbientOcclusion());
    }

    private static BakedQuad coverQuad(final Direction face, final AABB bounds, final Mask mask) {
        final TextureAtlasSprite texture = sprite("item/stencil/" + mask.pattern().getSerializedName());
        final int[] vertices = new int[4 * VERTEX_SIZE];
        final float plane = switch (face) {
            case NORTH -> (float) bounds.minZ - COVER_OFFSET;
            case SOUTH -> (float) bounds.maxZ + COVER_OFFSET;
            case WEST -> (float) bounds.minX - COVER_OFFSET;
            case EAST -> (float) bounds.maxX + COVER_OFFSET;
            case DOWN -> (float) bounds.minY - COVER_OFFSET;
            case UP -> (float) bounds.maxY + COVER_OFFSET;
        };
        final int normal = (face.getStepX() * 127 & 255) | ((face.getStepY() * 127 & 255) << 8) | ((face.getStepZ() * 127 & 255) << 16);
        for (int i = 0; i < 4; i++) {
            final int index = i * VERTEX_SIZE;
            final float u = i < 2 ? 0 : 1;
            final float v = i == 1 || i == 2 ? 1 : 0;
            switch (face) {
                case NORTH -> setPosition(vertices, index, 1 - u, 1 - v, plane);
                case SOUTH -> setPosition(vertices, index, u, 1 - v, plane);
                case WEST -> setPosition(vertices, index, plane, 1 - v, u);
                case EAST -> setPosition(vertices, index, plane, 1 - v, 1 - u);
                case DOWN -> setPosition(vertices, index, u, plane, 1 - v);
                case UP -> setPosition(vertices, index, u, plane, v);
            }
            vertices[index + 3] = -1;
            vertices[index + 7] = normal;
            setUv(vertices, index, texture, u, v, mask.rotation());
        }
        return new BakedQuad(vertices, -1, face, texture, true);
    }

    private static void setPosition(final int[] vertices, final int index, final float x, final float y, final float z) {
        vertices[index] = Float.floatToRawIntBits(x);
        vertices[index + 1] = Float.floatToRawIntBits(y);
        vertices[index + 2] = Float.floatToRawIntBits(z);
    }

    private static void setUv(final int[] vertices, final int index, final TextureAtlasSprite texture,
            final float u, final float v, final int rotation) {
        final float s = switch (rotation) {
            case 1 -> v;
            case 2 -> 1 - u;
            case 3 -> 1 - v;
            default -> u;
        };
        final float t = switch (rotation) {
            case 1 -> 1 - u;
            case 2 -> 1 - v;
            case 3 -> u;
            default -> v;
        };
        vertices[index + 4] = Float.floatToRawIntBits(texture.getU(Mth.clamp(s, 0.001F, 0.999F) * 16));
        vertices[index + 5] = Float.floatToRawIntBits(texture.getV(Mth.clamp(t, 0.001F, 0.999F) * 16));
    }
}
