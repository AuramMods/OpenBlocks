package art.arcane.openblocks.recipe;

import art.arcane.openblocks.item.OBEmptyMapItem;
import art.arcane.openblocks.item.OBHeightMapItem;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import art.arcane.openblocks.world.OBHeightMapData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class OBMapCloneRecipe extends CustomRecipe {

    public OBMapCloneRecipe(final ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(final CraftingContainer grid, final Level level) {
        final ItemStack map = findMap(grid);
        final int id = OBHeightMapItem.mapId(map);
        if (id < 0) return false;
        int scale = OBHeightMapItem.knownScale(map);
        if (level instanceof ServerLevel server) {
            final OBHeightMapData data = OBHeightMapData.get(server, id);
            if (data == null) return false;
            scale = data.scale();
        }
        for (final ItemStack stack : grid.getItems()) {
            if (stack.is(OBItems.EMPTY_MAP.get())) return scale == OBEmptyMapItem.scale(stack);
        }
        return false;
    }

    @Override
    public ItemStack assemble(final CraftingContainer grid, final RegistryAccess registries) {
        final ItemStack map = findMap(grid);
        return map.isEmpty() ? ItemStack.EMPTY : map.copyWithCount(2);
    }

    private static ItemStack findMap(final CraftingContainer grid) {
        ItemStack map = ItemStack.EMPTY;
        boolean blank = false;
        for (final ItemStack stack : grid.getItems()) {
            if (stack.isEmpty()) continue;
            if (stack.is(OBItems.HEIGHT_MAP.get()) && map.isEmpty()) map = stack;
            else if (stack.is(OBItems.EMPTY_MAP.get()) && !blank) blank = true;
            else return ItemStack.EMPTY;
        }
        return blank ? map : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OBRecipeSerializers.MAP_CLONE.get();
    }
}
