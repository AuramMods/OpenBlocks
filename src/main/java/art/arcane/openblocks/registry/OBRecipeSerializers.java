package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.recipe.OBElevatorUpgradeRecipe;
import art.arcane.openblocks.recipe.OBFlimFlamBookRecipe;
import art.arcane.openblocks.recipe.OBGoldenEyeRechargeRecipe;
import art.arcane.openblocks.recipe.OBImaginaryRecipe;
import art.arcane.openblocks.recipe.OBMapCloneRecipe;
import art.arcane.openblocks.recipe.OBMapResizeRecipe;
import art.arcane.openblocks.recipe.OBNoopSpecialRecipe;
import art.arcane.openblocks.recipe.OBSkyInversionRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, OpenBlocks.MODID);

    public static final RegistryObject<RecipeSerializer<?>> CRAYON_MERGE = registerImaginary("crayon_merge", OBImaginaryRecipe.Kind.MERGE);
    public static final RegistryObject<RecipeSerializer<?>> CRAYON_MIX = registerImaginary("crayon_mix", OBImaginaryRecipe.Kind.MIX);
    public static final RegistryObject<RecipeSerializer<?>> CRAYON_GLASSES = registerImaginary("crayon_glasses", OBImaginaryRecipe.Kind.CRAYON_GLASSES);
    public static final RegistryObject<RecipeSerializer<?>> MAP_CLONE = RECIPE_SERIALIZERS.register("map_clone",
            () -> new SimpleCraftingRecipeSerializer<>(OBMapCloneRecipe::new));
    public static final RegistryObject<RecipeSerializer<?>> MAP_RESIZE = RECIPE_SERIALIZERS.register("map_resize",
            () -> new SimpleCraftingRecipeSerializer<>(OBMapResizeRecipe::new));
    public static final RegistryObject<RecipeSerializer<?>> GOLDEN_EYE_RECHARGE = RECIPE_SERIALIZERS.register("golden_eye_recharge",
            () -> new SimpleCraftingRecipeSerializer<>(OBGoldenEyeRechargeRecipe::new));
    public static final RegistryObject<RecipeSerializer<?>> EPIC_ERASER_ACTION = register("epic_eraser_action");
    public static final RegistryObject<RecipeSerializer<?>> FLIM_FLAM_BOOK = RECIPE_SERIALIZERS.register("flim_flam_book",
            () -> new SimpleCraftingRecipeSerializer<>(OBFlimFlamBookRecipe::new));
    public static final RegistryObject<RecipeSerializer<?>> ELEVATOR_UPGRADE = RECIPE_SERIALIZERS.register("elevator_upgrade",
            () -> new SimpleCraftingRecipeSerializer<>(OBElevatorUpgradeRecipe::new));
    public static final RegistryObject<RecipeSerializer<?>> SKY_INVERSION = RECIPE_SERIALIZERS.register("sky_inversion",
            () -> new SimpleCraftingRecipeSerializer<>(OBSkyInversionRecipe::new));

    public static final RegistryObject<RecipeSerializer<?>> PENCIL_GLASSES =
            registerImaginary("pencil_glasses", OBImaginaryRecipe.Kind.PENCIL_GLASSES);

    private OBRecipeSerializers() {}

    private static RegistryObject<RecipeSerializer<?>> registerImaginary(final String id, final OBImaginaryRecipe.Kind kind) {
        return RECIPE_SERIALIZERS.register(id, () -> new SimpleCraftingRecipeSerializer<>(
                (name, category) -> new OBImaginaryRecipe(name, category, kind)));
    }

    private static RegistryObject<RecipeSerializer<?>> register(final String id) {
        return RECIPE_SERIALIZERS.register(id, () -> new SimpleCraftingRecipeSerializer<>(OBNoopSpecialRecipe::new));
    }
}
