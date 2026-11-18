package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBTankGeometry implements IUnbakedGeometry<OBTankGeometry> {

    private static final ModelProperty<Integer> HIDDEN_EDGES = new ModelProperty<>();
    private static final List<Edge> EDGES = List.of(
            new Edge(Direction.DOWN, Direction.NORTH, true), new Edge(Direction.DOWN, Direction.SOUTH, true),
            new Edge(Direction.UP, Direction.NORTH, false), new Edge(Direction.UP, Direction.SOUTH, false),
            new Edge(Direction.NORTH, Direction.WEST, true), new Edge(Direction.NORTH, Direction.EAST, true),
            new Edge(Direction.SOUTH, Direction.WEST, false), new Edge(Direction.SOUTH, Direction.EAST, false),
            new Edge(Direction.DOWN, Direction.WEST, true), new Edge(Direction.DOWN, Direction.EAST, true),
            new Edge(Direction.UP, Direction.WEST, false), new Edge(Direction.UP, Direction.EAST, false));

    @SubscribeEvent
    public static void registerLoader(final ModelEvent.RegisterGeometryLoaders event) {
        final IGeometryLoader<OBTankGeometry> loader = (json, context) -> new OBTankGeometry();
        event.register("tank", loader);
    }

    @Override
    public BakedModel bake(final IGeometryBakingContext context, final ModelBaker baker,
            final Function<Material, TextureAtlasSprite> sprites, final ModelState state,
            final ItemOverrides overrides, final ResourceLocation location) {
        final TextureAtlasSprite sprite = sprites.apply(context.getMaterial("particle"));
        final var builder = IModelBuilder.of(context.useAmbientOcclusion(), context.useBlockLight(), context.isGui3d(),
                context.getTransforms(), overrides, sprite, RenderTypeGroup.EMPTY);
        final List<List<BakedQuad>> edgeQuads = EDGES.stream().map(edge -> bakeEdge(edge, sprite, state, location)).toList();
        edgeQuads.forEach(quads -> quads.forEach(builder::addUnculledFace));

        return new BakedModelWrapper<BakedModel>(builder.build()) {
            @Override
            public ModelData getModelData(final BlockAndTintGetter level, final BlockPos pos,
                    final BlockState blockState, final ModelData data) {
                if (!(level.getBlockEntity(pos) instanceof OBTankBlockEntity tank)) return data;
                final FluidStack contents = tank.contents();
                final var adjacent = new EnumMap<Direction, Boolean>(Direction.class);
                for (final Direction side : Direction.values()) {
                    adjacent.put(side, connects(level, pos.relative(side), contents));
                }
                int hidden = 0;
                for (int i = 0; i < EDGES.size(); i++) {
                    final Edge edge = EDGES.get(i);
                    final boolean a = adjacent.get(edge.a());
                    final boolean b = adjacent.get(edge.b());
                    if (a != b || ((a || edge.checkEmptyDiagonal())
                            && connects(level, pos.relative(edge.a()).relative(edge.b()), contents))) hidden |= 1 << i;
                }
                return data.derive().with(HIDDEN_EDGES, hidden).build();
            }

            @Override
            public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                    final RandomSource random, final ModelData data, @Nullable final RenderType renderType) {
                if (side != null) return List.of();
                final Integer hidden = data.get(HIDDEN_EDGES);
                if (hidden == null || hidden == 0) return originalModel.getQuads(state, null, random);
                final List<BakedQuad> visible = new ArrayList<>();
                for (int i = 0; i < edgeQuads.size(); i++) {
                    if ((hidden & (1 << i)) == 0) visible.addAll(edgeQuads.get(i));
                }
                return visible;
            }
        };
    }

    private static boolean connects(final BlockAndTintGetter level, final BlockPos pos, final FluidStack own) {
        if (!(level.getBlockEntity(pos) instanceof OBTankBlockEntity other)) return false;
        final FluidStack fluid = other.contents();
        return own.isEmpty() || fluid.isEmpty() || own.isFluidEqual(fluid);
    }

    private static List<BakedQuad> bakeEdge(final Edge edge, final TextureAtlasSprite sprite,
            final ModelState state, final ResourceLocation location) {
        final Vector3f start = new Vector3f(-0.1F);
        final Vector3f end = new Vector3f(16.1F);
        connectEdgeAxis(start, end, edge.a());
        connectEdgeAxis(start, end, edge.b());
        final var faces = new EnumMap<Direction, BlockElementFace>(Direction.class);
        for (final Direction side : Direction.values()) {
            faces.put(side, new BlockElementFace(null, -1, "#particle", new BlockFaceUV(null, 0)));
        }
        final var element = new BlockElement(start, end, faces, null, true);
        return faces.entrySet().stream().map(face ->
                UnbakedGeometryHelper.bakeElementFace(element, face.getValue(), sprite, face.getKey(), state, location)).toList();
    }

    private static void connectEdgeAxis(final Vector3f start, final Vector3f end, final Direction side) {
        final int axis = side.getAxis().ordinal();
        if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            start.setComponent(axis, 15.9F);
        } else {
            end.setComponent(axis, 0.1F);
        }
    }

    private record Edge(Direction a, Direction b, boolean checkEmptyDiagonal) {}
}
