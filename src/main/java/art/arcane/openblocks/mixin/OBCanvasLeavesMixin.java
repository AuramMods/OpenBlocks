package art.arcane.openblocks.mixin;

import art.arcane.openblocks.block.OBCanvasBlock;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LeavesBlock.class)
public abstract class OBCanvasLeavesMixin {
    @Redirect(method = "updateDistance", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState openBlocks$readLeafSupport(final LevelAccessor level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof OBCanvasBlock && level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas) {
            final BlockState original = canvas.original();
            // A log is stable support. A wrapped leaf's saved distance can describe an old canopy.
            if (original.is(BlockTags.LOGS)) return original;
        }
        return state;
    }
}
