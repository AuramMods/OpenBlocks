package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity;
import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity.Output;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBVacuumHopperGeometry implements IUnbakedGeometry<OBVacuumHopperGeometry> {

    private static final ModelProperty<Integer> OUTPUTS = new ModelProperty<>();
    private static final Direction[] SIDES = Direction.values();
    private static final int FACE_COUNT = SIDES.length;

    @SubscribeEvent
    public static void registerLoader(final ModelEvent.RegisterGeometryLoaders event) {
        final IGeometryLoader<OBVacuumHopperGeometry> loader = (json, context) -> new OBVacuumHopperGeometry();
        event.register("vacuum_hopper", loader);
    }

    @Override
    public void resolveParents(final Function<ResourceLocation, UnbakedModel> modelGetter, final IGeometryBakingContext context) {
        modelGetter.apply(partLocation("body")).resolveParents(modelGetter);
        for (final Direction side : SIDES) modelGetter.apply(partLocation(side.getName())).resolveParents(modelGetter);
    }

    @Override
    public BakedModel bake(final IGeometryBakingContext context, final ModelBaker baker,
            final Function<Material, TextureAtlasSprite> sprites, final ModelState modelState,
            final ItemOverrides overrides, final ResourceLocation location) {
        final var bodySprite = sprites.apply(context.getMaterial("particle"));
        final var textures = List.of(sprites.apply(context.getMaterial("items")), sprites.apply(context.getMaterial("fluids")),
                sprites.apply(context.getMaterial("both")));
        final var body = bakePart(context, baker, modelState, overrides, partLocation("body"), bodySprite);
        final BakedModel[][] ports = new BakedModel[FACE_COUNT][textures.size()];
        for (final Direction face : SIDES) {
            final var part = partLocation(face.getName());
            for (int i = 0; i < textures.size(); i++) ports[face.ordinal()][i] = bakePart(context, baker, modelState, overrides, part, textures.get(i));
        }

        return new BakedModelWrapper<BakedModel>(body) {
            @Override
            public ModelData getModelData(final BlockAndTintGetter level, final BlockPos pos, final BlockState state, final ModelData data) {
                if (!(level.getBlockEntity(pos) instanceof OBVacuumHopperBlockEntity hopper)) return data;
                final int outputs = hopper.outputMask(Output.ITEMS) | (hopper.outputMask(Output.XP) << FACE_COUNT);
                return data.derive().with(OUTPUTS, outputs).build();
            }

            @Override
            public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                    final RandomSource random, final ModelData data, @Nullable final RenderType renderType) {
                final var base = originalModel.getQuads(state, side, random, data, renderType);
                final Integer outputs = data.get(OUTPUTS);
                if (state == null || outputs == null || outputs == 0) return base;
                final List<BakedQuad> quads = new ArrayList<>(base);
                for (int face = 0; face < FACE_COUNT; face++) {
                    final int items = (outputs >>> face) & 1;
                    final int fluid = (outputs >>> (face + FACE_COUNT)) & 1;
                    final int variant = items + fluid * 2;
                    if (variant > 0) quads.addAll(ports[face][variant - 1].getQuads(state, side, random, ModelData.EMPTY, renderType));
                }
                return quads;
            }
        };
    }

    private static ResourceLocation partLocation(final String name) {
        return OpenBlocks.id("block/vacuum_hopper_" + name);
    }

    private static BakedModel bakePart(final IGeometryBakingContext context, final ModelBaker baker, final ModelState state,
            final ItemOverrides overrides, final ResourceLocation part, final TextureAtlasSprite sprite) {
        final var builder = IModelBuilder.of(context.useAmbientOcclusion(), context.useBlockLight(), context.isGui3d(),
                context.getTransforms(), overrides, sprite, RenderTypeGroup.EMPTY);
        final var model = (BlockModel) baker.getModel(part);
        for (final var element : model.getElements()) {
            element.faces.forEach((direction, face) -> {
                final BakedQuad quad = UnbakedGeometryHelper.bakeElementFace(element, face, sprite, direction, state, part);
                if (face.cullForDirection == null) builder.addUnculledFace(quad);
                else builder.addCulledFace(state.getRotation().rotateTransform(face.cullForDirection), quad);
            });
        }
        return builder.build();
    }
}
