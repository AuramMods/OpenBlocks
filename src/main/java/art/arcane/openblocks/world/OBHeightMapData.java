package art.arcane.openblocks.world;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class OBHeightMapData extends SavedData {

    public static final int SIZE = 64;
    public static final int MAX_SCALE = 4;
    public static final int MAX_LAYERS = 16;
    private static final int PIXELS = SIZE * SIZE;
    private static final String FILE_PREFIX = "open_blocks_height_map_";

    private ResourceLocation dimension = Level.OVERWORLD.location();
    private int centerX;
    private int centerZ;
    private int scale;
    private long survey;
    private long revision;
    private int minHeight;
    private final BitSet chunks = new BitSet();
    private final List<Layer> layers = new ArrayList<>();

    public record Sample(int terrainY, int terrainColor, int liquidY, int liquidColor) {}

    private static final class Layer {
        private int alpha;
        private final int[] heights = new int[PIXELS];
        private final byte[] colors = new byte[PIXELS];

        private Layer(final int alpha) {
            this.alpha = alpha;
        }

        private static Layer load(final int alpha, final int[] heights, final byte[] colors) {
            final Layer layer = new Layer(Mth.clamp(alpha, 0, 255));
            if (heights.length == PIXELS && colors.length == PIXELS) {
                System.arraycopy(heights, 0, layer.heights, 0, PIXELS);
                System.arraycopy(colors, 0, layer.colors, 0, PIXELS);
            }
            return layer;
        }

        private CompoundTag save() {
            final CompoundTag tag = new CompoundTag();
            tag.putInt("Alpha", alpha);
            tag.putIntArray("Height", heights.clone());
            tag.putByteArray("Color", colors.clone());
            return tag;
        }
    }

    private static final class Ids extends SavedData {
        private int nextId;

        private static Ids load(final CompoundTag tag) {
            final Ids ids = new Ids();
            ids.nextId = Math.max(0, tag.getInt("NextId"));
            return ids;
        }

        @Override
        public CompoundTag save(final CompoundTag tag) {
            tag.putInt("NextId", nextId);
            return tag;
        }
    }

    public static int create(final ServerLevel level, final BlockPos center, final int scale) {
        return create(level, center, scale, true);
    }

    public static int createCanvas(final ServerLevel level, final BlockPos center, final int scale) {
        return create(level, center, scale, false);
    }

    private static int create(final ServerLevel level, final BlockPos center, final int scale, final boolean survey) {
        final DimensionDataStorage storage = storage(level);
        final Ids ids = storage.computeIfAbsent(Ids::load, Ids::new, FILE_PREFIX + "ids");
        while (get(level, ids.nextId) != null) ids.nextId++;
        final int id = ids.nextId++;
        ids.setDirty();
        final OBHeightMapData map = new OBHeightMapData();
        map.scale = Mth.clamp(scale, 0, MAX_SCALE);
        map.setSurveyOrigin(level, center);
        if (survey) map.addSurveyLayers();
        storage.set(FILE_PREFIX + id, map);
        return id;
    }

    @Nullable
    public static OBHeightMapData get(final ServerLevel level, final int id) {
        return id < 0 ? null : storage(level).get(OBHeightMapData::load, FILE_PREFIX + id);
    }

    private static DimensionDataStorage storage(final ServerLevel level) {
        return level.getServer().overworld().getDataStorage();
    }

    public void reset(final ServerLevel level, final BlockPos center) {
        setSurveyOrigin(level, center);
        addSurveyLayers();
    }

    private void setSurveyOrigin(final ServerLevel level, final BlockPos center) {
        dimension = level.dimension().location();
        minHeight = level.getMinBuildHeight();
        centerX = center.getX() & ~15;
        centerZ = center.getZ() & ~15;
        survey++;
        chunks.clear();
        changed();
    }

    private void addSurveyLayers() {
        layers.clear();
        layers.add(new Layer(255));
        layers.add(new Layer(128));
    }

    private void changed() {
        revision++;
        setDirty();
    }

    public ResourceLocation dimension() { return dimension; }
    public int centerX() { return centerX; }
    public int centerZ() { return centerZ; }
    public int scale() { return scale; }
    public long survey() { return survey; }
    public long revision() { return revision; }
    public int minHeight() { return minHeight; }
    public int blocksPerPixel() { return 1 << scale; }
    public int chunksPerSide() { return 4 << scale; }
    public int surveyedChunks() { return chunks.cardinality(); }
    public boolean complete() { return surveyedChunks() == chunksPerSide() * chunksPerSide(); }

    public int minChunkX() { return (centerX >> 4) - chunksPerSide() / 2; }
    public int minChunkZ() { return (centerZ >> 4) - chunksPerSide() / 2; }

    public boolean hasChunk(final int x, final int z) {
        return chunks.get(chunkIndex(x, z));
    }

    void markChunk(final ChunkPos chunk) {
        chunks.set(chunkIndex(chunk.x, chunk.z));
        changed();
    }

    private int chunkIndex(final int x, final int z) {
        return (z - minChunkZ()) * chunksPerSide() + x - minChunkX();
    }

    public CompoundTag progress() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("Size", chunksPerSide());
        tag.putLongArray("Chunks", chunks.toLongArray());
        return tag;
    }

    public Sample sample(final int x, final int z) {
        return new Sample(layerHeight(0, x, z), layerColor(0, x, z), layerHeight(1, x, z), layerColor(1, x, z));
    }

    void setSample(final int x, final int z, final int terrainY, final int terrainColor, final int liquidY, final int liquidColor) {
        setPoint(layers.get(0), x, z, terrainY, terrainColor);
        setPoint(layers.get(1), x, z, liquidY, liquidColor);
    }

    public int layerCount() { return layers.size(); }

    public int layerAlpha(final int layer) { return layers.get(layer).alpha; }

    public int layerHeight(final int layer, final int x, final int z) { return layers.get(layer).heights[z * SIZE + x]; }

    public int layerColor(final int layer, final int x, final int z) { return Byte.toUnsignedInt(layers.get(layer).colors[z * SIZE + x]); }

    private static void setPoint(final Layer layer, final int x, final int z, final int height, final int color) {
        final int index = z * SIZE + x;
        layer.heights[index] = height;
        layer.colors[index] = (byte) color;
    }

    public void setPoint(final int layer, final int x, final int z, final int height, final int color) {
        final Layer mapLayer = layers.get(layer);
        if (layerHeight(layer, x, z) == height && layerColor(layer, x, z) == color) return;
        setPoint(mapLayer, x, z, height, color);
        changed();
    }

    public void setMetadata(final int x, final int z, final ResourceLocation dimension, final int minHeight) {
        if (centerX == x && centerZ == z && this.dimension.equals(dimension) && this.minHeight == minHeight) return;
        centerX = x;
        centerZ = z;
        this.dimension = dimension;
        this.minHeight = minHeight;
        survey++;
        chunks.clear();
        changed();
    }

    public void setLayerAlpha(final int layer, final int alpha) {
        final Layer mapLayer = layers.get(layer);
        if (mapLayer.alpha == alpha) return;
        mapLayer.alpha = alpha;
        changed();
    }

    public boolean appendLayer() {
        if (layers.size() >= MAX_LAYERS) return false;
        layers.add(new Layer(255));
        changed();
        return true;
    }

    public void clearLayer(final int layer) {
        layers.set(layer, new Layer(255));
        changed();
    }

    public void clearMap() {
        layers.clear();
        survey++;
        chunks.clear();
        changed();
    }

    public static OBHeightMapData load(final CompoundTag tag) {
        final OBHeightMapData map = new OBHeightMapData();
        final ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimension != null && tag.contains("Dimension")) map.dimension = dimension;
        map.centerX = tag.getInt("CenterX");
        map.centerZ = tag.getInt("CenterZ");
        map.scale = Mth.clamp(tag.getByte("Scale"), 0, MAX_SCALE);
        map.survey = Math.max(1, tag.getLong("Survey"));
        map.minHeight = tag.contains("MinHeight", Tag.TAG_INT) ? tag.getInt("MinHeight") : map.dimension.equals(Level.OVERWORLD.location()) ? -64 : 0;
        final int count = map.chunksPerSide() * map.chunksPerSide();
        map.chunks.or(BitSet.valueOf(tag.getLongArray("Chunks")).get(0, count));
        map.revision = tag.contains("Revision", Tag.TAG_LONG) ? tag.getLong("Revision") : map.survey + map.surveyedChunks();
        if (tag.contains("Layers", Tag.TAG_LIST)) {
            final ListTag layers = tag.getList("Layers", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(layers.size(), MAX_LAYERS); i++) {
                final CompoundTag layer = layers.getCompound(i);
                map.layers.add(Layer.load(layer.getInt("Alpha"), layer.getIntArray("Height"), layer.getByteArray("Color")));
            }
        } else {
            // Earlier Forge 1.20.1 surveys stored terrain and liquid in four top-level arrays.
            map.layers.add(Layer.load(255, tag.getIntArray("TerrainHeight"), tag.getByteArray("TerrainColor")));
            map.layers.add(Layer.load(128, tag.getIntArray("LiquidHeight"), tag.getByteArray("LiquidColor")));
        }
        return map;
    }

    @Override
    public CompoundTag save(final CompoundTag tag) {
        tag.putString("Dimension", dimension.toString());
        tag.putInt("CenterX", centerX);
        tag.putInt("CenterZ", centerZ);
        tag.putByte("Scale", (byte) scale);
        tag.putLong("Survey", survey);
        tag.putLong("Revision", revision);
        tag.putInt("MinHeight", minHeight);
        tag.putLongArray("Chunks", chunks.toLongArray());
        final ListTag savedLayers = new ListTag();
        for (final Layer layer : layers) savedLayers.add(layer.save());
        tag.put("Layers", savedLayers);
        return tag;
    }
}
