package art.arcane.openblocks.integration;

import art.arcane.openblocks.block.entity.OBProjectorBlockEntity;
import art.arcane.openblocks.world.OBHeightMapData;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public final class OBComputerCraftProjector implements GenericPeripheral {

    public static void register() { ComputerCraftAPI.registerGenericSource(new OBComputerCraftProjector()); }
    @Override public String id() { return "open_blocks:projector"; }
    @Override public PeripheralType getType() { return PeripheralType.ofAdditional("openblocks_projector"); }

    @LuaFunction(mainThread = true)
    @Nullable
    public final Object getMapId(final OBProjectorBlockEntity projector) {
        return projector.map() == null ? null : projector.mapId();
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getMapInfo(final OBProjectorBlockEntity projector) throws LuaException {
        final OBHeightMapData map = requireMap(projector);
        final Map<Integer, Integer> alpha = new HashMap<>();
        for (int i = 0; i < map.layerCount(); i++) alpha.put(i, map.layerAlpha(i));
        final String dimensionId = map.dimension().toString();
        final Object dimension = switch (dimensionId) {
            case "minecraft:the_nether" -> -1;
            case "minecraft:overworld" -> 0;
            case "minecraft:the_end" -> 1;
            default -> dimensionId;
        };
        return Map.of("center_x", map.centerX(), "center_z", map.centerZ(), "scale", map.scale(),
                "dimension", dimension, "dimension_id", dimensionId, "layers", alpha);
    }

    @LuaFunction(mainThread = true)
    public final void setMapInfo(final OBProjectorBlockEntity projector, final Map<?, ?> properties) throws LuaException {
        final OBHeightMapData map = requireMap(projector);
        final int x = properties.containsKey("center_x") ? integer(properties.get("center_x"), "center_x") : map.centerX();
        final int z = properties.containsKey("center_z") ? integer(properties.get("center_z"), "center_z") : map.centerZ();
        ResourceLocation dimension = map.dimension();
        int minHeight = map.minHeight();
        if (properties.containsKey("dimension")) {
            dimension = dimension(properties.get("dimension"));
            final ServerLevel target = ((ServerLevel) projector.getLevel()).getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (target == null) throw new LuaException("Unknown dimension: " + dimension);
            minHeight = target.getMinBuildHeight();
        }
        final Map<Integer, Integer> updates = new HashMap<>();
        if (properties.containsKey("layers")) {
            if (!(properties.get("layers") instanceof Map<?, ?> layers)) throw new LuaException("layers must be a table");
            for (final var entry : layers.entrySet()) {
                final int index = integer(entry.getKey(), "layer index");
                checkIndex(index, map.layerCount(), "layer");
                final int alpha = integer(entry.getValue(), "layer opacity");
                checkIndex(alpha, 256, "layer opacity");
                updates.put(index, alpha);
            }
        }
        map.setMetadata(x, z, dimension, minHeight);
        updates.forEach(map::setLayerAlpha);
    }

    @LuaFunction(mainThread = true)
    public final int getRotation(final OBProjectorBlockEntity projector) { return projector.rotation(); }

    @LuaFunction(mainThread = true)
    public final void rotate(final OBProjectorBlockEntity projector, final int steps) { projector.rotate(steps); }

    @LuaFunction(mainThread = true)
    public final Object[] getPoint(final OBProjectorBlockEntity projector, final int row, final int column, final int layer) throws LuaException {
        final OBHeightMapData map = requireMap(projector);
        checkPoint(map, row, column, layer);
        return new Object[] { map.layerHeight(layer, column, row), map.layerColor(layer, column, row) };
    }

    @LuaFunction(mainThread = true)
    public final void setPoint(final OBProjectorBlockEntity projector, final int row, final int column, final int layer,
            final int height, final int color) throws LuaException {
        final OBHeightMapData map = requireMap(projector);
        checkPoint(map, row, column, layer);
        if (height < DimensionType.MIN_Y || height > DimensionType.MAX_Y) throw new LuaException("height out of range");
        checkIndex(color, 64, "color");
        map.setPoint(layer, column, row, height, color);
    }

    @LuaFunction(mainThread = true)
    public final void clearMap(final OBProjectorBlockEntity projector) throws LuaException { requireMap(projector).clearMap(); }

    @LuaFunction(mainThread = true)
    public final void clearLayer(final OBProjectorBlockEntity projector, final int layer) throws LuaException {
        final OBHeightMapData map = requireMap(projector);
        checkIndex(layer, map.layerCount(), "layer");
        map.clearLayer(layer);
    }

    @LuaFunction(mainThread = true)
    public final void appendLayer(final OBProjectorBlockEntity projector) throws LuaException {
        if (!requireMap(projector).appendLayer()) throw new LuaException("Maximum number of layers is " + OBHeightMapData.MAX_LAYERS);
    }

    private static OBHeightMapData requireMap(final OBProjectorBlockEntity projector) throws LuaException {
        final OBHeightMapData map = projector.map();
        if (map == null) throw new LuaException("Map not loaded");
        return map;
    }

    private static void checkPoint(final OBHeightMapData map, final int row, final int column, final int layer) throws LuaException {
        checkIndex(row, OBHeightMapData.SIZE, "row");
        checkIndex(column, OBHeightMapData.SIZE, "column");
        checkIndex(layer, map.layerCount(), "layer");
    }

    private static void checkIndex(final int index, final int length, final String field) throws LuaException {
        if (index < 0 || index >= length) throw new LuaException(field + " out of range");
    }

    private static int integer(final Object value, final String field) throws LuaException {
        if (value instanceof Number number && number.doubleValue() == number.intValue()) return number.intValue();
        throw new LuaException(field + " must be an integer");
    }

    private static ResourceLocation dimension(final Object value) throws LuaException {
        if (value instanceof String name) {
            final ResourceLocation id = ResourceLocation.tryParse(name);
            if (id != null) return id;
        } else if (value instanceof Number) {
            return switch (integer(value, "dimension")) {
                case -1 -> Level.NETHER.location();
                case 0 -> Level.OVERWORLD.location();
                case 1 -> Level.END.location();
                default -> throw new LuaException("Unknown legacy dimension");
            };
        }
        throw new LuaException("dimension must be a resource ID or a legacy dimension number");
    }
}
