package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeLoadingGameTests {

    @GameTest(template = "empty")
    public static void shippedRecipesLoadAndNormalRecipesCraft(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var converter = FileToIdConverter.json("recipes");
        final var files = level.getServer().getResourceManager().listResources("recipes",
                file -> file.getNamespace().equals(OpenBlocks.MODID) && file.getPath().endsWith(".json"));
        helper.assertTrue(!files.isEmpty(), "Expected the OpenBlocks recipe data pack");

        for (final var file : files.keySet()) {
            final var id = converter.fileToId(file);
            final var recipe = level.getRecipeManager().byKey(id).orElse(null);
            helper.assertTrue(recipe != null, "Forge did not load shipped recipe " + id);
            if (!(recipe instanceof CraftingRecipe crafting) || crafting.isSpecial()) continue;

            final var grid = ComponentRecipeGameTests.input(helper);
            final int width = crafting instanceof ShapedRecipe shaped ? shaped.getWidth() : grid.getWidth();
            final var ingredients = crafting.getIngredients();
            for (int i = 0; i < ingredients.size(); i++) {
                final var ingredient = ingredients.get(i);
                if (ingredient.isEmpty()) continue;
                final ItemStack[] choices = ingredient.getItems();
                helper.assertTrue(choices.length > 0 && !choices[0].isEmpty()
                        && !(choices[0].is(Items.BARRIER) && choices[0].hasCustomHoverName()), id + " has an empty ingredient");
                grid.setItem(i / width * grid.getWidth() + i % width, choices[0].copy());
            }
            final ItemStack expected = crafting.getResultItem(level.registryAccess());
            final ItemStack result = ComponentRecipeGameTests.craft(helper, grid);
            helper.assertTrue(!result.isEmpty() && result.getCount() == expected.getCount()
                    && ItemStack.isSameItemSameTags(result, expected), id + " did not craft its advertised result");
        }
        helper.succeed();
    }
}
