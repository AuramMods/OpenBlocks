package art.arcane.openblocks.client;

import static java.util.Map.entry;

import art.arcane.openblocks.OpenBlocks;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.SpawnEggItem;

public final class OBSoundIcons {

    private static final int WHITE = 0xFFFFFF;
    private static final int RED = 0xFF0000;
    private static final Layer FRAME = new Layer(openBlocks("frame"), WHITE, 1);
    private static final Icon UNKNOWN = single(openBlocks("unknown"), WHITE);
    private static final Icon GENERIC = single(openBlocks("generic"), WHITE);
    private static final Icon CLICK = single(openBlocks("click"), WHITE);

    private static final Map<String, String> SURFACES = Map.ofEntries(
            entry("anvil", "anvil"), entry("cloth", "white_wool"), entry("wool", "white_wool"), entry("glass", "glass"),
            entry("grass", "dirt"), entry("gravel", "gravel"), entry("ladder", "ladder"), entry("metal", "iron_block"),
            entry("sand", "sand"), entry("slime", "slime_block"), entry("slime_block", "slime_block"), entry("snow", "snow"),
            entry("stone", "stone"), entry("wood", "oak_log_top"));
    private static final Map<String, String> BLOCKS = Map.ofEntries(
            entry("brewing_stand", "block/brewing_stand"), entry("chest", "block/oak_planks"),
            entry("chorus_flower", "block/chorus_flower"), entry("comparator", "block/comparator_on"),
            entry("dispenser", "block/dispenser_front"), entry("enchantment_table", "block/enchanting_table_top"),
            entry("end_gateway", "item/end_crystal"), entry("ender_chest", "item/ender_eye"), entry("enderchest", "item/ender_eye"),
            entry("fence_gate", "block/oak_planks"), entry("fire", "block/fire_0"), entry("furnace", "block/furnace_front_on"),
            entry("iron_door", "item/iron_door"), entry("iron_trapdoor", "block/iron_trapdoor"), entry("lava", "block/lava_flow"),
            entry("piston", "block/piston_side"), entry("portal", "block/nether_portal"), entry("redstone_torch", "block/redstone_torch"),
            entry("tripwire", "block/tripwire"), entry("water", "block/water_flow"), entry("waterlily", "block/lily_pad"),
            entry("wooden_door", "item/oak_door"), entry("wooden_trapdoor", "block/oak_trapdoor"));
    private static final Map<String, String> SWITCHES = Map.of("metal_pressureplate", "iron_block", "metal_pressure_plate", "iron_block",
            "stone_pressureplate", "stone", "stone_pressure_plate", "stone", "wood_pressureplate", "oak_planks",
            "wooden_pressure_plate", "oak_planks", "lever", "lever");
    private static final Map<String, Integer> NOTES = Map.of("basedrum", 0x00FFFF, "bass", 0x0000FF, "harp", RED,
            "hat", 0x00FF00, "pling", 0xFF00FF, "snare", WHITE);
    private static final Map<String, String> ENTITY_ICONS = Map.ofEntries(
            entry("experience_orb", "item/experience_bottle"), entry("arrow", "item/arrow"), entry("egg", "item/experience_bottle"),
            entry("eye_of_ender", "item/ender_eye"), entry("ender_eye", "item/ender_eye"), entry("endereye", "item/ender_eye"),
            entry("ender_pearl", "item/ender_pearl"), entry("enderpearl", "item/ender_pearl"), entry("experience_bottle", "item/experience_bottle"),
            entry("firework_rocket", "item/firework_rocket"), entry("firework", "item/firework_rocket"), entry("item_frame", "item/item_frame"),
            entry("itemframe", "item/item_frame"), entry("leash_knot", "item/lead"), entry("leashknot", "item/lead"),
            entry("lingering_potion", "item/lingering_potion"), entry("lingeringpotion", "item/lingering_potion"), entry("minecart", "item/minecart"),
            entry("painting", "item/painting"), entry("snowball", "item/snowball"), entry("splash_potion", "item/splash_potion"),
            entry("tnt", "block/tnt_side"), entry("armor_stand", "item/armor_stand"), entry("armorstand", "item/armor_stand"),
            entry("fishing_bobber", "item/fishing_rod_cast"), entry("bobber", "item/fishing_rod_cast"), entry("evoker_fangs", "item/fishing_rod_cast"));
    private static final Map<String, String> MOB_ALIASES = Map.ofEntries(
            entry("elderguardian", "elder_guardian"), entry("enderdragon", "ender_dragon"), entry("endermen", "enderman"),
            entry("evocation_illager", "evoker"), entry("magmacube", "magma_cube"), entry("small_magmacube", "magma_cube"),
            entry("small_slime", "slime"), entry("vindication_illager", "vindicator"), entry("irongolem", "iron_golem"),
            entry("snowman", "snow_golem"), entry("zombie_pig", "zombified_piglin"));
    private static final Set<EntityType<?>> FRIENDLY_MISC = Set.of(EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.VILLAGER);
    private static final Map<String, String> ITEMS = Map.ofEntries(
            entry("bottle", "glass_bottle"), entry("bucket", "bucket"), entry("chorus_fruit", "chorus_fruit"), entry("elytra", "elytra"),
            entry("firecharge", "fire_charge"), entry("fire_charge", "fire_charge"), entry("flintandsteel", "flint_and_steel"),
            entry("flint_and_steel", "flint_and_steel"), entry("hoe", "wooden_hoe"), entry("shield", "empty_armor_slot_shield"),
            entry("shovel", "wooden_shovel"));
    private static final Map<String, String> ARMOR = Map.of("equip_chain", "chainmail_chestplate", "equip_diamond", "diamond_chestplate",
            "equip_gold", "golden_chestplate", "equip_iron", "iron_chestplate", "equip_leather", "leather_chestplate",
            "equip_netherite", "netherite_chestplate", "equip_turtle", "turtle_helmet", "equip_generic", "empty_armor_slot_chestplate");
    private static final Set<String> DISCS = Set.of("13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11",
            "wait", "pigstep", "otherside", "5", "relic");

