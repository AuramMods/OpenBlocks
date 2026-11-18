package art.arcane.openblocks.recipe;

import art.arcane.openblocks.item.OBSkyItem;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class OBSkyInversionRecipe extends CustomRecipe {

    public OBSkyInversionRecipe(final ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(final CraftingContainer grid, final Level level) {
        return !getSkyWithTorch(grid).isEmpty();
    }

    @Override
    public ItemStack assemble(final CraftingContainer grid, final RegistryAccess registries) {
        final ItemStack sky = getSkyWithTorch(grid);
        return sky.isEmpty() ? ItemStack.EMPTY : OBSkyItem.create(!OBSkyItem.isInverted(sky));
    }

    private static ItemStack getSkyWithTorch(final CraftingContainer grid) {
        ItemStack sky = ItemStack.EMPTY;
        boolean torch = false;
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            final ItemStack stack = grid.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.is(OBItems.SKY.get()) && sky.isEmpty()) sky = stack;
            else if (stack.is(Items.REDSTONE_TORCH) && !torch) torch = true;
            else return ItemStack.EMPTY;
        }
        return torch ? sky : ItemStack.EMPTY;
    }

    @Override public boolean canCraftInDimensions(final int width, final int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return OBRecipeSerializers.SKY_INVERSION.get(); }
}
