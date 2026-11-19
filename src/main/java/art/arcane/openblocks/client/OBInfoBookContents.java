package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.item.OBImaginaryItem;
import art.arcane.openblocks.item.OBSkyItem;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

final class OBInfoBookContents {

    static final String KEY = "book.open_blocks.";
    private static final ResourceLocation CHANGELOG = OpenBlocks.id("changelog.json");
    private static final Set<String> BLOCK_VIDEOS = Set.of("builder_guide", "canvas", "drawing_table", "elevator", "guide", "ladder", "paint_mixer", "tank");
    private static final Set<String> ITEM_VIDEOS = Set.of("crane_backpack", "crane_control", "cursor", "hang_glider", "luggage", "paintbrush", "squeegee", "unprepared_stencil");

    record Entry(ItemStack stack, String description, String video, CraftingRecipe recipe) {
        Component title() {
            return stack.getHoverName();
        }
    }

    record Changelog(String version, List<ChangeSection> sections) {}
    record ChangeSection(String title, List<String> lines) {}

    private OBInfoBookContents() {}

    static List<Entry> entries(final Minecraft game, final boolean blocks) {
        final var recipes = game.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> !recipe.isSpecial() && !recipe.getIngredients().isEmpty())
                .sorted(Comparator.comparing(CraftingRecipe::getId)).toList();
        final List<Entry> entries = new ArrayList<>();
        if (blocks) {
            for (final var entry : OBBlocks.all()) {
                final String id = entry.getId().getPath();
                if (entry == OBBlocks.IMAGINARY) continue;
                final String mediaId = entry == OBBlocks.BIG_BUTTON_WOOD ? "big_button" : id;
                add(entries, game, recipes, new ItemStack(entry.get()), id, BLOCK_VIDEOS.contains(mediaId) ? "tile." + mediaId : null);
            }
            add(entries, game, recipes, OBSkyItem.create(true), "sky_inverted", null);
        } else {
            for (final var entry : OBItems.all()) {
                final var item = entry.get();
                if (item instanceof BlockItem) continue;
                final String id = entry.getId().getPath();
                add(entries, game, recipes, new ItemStack(item), id, ITEM_VIDEOS.contains(id) ? "item." + id : null);
            }
            final ItemStack pencil = OBImaginaryItem.create(null, OBImaginaryItem.Mode.BLOCK, OBImaginaryItem.defaultUses());
            final ItemStack crayon = OBImaginaryItem.create(0xB02E26, OBImaginaryItem.Mode.BLOCK, OBImaginaryItem.defaultUses());
            add(entries, game, recipes, pencil, "magic_pencil", null);
            add(entries, game, recipes, crayon, "magic_crayon", null);
        }
        entries.sort(Comparator.comparing(entry -> entry.title().getString(), String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    private static void add(final List<Entry> entries, final Minecraft game, final List<CraftingRecipe> recipes,
            final ItemStack item, final String id, final String videoId) {
        final String description = KEY + "entry." + id;
        if (item.isEmpty() || !I18n.exists(description)) return;
        final var matching = recipes.stream().filter(recipe -> recipe.getResultItem(game.level.registryAccess()).is(item.getItem())).toList();
        // Match the displayed variant before falling back to a conventional recipe for an untagged item.
        final CraftingRecipe recipe = matching.stream()
                .filter(candidate -> ItemStack.isSameItemSameTags(candidate.getResultItem(game.level.registryAccess()), item))
                .findFirst().orElseGet(() -> item.hasTag() ? null : matching.stream().findFirst().orElse(null));
        final String video = videoId == null ? null : "https://videos.openmods.info/" + game.options.languageCode
                + "/tutorial.openblocks." + videoId;
        entries.add(new Entry(item, description, video, recipe));
    }

    static int recipeWidth(final CraftingRecipe recipe) {
        return recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
    }

    static List<Changelog> changelogs(final Minecraft game) {
        try (final var reader = game.getResourceManager().openAsReader(CHANGELOG)) {
            final List<Changelog> changes = new Gson().fromJson(reader, new TypeToken<List<Changelog>>() {}.getType());
            if (changes == null) return List.of();
            return changes.stream().filter(change -> change != null && change.version() != null && change.sections() != null).toList();
        } catch (final IOException | JsonParseException error) {
            LogUtils.getLogger().warn("Could not read the OpenBlocks Information Book changelog", error);
            return List.of();
        }
    }

    static Component releaseNotes(final Changelog change) {
        final var notes = Component.empty();
        for (final ChangeSection section : change.sections()) {
            if (section == null || section.title() == null || section.lines() == null) continue;
            if (!notes.getSiblings().isEmpty()) notes.append("\n\n");
            notes.append(Component.translatable(section.title()).withStyle(ChatFormatting.BOLD));
            section.lines().stream().filter(Objects::nonNull)
                    .forEach(line -> notes.append("\n• ").append(line));
        }
        return notes;
    }
}
