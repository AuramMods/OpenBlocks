package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class ComponentRecipeGameTests {

    @GameTest(template = "empty")
    public static void wingsCraftAndOnlyWingsMakeGliders(GameTestHelper helper) {
        final CraftingContainer wings = input(helper,
                Items.AIR, Items.STICK, Items.LEATHER,
                Items.STICK, Items.LEATHER, Items.LEATHER,
                Items.LEATHER, Items.LEATHER, Items.LEATHER);
        helper.assertTrue(craft(helper, wings).is(OBItems.GLIDER_WING.get()), "Recipe did not produce a wing");

        final CraftingContainer glider = input(helper, OBItems.GLIDER_WING.get(), Items.STICK, OBItems.GLIDER_WING.get());
        helper.assertTrue(craft(helper, glider).is(OBItems.HANG_GLIDER.get()), "Two wings and a stick did not produce a glider");
        glider.setItem(2, new ItemStack(OBItems.BEAM.get()));
        helper.assertTrue(craft(helper, glider).isEmpty(), "A beam substituted for a wing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mapsRequireControllerAndMemory(GameTestHelper helper) {
        final var memory = OBItems.MAP_MEMORY.get();
        final CraftingContainer map = input(helper,
                Items.AIR, memory, Items.AIR,
                memory, OBItems.MAP_CONTROLLER.get(), memory,
                Items.AIR, memory, Items.AIR);
        helper.assertTrue(craft(helper, map).is(OBItems.EMPTY_MAP.get()), "Recipe did not produce an empty map");
        map.setItem(4, new ItemStack(memory));
        helper.assertTrue(craft(helper, map).isEmpty(), "Map memory substituted for the controller");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyPointerCanBeUpgraded(GameTestHelper helper) {
        final ItemStack pointer = craft(helper, input(helper, OBItems.GENERIC_UNSTACKABLE.get()));
        helper.assertTrue(pointer.is(OBItems.POINTER.get()) && pointer.getMaxStackSize() == 1,
                "Legacy pointer did not convert to a single, unstackable pointer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sketchingPencilsAcceptCoalAndCharcoal(GameTestHelper helper) {
        final CraftingContainer pencil = input(helper,
                Items.COAL, Items.AIR, Items.AIR,
                Items.AIR, Items.STICK, Items.AIR,
                Items.AIR, Items.AIR, Items.STICK);
        helper.assertTrue(craft(helper, pencil).is(OBItems.SKETCHING_PENCIL.get()), "Coal did not craft a sketching pencil");
        pencil.setItem(0, new ItemStack(Items.CHARCOAL));
        helper.assertTrue(craft(helper, pencil).is(OBItems.SKETCHING_PENCIL.get()), "Charcoal did not craft a sketching pencil");
        helper.succeed();
    }

    static CraftingContainer input(GameTestHelper helper, ItemLike... slots) {
        final var menu = new CraftingMenu(0, helper.makeMockSurvivalPlayer().getInventory());
        final var input = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < slots.length; i++) input.setItem(i, new ItemStack(slots[i]));
        return input;
    }

    static ItemStack craft(GameTestHelper helper, CraftingContainer input) {
        final var level = helper.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(recipe -> recipe.assemble(input, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
