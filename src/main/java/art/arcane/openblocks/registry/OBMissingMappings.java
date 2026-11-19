package art.arcane.openblocks.registry;

import static java.util.Map.entry;

import art.arcane.openblocks.OpenBlocks;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryManager;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBMissingMappings {

    private static final Set<String> BLOCK_ENTITY_HEADER = Set.of("id", "x", "y", "z", "keepPacked");

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

    private static final Map<String, String> ITEM_ALIASES = withBlockItemAliases(Map.ofEntries(
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

    @Nullable
    public static ResourceLocation legacyItemDestination(final String id) {
        final int separator = id.indexOf(':');
        if (separator < 0 || !"openblocks".equalsIgnoreCase(id.substring(0, separator))) return null;
        return destination(id.substring(0, separator), id.substring(separator + 1), ITEM_ALIASES);
    }

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
        remap(event, Registries.VILLAGER_PROFESSION, Map.of());
    }

    @SubscribeEvent
    public static void onChunkDataLoad(final ChunkDataEvent.Load event) {
        // Forge does not save the block entity registry in 1.20.1. Fix the deferred NBT before vanilla reads it.
        final var entries = event.getData().getList("block_entities", Tag.TAG_COMPOUND);
        for (int i = entries.size() - 1; i >= 0; i--) {
            final var entry = entries.getCompound(i);
            final String id = entry.getString("id");
            // The 1.20.1 scaffold never attached this type. Rotating elevators read their data from the block state.
            if (event.getStatus() == ChunkStatus.ChunkType.LEVELCHUNK
                    && ("open_blocks:elevator_rotating".equals(id) || "openblocks:elevator_rotating".equalsIgnoreCase(id))
                    && BLOCK_ENTITY_HEADER.containsAll(entry.getAllKeys())) {
                entries.remove(i);
                event.getChunk().setUnsaved(true);
                continue;
            }
            final int separator = id.indexOf(':');
            if (separator < 0) continue;
            final ResourceLocation current = destination(id.substring(0, separator), id.substring(separator + 1), BLOCK_ALIASES);
            if (current != null && BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(current)) {
                entry.putString("id", current.toString());
                event.getChunk().setUnsaved(true);
            }
        }
    }

    private static <T> void remap(final MissingMappingsEvent event,
            final ResourceKey<? extends Registry<T>> registryKey,
            final Map<String, String> aliases) {
        if (event.getKey() != registryKey) return;
        // The mapping's registry is still being reconstructed from the save. Its replacement may not be present yet.
        final var registered = RegistryManager.ACTIVE.getRegistry(registryKey);
        for (final var mapping : event.getAllMappings(registryKey)) {
            final ResourceLocation id = mapping.getKey();
            final ResourceLocation current = destination(id.getNamespace(), id.getPath(), aliases);
            if (current == null) continue;
            final T target = registered.getRaw(current);
            if (target != null) mapping.remap(target);
        }
    }

    @Nullable
    private static ResourceLocation destination(final String namespace, final String path, final Map<String, String> aliases) {
        final boolean legacyNamespace = "openblocks".equalsIgnoreCase(namespace);
        if (!legacyNamespace && !OpenBlocks.MODID.equals(namespace)) return null;
        final String newPath = aliases.getOrDefault(path.toLowerCase(Locale.ROOT), path);
        return legacyNamespace || !path.equals(newPath) ? ResourceLocation.tryBuild(OpenBlocks.MODID, newPath) : null;
    }

    private static Map<String, String> withBlockItemAliases(final Map<String, String> standaloneItems) {
        final var aliases = new LinkedHashMap<>(BLOCK_ALIASES);
        aliases.putAll(withLowercaseAliases(standaloneItems));
        return Map.copyOf(aliases);
    }

    private static Map<String, String> withLowercaseAliases(final Map<String, String> aliases) {
        final Map<String, String> expanded = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            expanded.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return Map.copyOf(expanded);
    }
}
