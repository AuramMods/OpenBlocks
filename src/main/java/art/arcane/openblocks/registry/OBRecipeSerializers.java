package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.recipe.OBFlimFlamBookRecipe;
import art.arcane.openblocks.recipe.OBNoopSpecialRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, OpenBlocks.MODID);

    public static final RegistryObject<RecipeSerializer<?>> CRAYON_MERGE = register("crayon_merge");
    public static final RegistryObject<RecipeSerializer<?>> CRAYON_MIX = register("crayon_mix");
    public static final RegistryObject<RecipeSerializer<?>> CRAYON_GLASSES = register("crayon_glasses");
    public static final RegistryObject<RecipeSerializer<?>> MAP_CLONE = register("map_clone");
    public static final RegistryObject<RecipeSerializer<?>> MAP_RESIZE = register("map_resize");
    public static final RegistryObject<RecipeSerializer<?>> GOLDEN_EYE_RECHARGE = register("golden_eye_recharge");
    public static final RegistryObject<RecipeSerializer<?>> EPIC_ERASER_ACTION = register("epic_eraser_action");
    public static final RegistryObject<RecipeSerializer<?>> FLIM_FLAM_BOOK = RECIPE_SERIALIZERS.register("flim_flam_book",
            () -> new SimpleCraftingRecipeSerializer<>(OBFlimFlamBookRecipe::new));

    private OBRecipeSerializers() {}

    private static RegistryObject<RecipeSerializer<?>> register(final String id) {
        return RECIPE_SERIALIZERS.register(id, () -> new SimpleCraftingRecipeSerializer<>(OBNoopSpecialRecipe::new));
    }
}
