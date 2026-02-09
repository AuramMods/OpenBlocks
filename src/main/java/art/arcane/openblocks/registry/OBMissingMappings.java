package art.arcane.openblocks.registry;

import static java.util.Map.entry;

import art.arcane.openblocks.OpenBlocks;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBMissingMappings {

    private static final List<String> LEGACY_MODIDS = List.of(
            "openblocks",
            "OpenBlocks");

    private static final Map<String, String> BLOCK_ALIASES = withLowercaseAliases(Map.ofEntries(
            entry("vacuumhopper", "vacuum_hopper"),
            entry("bigbutton", "big_button"),
            entry("xpbottler", "xp_bottler"),
            entry("autoanvil", "auto_anvil"),
            entry("autoenchantmenttable", "auto_enchantment_table"),
            entry("xpdrain", "xp_drain"),
            entry("blockbreaker", "block_breaker"),
            entry("blockPlacer", "block_placer"),
            entry("itemDropper", "item_dropper"),
            entry("ropeladder", "rope_ladder"),
            entry("donationStation", "donation_station"),
            entry("paintmixer", "paint_mixer"),
            entry("paintcan", "paint_can"),
            entry("canvasglass", "canvas_glass"),
            entry("drawingtable", "drawing_table"),
            entry("xpshower", "xp_shower"),
            entry("goldenegg", "golden_egg")));

    private static final Map<String, String> ITEM_ALIASES = withLowercaseAliases(Map.ofEntries(
            entry("hangglider", "hang_glider"),
            entry("sonicglasses", "sonic_glasses"),
            entry("pencilGlasses", "pencil_glasses"),
            entry("crayonGlasses", "crayon_glasses"),
            entry("technicolorGlasses", "technicolor_glasses"),
            entry("seriousGlasses", "serious_glasses"),
            entry("craneControl", "crane_control"),
            entry("craneBackpack", "crane_backpack"),
            entry("filledbucket", "xp_bucket"),
            entry("sleepingBag", "sleeping_bag"),
            entry("paintBrush", "paintbrush"),
            entry("heightMap", "height_map"),
            entry("emptyMap", "empty_map"),
            entry("tastyClay", "tasty_clay"),
            entry("goldenEye", "golden_eye"),
            entry("genericUnstackable", "generic_unstackable"),
            entry("infoBook", "info_book"),
            entry("devnull", "dev_null"),
            entry("spongeonastick", "sponge_on_a_stick"),
            entry("epicEraser", "epic_eraser")));

    private static final Map<String, String> FLUID_ALIASES = withLowercaseAliases(Map.ofEntries(
            entry("liquidxp", "xpjuice")));

    private static final Map<String, String> MENU_ALIASES = withLowercaseAliases(Map.ofEntries(
            entry("devnull", "dev_null"),
            entry("vacuumhopper", "vacuum_hopper"),
            entry("xpbottler", "xp_bottler"),
            entry("autoanvil", "auto_anvil"),
            entry("autoenchantmenttable", "auto_enchantment_table"),
            entry("blockPlacer", "block_placer"),
            entry("itemDropper", "item_dropper"),
            entry("donationStation", "donation_station"),
            entry("paintmixer", "paint_mixer"),
            entry("drawingtable", "drawing_table")));

    private OBMissingMappings() {}

    @SubscribeEvent
    public static void onMissingMappings(final MissingMappingsEvent event) {
        remap(event, Registries.BLOCK, BLOCK_ALIASES);
        remap(event, Registries.ITEM, ITEM_ALIASES);
        remap(event, Registries.FLUID, FLUID_ALIASES);
        remap(event, ForgeRegistries.Keys.FLUID_TYPES, FLUID_ALIASES);

        remap(event, Registries.BLOCK_ENTITY_TYPE, BLOCK_ALIASES);
        remap(event, Registries.ENTITY_TYPE, Map.of());
        remap(event, Registries.SOUND_EVENT, Map.of());
        remap(event, Registries.ENCHANTMENT, Map.of());
        remap(event, Registries.MENU, MENU_ALIASES);
        remap(event, Registries.RECIPE_SERIALIZER, Map.of());
    }

    private static <T> void remap(final MissingMappingsEvent event,
            final ResourceKey<? extends Registry<T>> registryKey,
            final Map<String, String> aliases) {
        for (String legacyModid : LEGACY_MODIDS) remapNamespace(event, registryKey, legacyModid, aliases, true);
        remapNamespace(event, registryKey, OpenBlocks.MODID, aliases, false);
    }

    private static <T> void remapNamespace(final MissingMappingsEvent event,
            final ResourceKey<? extends Registry<T>> registryKey,
            final String namespace,
            final Map<String, String> aliases,
            final boolean applyIdentity) {
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(registryKey, namespace)) {
            final String oldPath = mapping.getKey().getPath();
            final String newPath = aliases.getOrDefault(oldPath, oldPath);
            if (!applyIdentity && oldPath.equals(newPath)) continue;

            final ResourceLocation targetId = ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, newPath);
            final T target = mapping.getRegistry().getValue(targetId);
            if (target != null) mapping.remap(target);
        }
    }

    private static Map<String, String> withLowercaseAliases(final Map<String, String> aliases) {
        final Map<String, String> expanded = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            expanded.put(entry.getKey(), entry.getValue());
            expanded.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return Map.copyOf(expanded);
    }
}
