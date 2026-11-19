package art.arcane.openblocks.recipe;

import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;

public final class OBEpicEraserRecipe extends CustomRecipe {

    private static final String LORE = "Lore";
    private static final String LEGACY_LORE = "SillyLore";

    public OBEpicEraserRecipe(final ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    @Override public boolean matches(final CraftingContainer grid, final Level level) { return !loreTarget(grid).isEmpty(); }

    @Override
    public ItemStack assemble(final CraftingContainer grid, final RegistryAccess access) {
        final ItemStack target = loreTarget(grid);
        if (target.isEmpty()) return ItemStack.EMPTY;
        final ItemStack result = target.copyWithCount(1);
        final var display = result.getTagElement("display");
        display.remove(LORE);
        display.remove(LEGACY_LORE);
        if (display.isEmpty()) result.removeTagKey("display");
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingContainer grid) {
        final var remaining = NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
        // The other input returns as the result, including its container if it has one.
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            final ItemStack input = grid.getItem(slot);
            if (input.is(OBItems.EPIC_ERASER.get())) remaining.set(slot, ForgeHooks.getCraftingRemainingItem(input));
        }
        return remaining;
    }

    private static ItemStack loreTarget(final CraftingContainer grid) {
        ItemStack target = ItemStack.EMPTY;
        boolean eraserFound = false;
        for (final ItemStack stack : grid.getItems()) {
            if (stack.isEmpty()) continue;
            if (stack.is(OBItems.EPIC_ERASER.get())) {
                if (eraserFound || stack.getDamageValue() >= stack.getMaxDamage()) return ItemStack.EMPTY;
                eraserFound = true;
            } else {
                final var display = stack.getTagElement("display");
                if (!target.isEmpty() || display == null || !(display.contains(LORE, Tag.TAG_LIST) || display.contains(LEGACY_LORE, Tag.TAG_LIST))) {
                    return ItemStack.EMPTY;
                }
                target = stack;
            }
        }
        return eraserFound ? target : ItemStack.EMPTY;
    }

    @Override public boolean canCraftInDimensions(final int width, final int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return OBRecipeSerializers.EPIC_ERASER_ACTION.get(); }
}
