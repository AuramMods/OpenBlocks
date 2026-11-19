package art.arcane.openblocks.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public enum OBGuideShape {
    SPHERE, CYLINDER, CUBOID, FULL_CUBOID, DOME, TRIANGLE, PENTAGON, HEXAGON, OCTAGON, AXES, PLANES;

    private static final OBGuideShape[] SHAPES = values();
    private static final Comparator<BlockPos> BUILD_ORDER = Comparator.comparingInt((BlockPos pos) -> pos.getY())
            .thenComparingDouble(pos -> Math.atan2(pos.getZ(), pos.getX()))
            .thenComparingInt(pos -> -pos.getX() * pos.getX() - pos.getZ() * pos.getZ())
            .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ);

    private final String id = name().toLowerCase(Locale.ROOT);

    public record Bounds(int negX, int negY, int negZ, int posX, int posY, int posZ) {
        public static final Bounds DEFAULT = new Bounds(8, 8, 8, 8, 8, 8);
    }

    public String id() { return id; }

    public Component label() { return Component.translatable("guide.open_blocks.shape." + id); }

    public static OBGuideShape byIndex(final int index) { return SHAPES[Math.floorMod(index, SHAPES.length)]; }

    @Nullable
    public static OBGuideShape byName(final String name) {
        final String normalized = name.toLowerCase(Locale.ROOT).replace("_", "");
        for (final OBGuideShape shape : SHAPES) if (shape.id.replace("_", "").equals(normalized)) return shape;
        return null;
    }

    public List<BlockPos> points(final Bounds bounds, final OBGuideOrientation orientation, final boolean builder) {
        final Set<BlockPos> points = new HashSet<>();
        switch (this) {
            case SPHERE, DOME, CYLINDER -> round(points, bounds);
            case CUBOID, FULL_CUBOID -> box(points, bounds, this == FULL_CUBOID);
            case TRIANGLE -> polygon(points, bounds, 3);
            case PENTAGON -> polygon(points, bounds, 5);
            case HEXAGON -> polygon(points, bounds, 6);
            case OCTAGON -> polygon(points, bounds, 8);
            case AXES, PLANES -> grid(points, bounds, this == PLANES);
        }
        points.remove(BlockPos.ZERO);
        if (builder) points.removeIf(pos -> Math.abs(pos.getX()) <= 1 && Math.abs(pos.getY()) <= 1 && Math.abs(pos.getZ()) <= 1);
        final List<BlockPos> sorted = new ArrayList<>(points);
        sorted.sort(BUILD_ORDER);
        return sorted.stream().map(orientation::offset).toList();
    }

    private record SampleAxis(int center, int radius, boolean evenLength) {

        static SampleAxis from(final int min, final int max) {
            final int span = max - min;
            return new SampleAxis(Math.floorDiv(min + max, 2), (span + 1) / 2, (span & 1) != 0);
        }

        int map(final int value) { return center + (evenLength && value < 0 ? value + 1 : value); }

        double[] distances() {
            final double[] result = new double[radius + 2];
            final double step = 1.0 / (radius + 0.5);
            double distance = 0;
            for (int i = 0; i < result.length; i++, distance += step) result[i] = distance * distance;
            return result;
        }
    }

    private void round(final Set<BlockPos> out, final Bounds bounds) {
        final SampleAxis ax = SampleAxis.from(-bounds.negX(), bounds.posX());
        final SampleAxis ay = this == CYLINDER ? SampleAxis.from(0, 0) : SampleAxis.from(-bounds.negY(), bounds.posY());
        final SampleAxis az = SampleAxis.from(-bounds.negZ(), bounds.posZ());
        final double[] dx = ax.distances();
        final double[] dy = ay.distances();
        final double[] dz = az.distances();
        final boolean cylinder = this == CYLINDER;
        final boolean dome = this == DOME;
        for (int x = 0; x <= ax.radius(); x++) {
            if (x == 0 && ax.evenLength()) continue;
            for (int y = 0; y <= ay.radius() && dx[x] + dy[y] <= 1; y++) {
                if (y == 0 && ay.evenLength()) continue;
                for (int z = 0; z <= az.radius() && dx[x] + dy[y] + dz[z] <= 1; z++) {
                    if (z == 0 && az.evenLength()) continue;
                    if (dx[x + 1] + dy[y] + dz[z] <= 1 && dx[x] + dy[y] + dz[z + 1] <= 1
                            && (cylinder || dx[x] + dy[y + 1] + dz[z] <= 1)) continue;
                    for (int sx = x == 0 ? 1 : -1; sx <= 1; sx += 2) {
                        for (int sz = z == 0 || dome ? 1 : -1; sz <= 1; sz += 2) {
                            if (cylinder) {
                                column(out, bounds, ax.map(sx * x), az.map(sz * z));
                            } else {
                                for (int sy = y == 0 ? 1 : -1; sy <= 1; sy += 2) {
                                    out.add(new BlockPos(ax.map(sx * x), ay.map(sy * y), az.map(sz * z)));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void box(final Set<BlockPos> out, final Bounds b, final boolean walls) {
        final int[] xs = {-b.negX(), b.posX()};
        final int[] ys = {-b.negY(), b.posY()};
        final int[] zs = {-b.negZ(), b.posZ()};
        for (final int x : xs) {
            for (int y = ys[0]; y <= ys[1]; y++) {
                if (walls || y == ys[0] || y == ys[1]) {
                    for (int z = zs[0]; z <= zs[1]; z++) out.add(new BlockPos(x, y, z));
                } else {
                    for (final int z : zs) out.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int x = xs[0] + 1; x < xs[1]; x++) {
            for (final int y : ys) {
                if (walls) {
                    for (int z = zs[0]; z <= zs[1]; z++) out.add(new BlockPos(x, y, z));
                } else {
                    for (final int z : zs) out.add(new BlockPos(x, y, z));
                }
            }
            if (walls) {
                for (int y = ys[0] + 1; y < ys[1]; y++) {
                    for (final int z : zs) out.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    private static void grid(final Set<BlockPos> out, final Bounds b, final boolean planes) {
        if (!planes) {
            for (int x = -b.negX(); x <= b.posX(); x++) out.add(new BlockPos(x, 0, 0));
            for (int y = -b.negY(); y <= b.posY(); y++) out.add(new BlockPos(0, y, 0));
            for (int z = -b.negZ(); z <= b.posZ(); z++) out.add(new BlockPos(0, 0, z));
            return;
        }
        for (int y = -b.negY(); y <= b.posY(); y++) {
            for (int x = -b.negX(); x <= b.posX(); x++) out.add(new BlockPos(x, y, 0));
            for (int z = -b.negZ(); z <= b.posZ(); z++) out.add(new BlockPos(0, y, z));
        }
        for (int x = -b.negX(); x <= b.posX(); x++) {
            for (int z = -b.negZ(); z <= b.posZ(); z++) out.add(new BlockPos(x, 0, z));
        }
    }

    private static void polygon(final Set<BlockPos> out, final Bounds bounds, final int sides) {
        final BiConsumer<Integer, Integer> drawColumn = (x, z) -> column(out, bounds, x, z);
        final boolean centered = bounds.negX() == bounds.posX() && bounds.negZ() == bounds.posZ();
        if (centered && (sides != 8 || bounds.posX() == bounds.posZ())) {
            centeredPolygon(bounds, sides, drawColumn);
            return;
        }
        final double middleX = (bounds.posX() - bounds.negX()) / 2.0;
        final double middleZ = (bounds.posZ() - bounds.negZ()) / 2.0;
        final double radiusX = (bounds.posX() + bounds.negX()) / 2.0;
        final double radiusZ = (bounds.posZ() + bounds.negZ()) / 2.0;
        int x = bounds.posX();
        int z = (int) Math.round(middleZ);
        for (int side = 1; side <= sides; side++) {
            final double angle = 2 * Math.PI * side / sides;
            final int nextX = (int) Math.round(middleX + radiusX * Math.cos(angle));
            final int nextZ = (int) Math.round(middleZ + radiusZ * Math.sin(angle));
            line(x, z, nextX, nextZ, drawColumn);
            x = nextX;
            z = nextZ;
        }
    }

    private static void centeredPolygon(final Bounds bounds, final int sides, final BiConsumer<Integer, Integer> drawColumn) {
        final int symmetry = sides == 8 ? 8 : sides == 6 ? 4 : 2;
        final BiConsumer<Integer, Integer> reflected = (x, z) -> {
            if (symmetry == 2 && z >= 0) {
                drawColumn.accept(x, z);
                drawColumn.accept(x, -z);
            } else if (symmetry == 4 && x >= 0 && z >= 0 || symmetry == 8 && x >= z) {
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sz = -1; sz <= 1; sz += 2) {
                        drawColumn.accept(sx * x, sz * z);
                        if (symmetry == 8) drawColumn.accept(sx * z, sz * x);
                    }
                }
            }
        };
        int x = bounds.posX();
        int z = 0;
        for (int side = 1; side <= sides / symmetry; side++) {
            final double angle = 2 * Math.PI * side / sides;
            final int nextX = (int) Math.round(bounds.posX() * Math.cos(angle));
            final int nextZ = (int) Math.round(bounds.posZ() * Math.sin(angle));
            line(x, z, nextX, nextZ, reflected);
            x = nextX;
            z = nextZ;
        }
        line(x, z, symmetry == 8 ? z : symmetry == 4 ? -x : x, symmetry == 8 ? x : symmetry == 4 ? z : -z, reflected);
    }

    private static void line(int x, int z, final int endX, final int endZ, final BiConsumer<Integer, Integer> point) {
        final int dx = Math.abs(endX - x);
        final int dz = -Math.abs(endZ - z);
        final int stepX = Integer.compare(endX, x);
        final int stepZ = Integer.compare(endZ, z);
        int error = dx + dz;
        while (true) {
            point.accept(x, z);
            if (x == endX && z == endZ) return;
            final int doubled = error * 2;
            if (doubled >= dz) {
                error += dz;
                x += stepX;
            }
            if (doubled <= dx) {
                error += dx;
                z += stepZ;
            }
        }
    }

    private static void column(final Set<BlockPos> out, final Bounds bounds, final int x, final int z) {
        for (int y = -bounds.negY(); y <= bounds.posY(); y++) out.add(new BlockPos(x, y, z));
    }
}
