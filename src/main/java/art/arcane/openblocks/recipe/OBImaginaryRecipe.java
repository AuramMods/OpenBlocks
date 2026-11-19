package art.arcane.openblocks.recipe;

import art.arcane.openblocks.item.OBImaginaryItem;
import art.arcane.openblocks.item.OBImaginaryItem.Mode;
import art.arcane.openblocks.item.OBImaginationGlasses;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class OBImaginaryRecipe extends CustomRecipe {

    public enum Kind { MERGE, MIX, PENCIL_GLASSES, CRAYON_GLASSES }

    private final Kind kind;

    public OBImaginaryRecipe(final ResourceLocation id, final CraftingBookCategory category, final Kind kind) {
        super(id, category);
        this.kind = kind;
    }

    @Override
    public boolean matches(final CraftingContainer grid, final Level level) {
        return !assemble(grid, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(final CraftingContainer grid, final RegistryAccess registries) {
        return switch (kind) {
            case MERGE -> merge(grid);
            case MIX -> mix(grid);
            case PENCIL_GLASSES, CRAYON_GLASSES -> glasses(grid);
        };
    }

    private static ItemStack merge(final CraftingContainer grid) {
        int count = 0;
        float uses = 0;
        Integer color = null;
        for (final ItemStack stack : grid.getItems()) {
            if (stack.isEmpty()) continue;
            if (!stack.is(OBItems.IMAGINARY.get())) return ItemStack.EMPTY;
            final Integer nextColor = OBImaginaryItem.color(stack);
            if (count != 0 && !Objects.equals(color, nextColor)) return ItemStack.EMPTY;
            color = nextColor;
            count++;
            uses += OBImaginaryItem.uses(stack);
        }
        return count >= 2 && Float.isFinite(uses) && uses > 0
                ? OBImaginaryItem.create(color, Mode.BLOCK, uses) : ItemStack.EMPTY;
    }

    private static ItemStack mix(final CraftingContainer grid) {
        int count = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        boolean different = false;
        int first = 0;
        final var modes = new EnumMap<Mode, Integer>(Mode.class);
        for (final ItemStack stack : grid.getItems()) {
            if (stack.isEmpty()) continue;
            if (!stack.is(OBItems.IMAGINARY.get())) return ItemStack.EMPTY;
            final Integer color = OBImaginaryItem.color(stack);
            if (color == null) return ItemStack.EMPTY;
            if (OBImaginaryItem.uses(stack) < OBImaginaryItem.CRAFTING_COST) continue;
            if (count == 0) first = color;
            else if (color != first) different = true;
            count++;
            red += color >> 16;
            green += (color >> 8) & 255;
            blue += color & 255;
            modes.merge(OBImaginaryItem.mode(stack), 1, Integer::sum);
        }
        if (!different) return ItemStack.EMPTY;

        final int rgb = (red / count << 16) | (green / count << 8) | (blue / count);
        final var mode = modes.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        return OBImaginaryItem.create(rgb, mode, count * 0.9F);
    }

    private ItemStack glasses(final CraftingContainer grid) {
        ItemStack tool = ItemStack.EMPTY;
        boolean paper = false;
        for (final ItemStack stack : grid.getItems()) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.PAPER) && !paper) paper = true;
            else if (stack.is(OBItems.IMAGINARY.get()) && tool.isEmpty()) tool = stack;
            else return ItemStack.EMPTY;
        }
        if (!paper || tool.isEmpty()) return ItemStack.EMPTY;
        final var color = OBImaginaryItem.color(tool);
        if (kind == Kind.CRAYON_GLASSES) return color == null ? ItemStack.EMPTY : OBImaginationGlasses.createCrayonGlasses(color);
        return color == null ? new ItemStack(OBItems.PENCIL_GLASSES.get()) : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingContainer grid) {
        if (kind == Kind.MERGE) return NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
        return super.getRemainingItems(grid);
    }

    @Override public boolean canCraftInDimensions(final int width, final int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return switch (kind) {
            case MERGE -> OBRecipeSerializers.CRAYON_MERGE.get();
            case MIX -> OBRecipeSerializers.CRAYON_MIX.get();
            case PENCIL_GLASSES -> OBRecipeSerializers.PENCIL_GLASSES.get();
            case CRAYON_GLASSES -> OBRecipeSerializers.CRAYON_GLASSES.get();
        };
    }
}
