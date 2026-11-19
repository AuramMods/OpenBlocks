package art.arcane.openblocks.world;

import java.util.Arrays;
import java.util.BitSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
    private static final int PIXELS = SIZE * SIZE;
    private static final String FILE_PREFIX = "open_blocks_height_map_";

    private ResourceLocation dimension = Level.OVERWORLD.location();
    private int centerX;
    private int centerZ;
    private int scale;
    private long survey;
    private final BitSet chunks = new BitSet();
    private final int[] terrainHeights = new int[PIXELS];
    private final int[] liquidHeights = new int[PIXELS];
    private final byte[] terrainColors = new byte[PIXELS];
    private final byte[] liquidColors = new byte[PIXELS];

    public record Sample(int terrainY, int terrainColor, int liquidY, int liquidColor) {}

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
        final DimensionDataStorage storage = storage(level);
        final Ids ids = storage.computeIfAbsent(Ids::load, Ids::new, FILE_PREFIX + "ids");
        while (get(level, ids.nextId) != null) ids.nextId++;
        final int id = ids.nextId++;
        ids.setDirty();
        final OBHeightMapData map = new OBHeightMapData();
        map.reset(level, center);
        map.scale = Mth.clamp(scale, 0, MAX_SCALE);
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
        dimension = level.dimension().location();
        centerX = center.getX() & ~15;
        centerZ = center.getZ() & ~15;
        survey++;
        chunks.clear();
        Arrays.fill(terrainColors, (byte) 0);
        Arrays.fill(liquidColors, (byte) 0);
        Arrays.fill(terrainHeights, 0);
        Arrays.fill(liquidHeights, 0);
        setDirty();
    }

    public ResourceLocation dimension() { return dimension; }
    public int centerX() { return centerX; }
    public int centerZ() { return centerZ; }
    public int scale() { return scale; }
    public long survey() { return survey; }
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
        setDirty();
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
        final int index = z * SIZE + x;
        return new Sample(terrainHeights[index], Byte.toUnsignedInt(terrainColors[index]), liquidHeights[index], Byte.toUnsignedInt(liquidColors[index]));
    }

    void setSample(final int x, final int z, final int terrainY, final int terrainColor, final int liquidY, final int liquidColor) {
        final int index = z * SIZE + x;
        terrainHeights[index] = terrainY;
        terrainColors[index] = (byte) terrainColor;
        liquidHeights[index] = liquidY;
        liquidColors[index] = (byte) liquidColor;
    }

    public static OBHeightMapData load(final CompoundTag tag) {
        final OBHeightMapData map = new OBHeightMapData();
        final ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimension != null && tag.contains("Dimension")) map.dimension = dimension;
        map.centerX = tag.getInt("CenterX");
        map.centerZ = tag.getInt("CenterZ");
        map.scale = Mth.clamp(tag.getByte("Scale"), 0, MAX_SCALE);
        map.survey = Math.max(1, tag.getLong("Survey"));
        final int[] terrainY = tag.getIntArray("TerrainHeight");
        final int[] liquidY = tag.getIntArray("LiquidHeight");
        final byte[] terrainColor = tag.getByteArray("TerrainColor");
        final byte[] liquidColor = tag.getByteArray("LiquidColor");
        if (terrainY.length == PIXELS && liquidY.length == PIXELS && terrainColor.length == PIXELS && liquidColor.length == PIXELS) {
            System.arraycopy(terrainY, 0, map.terrainHeights, 0, PIXELS);
            System.arraycopy(liquidY, 0, map.liquidHeights, 0, PIXELS);
            System.arraycopy(terrainColor, 0, map.terrainColors, 0, PIXELS);
            System.arraycopy(liquidColor, 0, map.liquidColors, 0, PIXELS);
            final int count = map.chunksPerSide() * map.chunksPerSide();
            map.chunks.or(BitSet.valueOf(tag.getLongArray("Chunks")).get(0, count));
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
        tag.putLongArray("Chunks", chunks.toLongArray());
        tag.putIntArray("TerrainHeight", terrainHeights.clone());
        tag.putIntArray("LiquidHeight", liquidHeights.clone());
        tag.putByteArray("TerrainColor", terrainColors.clone());
        tag.putByteArray("LiquidColor", liquidColors.clone());
        return tag;
    }
}
