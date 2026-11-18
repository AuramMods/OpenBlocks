package art.arcane.openblocks.item;

import javax.annotation.Nullable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

public final class OBScaffoldingItem extends BlockItem {

    public OBScaffoldingItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public int getBurnTime(final ItemStack stack, @Nullable final RecipeType<?> recipeType) {
        return 100;
    }
}
