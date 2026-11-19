package art.arcane.openblocks.integration;

import art.arcane.openblocks.block.OBFoldingTargetBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class OBFlanTargetHits {
    // Flan's Mod: Reloaded's 1.20.1 hitscan range. The reported point can be slightly rounded in transit.
    private static final double RANGE = 500.01;

    private OBFlanTargetHits() {}

    public static void recordHitscan(final ServerLevel level, final BlockHitResult reported, final Vec3 origin, final Entity shooter) {
        final var pos = reported.getBlockPos();
        if (reported.getType() != HitResult.Type.BLOCK || !level.hasChunkAt(pos)
                || !(level.getBlockState(pos).getBlock() instanceof OBFoldingTargetBlock)) return;
        final Vec3 offset = reported.getLocation().subtract(origin);
        final double distance = offset.length();
        if (!(distance > 0 && distance <= RANGE)) return;

        // Recheck the current server collision shape without loading chunks along the ray.
        final Vec3 end = reported.getLocation().add(offset.scale(0.01 / distance));
        final var context = new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter);
        final BlockHitResult actual = BlockGetter.traverseBlocks(origin, end, context, (ray, cursor) -> {
            final var chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(cursor.getX()),
                    SectionPos.blockToSectionCoord(cursor.getZ()));
            if (chunk == null) return BlockHitResult.miss(end, Direction.UP, cursor.immutable());
            return ray.getBlockShape(chunk.getBlockState(cursor), level, cursor).clip(origin, end, cursor);
        }, ray -> null);
        if (actual != null && actual.getBlockPos().equals(pos)) OBFoldingTargetBlock.recordOtherProjectile(level, actual);
    }
}
