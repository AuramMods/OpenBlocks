package art.arcane.openblocks.recipe;

import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class OBGoldenEyeRechargeRecipe extends CustomRecipe {

    private static final int CHARGES_PER_PEARL = 10;

    public OBGoldenEyeRechargeRecipe(final ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(final CraftingContainer grid, final Level level) {
        return !assemble(grid, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(final CraftingContainer grid, final RegistryAccess access) {
        ItemStack eye = ItemStack.EMPTY;
        int recharge = 0;
        for (final ItemStack stack : grid.getItems()) {
            if (stack.isEmpty()) continue;
            if (stack.is(OBItems.GOLDEN_EYE.get()) && eye.isEmpty()) eye = stack;
            else if (stack.getItem() instanceof EnderpearlItem) recharge += CHARGES_PER_PEARL;
            else return ItemStack.EMPTY;
        }
        if (eye.isEmpty() || recharge == 0 || recharge > eye.getDamageValue()) return ItemStack.EMPTY;

        final ItemStack result = eye.copyWithCount(1);
        result.setDamageValue(eye.getDamageValue() - recharge);
        return result;
    }

    @Override public boolean canCraftInDimensions(final int width, final int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return OBRecipeSerializers.GOLDEN_EYE_RECHARGE.get(); }
}
