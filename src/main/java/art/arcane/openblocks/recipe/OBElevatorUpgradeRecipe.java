package art.arcane.openblocks.recipe;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.item.OBElevatorItem;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class OBElevatorUpgradeRecipe extends CustomRecipe {

    private static final TagKey<Item> IRON = TagKey.create(Registries.ITEM, OpenBlocks.id("legacy_ore_dict/ingot_iron"));

    public OBElevatorUpgradeRecipe(final ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(final CraftingContainer input, final Level level) {
        return !findElevator(input).isEmpty();
    }

    @Override
    public ItemStack assemble(final CraftingContainer input, final RegistryAccess registries) {
        final ItemStack elevator = findElevator(input);
        return elevator.isEmpty() ? ItemStack.EMPTY
                : OBElevatorItem.create(OBItems.ELEVATOR_ROTATING.get(), OBElevatorItem.getColor(elevator));
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OBRecipeSerializers.ELEVATOR_UPGRADE.get();
    }

    private static ItemStack findElevator(final CraftingContainer input) {
        ItemStack elevator = ItemStack.EMPTY;
        int ironSlots = 0;
        for (final ItemStack stack : input.getItems()) {
            if (stack.isEmpty()) continue;
            if (stack.is(OBItems.ELEVATOR.get()) && elevator.isEmpty()) {
                elevator = stack;
            } else if (stack.is(IRON)) {
                ironSlots++;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return ironSlots == 2 ? elevator : ItemStack.EMPTY;
    }
}