    private OBSoundIcons() {}

    public static Icon forSound(final ResourceLocation sound) {
        final String[] path = sound.getPath().split("\\.", 3);
        final String group = path[0];
        final String subject = path.length > 1 ? path[1] : "";
        final String action = path.length > 2 ? path[2] : "";
        if (sound.getNamespace().equals(OpenBlocks.MODID) || sound.getNamespace().equals("openblocks")) {
            return switch (group) {
                case "elevator" -> framed("item/ender_pearl");
                case "luggage" -> switch (action) {
                    case "food" -> framed("item/baked_potato");
                    case "item" -> framed("item/apple");
                    default -> GENERIC;
                };
                case "crayon" -> framed("item/writable_book");
                default -> GENERIC;
            };
        }
        if (!sound.getNamespace().equals("minecraft")) return GENERIC;
        return switch (group) {
            case "block" -> block(subject, action);
            case "entity" -> entity(subject, action);
            case "item" -> item(subject, action);
            case "record", "music_disc" -> DISCS.contains(subject) ? framed(vanilla("item/music_disc_" + subject), WHITE, 0x0000FF) : UNKNOWN;
            case "weather" -> subject.equals("rain") ? single(openBlocks("rain"), 0x0000FF) : UNKNOWN;
            case "ui" -> CLICK;
            default -> UNKNOWN;
        };
    }

