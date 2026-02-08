package art.arcane.openblocks.recipe;

import java.util.Objects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class OBNoopSpecialRecipe extends CustomRecipe {

    public OBNoopSpecialRecipe(final net.minecraft.resources.ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(final CraftingContainer container, final Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(final CraftingContainer container, final RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(final RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Objects.requireNonNull(ForgeRegistries.RECIPE_SERIALIZERS.getValue(getId()),
                "Missing recipe serializer registration for " + getId());
    }
}
