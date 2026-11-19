package art.arcane.openblocks.guide;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum OBGuideOrientation implements StringRepresentable {
    XP_YP(Direction.EAST, Direction.UP), XN_YP(Direction.WEST, Direction.UP),
    ZP_YP(Direction.SOUTH, Direction.UP), ZN_YP(Direction.NORTH, Direction.UP),
    YP_XN(Direction.UP, Direction.WEST), YN_XN(Direction.DOWN, Direction.WEST),
    ZP_XN(Direction.SOUTH, Direction.WEST), ZN_XN(Direction.NORTH, Direction.WEST),
    XP_ZN(Direction.EAST, Direction.NORTH), XN_ZN(Direction.WEST, Direction.NORTH),
    YP_ZN(Direction.UP, Direction.NORTH), YN_ZN(Direction.DOWN, Direction.NORTH);

    private final String name = name().toLowerCase(Locale.ROOT);
    private final Direction x;
    private final Direction y;
    private final Direction z;

    OBGuideOrientation(final Direction x, final Direction y) {
        this.x = x;
        this.y = y;
        final Vec3 cross = axis(x).cross(axis(y));
        this.z = Direction.getNearest(cross.x, cross.y, cross.z);
    }

    public Direction x() { return x; }
    public Direction y() { return y; }
    public Direction z() { return z; }

    @Override public String getSerializedName() { return name; }

    public static OBGuideOrientation forSurface(final Direction face) {
        return switch (face.getAxis()) {
            case X -> ZN_XN;
            case Y -> XP_YP;
            case Z -> XP_ZN;
        };
    }

    public OBGuideOrientation quarterTurn(final boolean clockwise) {
        return find(clockwise ? z : z.getOpposite(), y);
    }

    public OBGuideOrientation rotate(final Rotation rotation) {
        final Direction rotatedX = rotation.rotate(x);
        final Direction rotatedY = rotation.rotate(y);
        if (rotatedY.getAxis() == Direction.Axis.Y) return find(rotatedX, rotatedY);
        // The housing has symmetric caps. Normalize wall mounts to the original twelve orientations.
        return find(rotatedX, rotatedY.getAxis() == Direction.Axis.X ? Direction.WEST : Direction.NORTH);
    }

    public BlockPos offset(final BlockPos local) {
        return new BlockPos(
                local.getX() * x.getStepX() + local.getY() * y.getStepX() + local.getZ() * z.getStepX(),
                local.getX() * x.getStepY() + local.getY() * y.getStepY() + local.getZ() * z.getStepY(),
                local.getX() * x.getStepZ() + local.getY() * y.getStepZ() + local.getZ() * z.getStepZ());
    }

    public Vec3 toLocal(final Vec3 world) {
        final Vec3 centered = world.subtract(0.5, 0.5, 0.5);
        return new Vec3(centered.dot(axis(x)) + 0.5, centered.dot(axis(y)) + 0.5, centered.dot(axis(z)) + 0.5);
    }

    public AABB toWorld(final AABB box) {
        return new AABB(toWorld(new Vec3(box.minX, box.minY, box.minZ)), toWorld(new Vec3(box.maxX, box.maxY, box.maxZ)));
    }

    private Vec3 toWorld(final Vec3 local) {
        return axis(x).scale(local.x - 0.5).add(axis(y).scale(local.y - 0.5)).add(axis(z).scale(local.z - 0.5)).add(0.5, 0.5, 0.5);
    }

    private static Vec3 axis(final Direction direction) {
        return Vec3.atLowerCornerOf(direction.getNormal());
    }

    private static OBGuideOrientation find(final Direction x, final Direction y) {
        for (final OBGuideOrientation orientation : values()) {
            if (orientation.x == x && orientation.y == y) return orientation;
        }
        throw new IllegalArgumentException("Unsupported guide orientation " + x + " / " + y);
    }
}
