package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
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
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBPathGeometry implements IUnbakedGeometry<OBPathGeometry> {

    private static final long INVENTORY_SEED = -5077139222181326242L;
    private static final int MAX_STONES = 10;
    private static final ModelProperty<Long> SEED = new ModelProperty<>();

    @SubscribeEvent
    public static void registerLoader(final ModelEvent.RegisterGeometryLoaders event) {
        final IGeometryLoader<OBPathGeometry> loader = (json, context) -> new OBPathGeometry();
        event.register("path", loader);
    }

    @Override
    public BakedModel bake(final IGeometryBakingContext context, final ModelBaker baker,
            final Function<Material, TextureAtlasSprite> sprites, final ModelState modelState,
            final ItemOverrides overrides, final ResourceLocation modelLocation) {
        final TextureAtlasSprite sprite = sprites.apply(context.getMaterial("particle"));
        final var hint = context.getRenderTypeHint();
        final var renderTypes = hint == null ? RenderTypeGroup.EMPTY : context.getRenderType(hint);
        final var builder = IModelBuilder.of(context.useAmbientOcclusion(), context.useBlockLight(), context.isGui3d(),
                context.getTransforms(), overrides, sprite, renderTypes);
        final var inventory = bakeStones(INVENTORY_SEED, sprite, modelState, modelLocation);
        inventory.visible().forEach(builder::addUnculledFace);
        inventory.bottom().forEach(builder::addUnculledFace);

        final LoadingCache<Long, PathQuads> cache = CacheBuilder.newBuilder().maximumSize(256)
                .build(CacheLoader.from(seed -> bakeStones(seed, sprite, modelState, modelLocation)));
        final Direction bottom = modelState.getRotation().rotateTransform(Direction.DOWN);
        return new BakedModelWrapper<BakedModel>(builder.build()) {
            @Override
            public ModelData getModelData(final BlockAndTintGetter level, final BlockPos pos,
                    final BlockState state, final ModelData data) {
                return data.derive().with(SEED, state.getSeed(pos)).build();
            }

            @Override
            public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction face,
                    final RandomSource random, final ModelData data, @Nullable final RenderType renderType) {
                if (state == null) return originalModel.getQuads(null, face, random, data, renderType);
                return pathQuads(face, data.has(SEED) ? data.get(SEED) : random.nextLong());
            }

            @Override
            public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction face, final RandomSource random) {
                return state == null ? originalModel.getQuads(null, face, random) : pathQuads(face, random.nextLong());
            }

            private List<BakedQuad> pathQuads(@Nullable final Direction face, final long seed) {
                if (face != null && face != bottom) return List.of();
                final PathQuads quads = cache.getUnchecked(seed);
                return face == null ? quads.visible() : quads.bottom();
            }
        };
    }

    private static PathQuads bakeStones(final long seed, final TextureAtlasSprite sprite,
            final ModelState state, final ResourceLocation location) {
        final Random random = new Random(seed);
        final List<Stone> stones = new ArrayList<>(MAX_STONES);
        final List<BakedQuad> visible = new ArrayList<>(MAX_STONES * 5);
        final List<BakedQuad> bottom = new ArrayList<>(MAX_STONES);

        for (int i = 0; i < MAX_STONES; i++) {
            final double width = random.nextDouble() * 0.3 + 0.1;
            final double length = random.nextDouble() * 0.3 + 0.1;
            final double x = random.nextDouble() * (1.0 - width);
            final double z = random.nextDouble() * (1.0 - length);
            final Stone stone = new Stone(x, z, x + width, z + length);
            if (stones.stream().anyMatch(stone::overlaps)) break;
            stones.add(stone);

            final var faces = new EnumMap<Direction, BlockElementFace>(Direction.class);
            for (final Direction direction : Direction.values()) {
                faces.put(direction, new BlockElementFace(null, -1, "#particle", new BlockFaceUV(null, 0)));
            }
            final var element = new BlockElement(new Vector3f((float) (16 * stone.x()), 0, (float) (16 * stone.z())),
                    new Vector3f((float) (16 * stone.endX()), 1, (float) (16 * stone.endZ())), faces, null, true);
            faces.forEach((direction, face) -> {
                final BakedQuad quad = UnbakedGeometryHelper.bakeElementFace(element, face, sprite, direction, state, location);
                (direction == Direction.DOWN ? bottom : visible).add(quad);
            });
        }
        return new PathQuads(List.copyOf(visible), List.copyOf(bottom));
    }

    private record Stone(double x, double z, double endX, double endZ) {
        boolean overlaps(final Stone other) {
            return x < other.endX && endX > other.x && z < other.endZ && endZ > other.z;
        }
    }

    private record PathQuads(List<BakedQuad> visible, List<BakedQuad> bottom) {}
}
