package art.arcane.openblocks.block;

import art.arcane.openblocks.config.OBServerConfig;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public final class OBLiquidExperienceBlock extends LiquidBlock {

    public OBLiquidExperienceBlock(final Supplier<? extends FlowingFluid> fluid, final Properties properties) {
        super(fluid, properties);
    }

    @Override
    public ItemStack pickupBlock(final LevelAccessor level, final BlockPos pos, final BlockState state) {
        if (OBServerConfig.SPEC.isLoaded() && !OBServerConfig.XP_BUCKET_DIRECT_FILL.get()) return ItemStack.EMPTY;
        return super.pickupBlock(level, pos, state);
    }
}
