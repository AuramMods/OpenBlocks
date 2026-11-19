package art.arcane.openblocks.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** A block that accepts paint directly from an OpenBlocks brush. */
public interface OBPaintableBlock {

    /** Applies a 24-bit RGB color to the requested faces and reports whether paint was used. */
    boolean applyPaint(Level level, BlockPos pos, int rgb, Direction... faces);
}
