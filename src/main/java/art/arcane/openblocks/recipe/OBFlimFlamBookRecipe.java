package art.arcane.openblocks.recipe;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBEnchantments;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

public class OBFlimFlamBookRecipe extends CustomRecipe {

    private static final TagKey<Item> EMERALDS = TagKey.create(Registries.ITEM, OpenBlocks.id("legacy_ore_dict/gem_emerald"));

    public OBFlimFlamBookRecipe(final ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(final CraftingContainer container, final Level level) {
        return OBEnchantments.FLIM_FLAM.get().isDiscoverable() && emeraldCost(container) > 0;
    }

    @Override
    public ItemStack assemble(final CraftingContainer container, final RegistryAccess registryAccess) {
        final int level = emeraldCost(container);
        if (level <= 0 || !OBEnchantments.FLIM_FLAM.get().isDiscoverable()) return ItemStack.EMPTY;

        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(OBEnchantments.FLIM_FLAM.get(), level));
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OBRecipeSerializers.FLIM_FLAM_BOOK.get();
    }

    private static int emeraldCost(final CraftingContainer container) {
        int books = 0;
        int emeralds = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            final ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(Items.BOOK)) {
                if (++books > 1) return 0;
                continue;
            }

            if (stack.is(EMERALDS)) {
                if (++emeralds > 4) return 0;
                continue;
            }

            return 0;
        }

        if (books != 1) return 0;
        return emeralds >= 1 && emeralds <= 4 ? emeralds : 0;
    }
}
