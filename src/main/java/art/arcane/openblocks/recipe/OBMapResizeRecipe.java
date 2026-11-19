package art.arcane.openblocks.recipe;

import art.arcane.openblocks.item.OBEmptyMapItem;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import art.arcane.openblocks.world.OBHeightMapData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class OBMapResizeRecipe extends CustomRecipe {

    public OBMapResizeRecipe(final ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(final CraftingContainer grid, final Level level) {
        return !findBlank(grid).isEmpty();
    }

    @Override
    public ItemStack assemble(final CraftingContainer grid, final RegistryAccess registries) {
        final ItemStack map = findBlank(grid);
        if (map.isEmpty()) return ItemStack.EMPTY;
        final ItemStack output = map.copyWithCount(1);
        OBEmptyMapItem.setScale(output, OBEmptyMapItem.scale(map) + 1);
        return output;
    }

    private static ItemStack findBlank(final CraftingContainer grid) {
        final int width = grid.getWidth();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            final ItemStack candidate = grid.getItem(i);
            if (!candidate.is(OBItems.EMPTY_MAP.get()) || OBEmptyMapItem.scale(candidate) >= OBHeightMapData.MAX_SCALE) continue;
            if (i % width == 0 || i % width == width - 1) return ItemStack.EMPTY;
            if (!grid.getItem(i - 1).is(OBItems.MAP_MEMORY.get()) || !grid.getItem(i + 1).is(OBItems.MAP_MEMORY.get())) return ItemStack.EMPTY;
            final boolean cross = i >= width && i + width < grid.getContainerSize()
                    && grid.getItem(i - width).is(OBItems.MAP_MEMORY.get()) && grid.getItem(i + width).is(OBItems.MAP_MEMORY.get());
            for (int j = 0; j < grid.getContainerSize(); j++) {
                if (j == i || j == i - 1 || j == i + 1 || cross && (j == i - width || j == i + width)) continue;
                if (!grid.getItem(j).isEmpty()) return ItemStack.EMPTY;
            }
            return candidate;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OBRecipeSerializers.MAP_RESIZE.get();
    }
}
