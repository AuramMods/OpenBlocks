package art.arcane.openblocks.world;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class OBSpongeDrain {

    private OBSpongeDrain() {}

    public static Result drain(final ServerLevel level, final BlockPos origin, final int radius,
            final boolean updateNeighbors, @Nullable final Player player) {
        final int flags = updateNeighbors ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        boolean absorbed = false;
        boolean lava = false;

        for (final BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) continue;
            final BlockState state = level.getBlockState(pos);
            final var fluid = state.getFluidState();
            if (fluid.isEmpty() || (player != null && !level.mayInteract(player, pos))) continue;

            final BlockState replacement;
            if (state.getBlock() instanceof LiquidBlock) {
                replacement = Blocks.AIR.defaultBlockState();
            } else if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
                replacement = state.setValue(BlockStateProperties.WATERLOGGED, false);
            } else {
                continue;
            }
            if (level.setBlock(pos, replacement, flags)) {
                absorbed = true;
                lava |= fluid.is(FluidTags.LAVA);
            }
        }
        return new Result(absorbed, lava);
    }

    public record Result(boolean absorbed, boolean lava) {}
}
