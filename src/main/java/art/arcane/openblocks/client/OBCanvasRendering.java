package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final ModelProperty<ModelData> BASE_DATA = new ModelProperty<>();

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
            if (surface == null || surface.original().isAir()) return data;
            final BlockState original = surface.original();
            final ModelData originalData = modelFor(original).getModelData(level, pos, original, ModelData.EMPTY);
            return data.derive().with(BASE_DATA, originalData).build();
        }

        private static ModelData baseData(final ModelData data) {
            final ModelData original = data.get(BASE_DATA);
            return original == null ? ModelData.EMPTY : original;
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(final BlockState state, final RandomSource random, final ModelData data) {
            final Surface surface = data.get(OBCanvasBlockEntity.SURFACE);
            if (surface == null || surface.original().isAir()) return super.getRenderTypes(state, random, data);
            return modelFor(surface.original()).getRenderTypes(surface.original(), random, baseData(data));
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
            if (surface == null) return super.getQuads(state, side, random, data, layer);
            final BlockState original = surface.original();
            final List<BakedQuad> source = original.isAir()
                    ? super.getQuads(state, side, random, data, layer)
                    : modelFor(original).getQuads(original, side, random, baseData(data), layer);
            if (source.isEmpty()) return source;
            final List<BakedQuad> result = new ArrayList<>(source.size());
            final TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(OpenBlocks.id("block/canvas"));
            for (final BakedQuad quad : source) {
                final int rgb = surface.color(quad.getDirection());
                result.add(rgb == OBCanvasBlockEntity.UNPAINTED ? quad : paint(quad, white, rgb));
            }
            return result;
        }

        private static BakedQuad paint(final BakedQuad source, final TextureAtlasSprite white, final int rgb) {
            final int[] vertices = source.getVertices().clone();
            for (int i = 0; i < 4; i++) {
                final int uv = i * VERTEX_SIZE + 4;
                vertices[uv] = Float.floatToRawIntBits(white.getU(8));
                vertices[uv + 1] = Float.floatToRawIntBits(white.getV(8));
            }
            return new BakedQuad(vertices, RGB_TINT | rgb, source.getDirection(), white, source.isShade(), source.hasAmbientOcclusion());
        }
    }
}