    private static Icon block(final String subject, final String action) {
        if (subject.equals("note") || subject.equals("note_block")) return single(openBlocks("note"), NOTES.getOrDefault(action, WHITE));
        final String surface = SURFACES.get(subject);
        if (surface != null) {
            final ResourceLocation texture = vanilla("block/" + surface);
            if (action.equals("place")) return framed(texture, WHITE, WHITE);
            final ResourceLocation detail = switch (action) {
                case "break" -> vanilla("block/destroy_stage_5");
                case "fall" -> openBlocks("mob_hurt");
                case "hit" -> vanilla("item/diamond_shovel");
                case "step" -> vanilla("item/diamond_boots");
                default -> null;
            };
            if (detail != null) return new Icon(List.of(new Layer(texture, WHITE, 0.6F), new Layer(detail, WHITE, 0.6F), FRAME));
            return new Icon(List.of(new Layer(texture, WHITE, 1), new Layer(openBlocks("unknown"), WHITE, 1)));
        }
        final String button = SWITCHES.get(subject);
        if (button != null) return new Icon(List.of(new Layer(vanilla("block/" + button), WHITE, 1), new Layer(openBlocks("click"), WHITE, 1)));
        final String known = BLOCKS.get(subject);
        return known == null ? UNKNOWN : framed(known);
    }

    private static Icon entity(final String subject, final String action) {
        if (subject.equals("player")) {
            return switch (action) {
                case "burp" -> framed("item/baked_potato");
                case "levelup" -> framed("item/experience_bottle");
                case "die", "hurt" -> new Icon(List.of(new Layer(openBlocks("player"), WHITE, 0.6F),
                        new Layer(openBlocks(action.equals("die") ? "mob_death" : "mob_hurt"), WHITE, 0.36F),
                        new Layer(openBlocks("frame"), RED, 1)));
                default -> framed(openBlocks("player"), WHITE, WHITE);
            };
        }
        if (subject.equals("generic")) return genericEntity(action);
        if (subject.equals("item") || subject.equals("lightning") || subject.equals("lightning_bolt")) return CLICK;
        final String prop = ENTITY_ICONS.get(subject);
        if (prop != null) return framed(prop);

        final var type = EntityType.byString("minecraft:" + MOB_ALIASES.getOrDefault(subject, subject)).orElse(null);
        if (type == null || (type.getCategory() == MobCategory.MISC && !FRIENDLY_MISC.contains(type))) return UNKNOWN;
        final var egg = SpawnEggItem.byId(type);
        final String symbol = switch (action) {
            case "death" -> "mob_death";
            case "hurt" -> "mob_hurt";
            default -> type.getCategory() == MobCategory.MONSTER ? "mob_hostile" : "mob_friendly";
        };
        return framed(openBlocks(symbol), egg == null ? WHITE : egg.getColor(0), egg == null ? 0 : egg.getColor(1));
    }

    private static Icon genericEntity(final String action) {
        return switch (action) {
            case "burn" -> framed("block/fire_0");
            case "death" -> single(openBlocks("mob_death"), WHITE);
            case "drink" -> framed("item/potion");
            case "eat" -> framed("item/baked_potato");
            case "extinguish_fire", "swim" -> single(openBlocks("fizz"), WHITE);
            case "hurt", "small_fall" -> single(openBlocks("mob_hurt"), WHITE);
            case "splash" -> single(openBlocks("rain"), WHITE);
            default -> GENERIC;
        };
    }

    private static Icon item(final String subject, final String action) {
        final String texture = subject.equals("armor") ? ARMOR.get(action) : ITEMS.get(subject);
        return texture == null ? UNKNOWN : framed("item/" + texture);
    }

    private static Icon single(final ResourceLocation texture, final int color) {
        return new Icon(List.of(new Layer(texture, color, 1)));
    }

    private static Icon framed(final String texture) { return framed(vanilla(texture), WHITE, WHITE); }

    private static Icon framed(final ResourceLocation texture, final int tint, final int frame) {
        return new Icon(List.of(new Layer(texture, tint, 0.6F), new Layer(openBlocks("frame"), frame, 1)));
    }

    private static ResourceLocation openBlocks(final String icon) { return OpenBlocks.id("item/sound_" + icon); }
    private static ResourceLocation vanilla(final String path) { return new ResourceLocation("minecraft", path); }

    public record Icon(List<Layer> layers) {}
    public record Layer(ResourceLocation sprite, int color, float scale) {}
}
