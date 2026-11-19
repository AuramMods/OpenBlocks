package art.arcane.openblocks.world;

import art.arcane.openblocks.config.OBServerConfig;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

public final class OBHeightMapSurvey {

    private OBHeightMapSurvey() {}

    public static boolean scanNext(final OBHeightMapData map, final ServerLevel level, final double x, final double z) {
        if (!level.dimension().location().equals(map.dimension()) || map.complete()) return false;
        LevelChunk nearest = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        final int size = map.chunksPerSide();
        final int minX = map.minChunkX();
        final int minZ = map.minChunkZ();
        for (int cx = minX; cx < minX + size; cx++) {
            for (int cz = minZ; cz < minZ + size; cz++) {
                if (map.hasChunk(cx, cz)) continue;
                final double dx = (cx << 4) + 7.5 - x;
                final double dz = (cz << 4) + 7.5 - z;
                final double distance = dx * dx + dz * dz;
                if (distance >= bestDistance) continue;
                final LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk != null) {
                    nearest = chunk;
                    bestDistance = distance;
                }
            }
        }
        if (nearest == null) return false;
        scanChunk(map, level, nearest);
        return true;
    }

    private static void scanChunk(final OBHeightMapData map, final ServerLevel level, final LevelChunk chunk) {
        final int step = map.blocksPerPixel();
        final int pixelsPerChunk = 16 / step;
        final ChunkPos chunkPos = chunk.getPos();
        final int pixelX = (chunkPos.x - map.minChunkX()) * pixelsPerChunk;
        final int pixelZ = (chunkPos.z - map.minChunkZ()) * pixelsPerChunk;
        final Set<Block> ignored = ignoredBlocks();
        final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        final int[] terrainColors = new int[64];
        final int[] liquidColors = new int[64];
        for (int px = 0; px < pixelsPerChunk; px++) {
            for (int pz = 0; pz < pixelsPerChunk; pz++) {
                Arrays.fill(terrainColors, 0);
                Arrays.fill(liquidColors, 0);
                int terrainSum = 0;
                int liquidSum = 0;
                int terrainCount = 0;
                int liquidCount = 0;
                for (int x = chunkPos.getMinBlockX() + px * step; x < chunkPos.getMinBlockX() + (px + 1) * step; x++) {
                    for (int z = chunkPos.getMinBlockZ() + pz * step; z < chunkPos.getMinBlockZ() + (pz + 1) * step; z++) {
                        boolean sawLiquid = false;
                        for (int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z); y >= level.getMinBuildHeight(); y--) {
                            final var state = chunk.getBlockState(position.set(x, y, z));
                            if (state.isAir() || ignored.contains(state.getBlock())) continue;
                            final int color = state.getMapColor(level, position).id;
                            if (color == 0) continue;
                            if (state.getBlock() instanceof LiquidBlock || state.is(Blocks.BUBBLE_COLUMN)) {
                                if (!sawLiquid) {
                                    liquidColors[color]++;
                                    liquidSum += y;
                                    liquidCount++;
                                    sawLiquid = true;
                                }
                            } else {
                                terrainColors[color]++;
                                terrainSum += y;
                                terrainCount++;
                                break;
                            }
                        }
                    }
                }
                final boolean showLiquid = liquidCount > step * step / 2;
                map.setSample(pixelX + px, pixelZ + pz,
                        terrainCount == 0 ? 0 : Math.floorDiv(terrainSum, terrainCount), mostCommon(terrainColors),
                        showLiquid ? Math.floorDiv(liquidSum, liquidCount) : 0, showLiquid ? mostCommon(liquidColors) : 0);
            }
        }
        map.markChunk(chunkPos);
    }

    private static int mostCommon(final int[] colors) {
        int color = MapColor.NONE.id;
        for (int i = 1; i < colors.length; i++) {
            if (colors[i] > colors[color]) color = i;
        }
        return color;
    }

    private static Set<Block> ignoredBlocks() {
        final var entries = OBServerConfig.CARTOGRAPHER_BLACKLIST.get();
        if (entries.isEmpty()) return Set.of();
        final Set<Block> ignored = new HashSet<>();
        for (final String entry : entries) {
            final ResourceLocation id = ResourceLocation.tryParse(entry);
            if (id != null) BuiltInRegistries.BLOCK.getOptional(id).ifPresent(ignored::add);
        }
        return ignored;
    }
}
