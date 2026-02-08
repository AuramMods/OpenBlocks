package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OpenBlocks.MODID);

    private static final List<RegistryObject<Item>> CREATIVE_ORDER = new ArrayList<>();

    // Block items
    public static final RegistryObject<Item> LADDER = registerBlockItem("ladder", OBBlocks.LADDER);
    public static final RegistryObject<Item> GUIDE = registerBlockItem("guide", OBBlocks.GUIDE);
    public static final RegistryObject<Item> BUILDER_GUIDE = registerBlockItem("builder_guide", OBBlocks.BUILDER_GUIDE);
    public static final RegistryObject<Item> ELEVATOR = registerBlockItem("elevator", OBBlocks.ELEVATOR);
    public static final RegistryObject<Item> ELEVATOR_ROTATING = registerBlockItem("elevator_rotating", OBBlocks.ELEVATOR_ROTATING);
    public static final RegistryObject<Item> HEAL = registerBlockItem("heal", OBBlocks.HEAL);
    public static final RegistryObject<Item> TARGET = registerBlockItem("target", OBBlocks.TARGET);
    public static final RegistryObject<Item> GRAVE = registerBlockItem("grave", OBBlocks.GRAVE);
    public static final RegistryObject<Item> FLAG = registerBlockItem("flag", OBBlocks.FLAG);
    public static final RegistryObject<Item> TANK = registerBlockItem("tank", OBBlocks.TANK);
    public static final RegistryObject<Item> TROPHY = registerBlockItem("trophy", OBBlocks.TROPHY);
    public static final RegistryObject<Item> BEARTRAP = registerBlockItem("beartrap", OBBlocks.BEARTRAP);
    public static final RegistryObject<Item> SPRINKLER = registerBlockItem("sprinkler", OBBlocks.SPRINKLER);
    public static final RegistryObject<Item> CANNON = registerBlockItem("cannon", OBBlocks.CANNON);
    public static final RegistryObject<Item> VACUUM_HOPPER = registerBlockItem("vacuum_hopper", OBBlocks.VACUUM_HOPPER);
    public static final RegistryObject<Item> SPONGE = registerBlockItem("sponge", OBBlocks.SPONGE);
    public static final RegistryObject<Item> BIG_BUTTON = registerBlockItem("big_button", OBBlocks.BIG_BUTTON);
    public static final RegistryObject<Item> BIG_BUTTON_WOOD = registerBlockItem("big_button_wood", OBBlocks.BIG_BUTTON_WOOD);
    public static final RegistryObject<Item> IMAGINARY = registerBlockItem("imaginary", OBBlocks.IMAGINARY);
    public static final RegistryObject<Item> FAN = registerBlockItem("fan", OBBlocks.FAN);
    public static final RegistryObject<Item> XP_BOTTLER = registerBlockItem("xp_bottler", OBBlocks.XP_BOTTLER);
    public static final RegistryObject<Item> VILLAGE_HIGHLIGHTER = registerBlockItem("village_highlighter", OBBlocks.VILLAGE_HIGHLIGHTER);
    public static final RegistryObject<Item> PATH = registerBlockItem("path", OBBlocks.PATH);
    public static final RegistryObject<Item> AUTO_ANVIL = registerBlockItem("auto_anvil", OBBlocks.AUTO_ANVIL);
    public static final RegistryObject<Item> AUTO_ENCHANTMENT_TABLE = registerBlockItem("auto_enchantment_table", OBBlocks.AUTO_ENCHANTMENT_TABLE);
    public static final RegistryObject<Item> XP_DRAIN = registerBlockItem("xp_drain", OBBlocks.XP_DRAIN);
    public static final RegistryObject<Item> BLOCK_BREAKER = registerBlockItem("block_breaker", OBBlocks.BLOCK_BREAKER);
    public static final RegistryObject<Item> BLOCK_PLACER = registerBlockItem("block_placer", OBBlocks.BLOCK_PLACER);
    public static final RegistryObject<Item> ITEM_DROPPER = registerBlockItem("item_dropper", OBBlocks.ITEM_DROPPER);
    public static final RegistryObject<Item> ROPE_LADDER = registerBlockItem("rope_ladder", OBBlocks.ROPE_LADDER);
    public static final RegistryObject<Item> DONATION_STATION = registerBlockItem("donation_station", OBBlocks.DONATION_STATION);
    public static final RegistryObject<Item> PAINT_MIXER = registerBlockItem("paint_mixer", OBBlocks.PAINT_MIXER);
    public static final RegistryObject<Item> CANVAS = registerBlockItem("canvas", OBBlocks.CANVAS);
    public static final RegistryObject<Item> PAINT_CAN = registerBlockItem("paint_can", OBBlocks.PAINT_CAN);
    public static final RegistryObject<Item> CANVAS_GLASS = registerBlockItem("canvas_glass", OBBlocks.CANVAS_GLASS);
    public static final RegistryObject<Item> PROJECTOR = registerBlockItem("projector", OBBlocks.PROJECTOR);
    public static final RegistryObject<Item> DRAWING_TABLE = registerBlockItem("drawing_table", OBBlocks.DRAWING_TABLE);
    public static final RegistryObject<Item> SKY = registerBlockItem("sky", OBBlocks.SKY);
    public static final RegistryObject<Item> XP_SHOWER = registerBlockItem("xp_shower", OBBlocks.XP_SHOWER);
    public static final RegistryObject<Item> GOLDEN_EGG = registerBlockItem("golden_egg", OBBlocks.GOLDEN_EGG);
    public static final RegistryObject<Item> SCAFFOLDING = registerBlockItem("scaffolding", OBBlocks.SCAFFOLDING);

    // Standalone items
    public static final RegistryObject<Item> HANG_GLIDER = registerItem("hang_glider");
    public static final RegistryObject<Item> GENERIC = registerItem("generic");
    public static final RegistryObject<Item> LUGGAGE = registerItem("luggage");
    public static final RegistryObject<Item> SONIC_GLASSES = registerItem("sonic_glasses");
    public static final RegistryObject<Item> PENCIL_GLASSES = registerItem("pencil_glasses");
    public static final RegistryObject<Item> CRAYON_GLASSES = registerItem("crayon_glasses");
    public static final RegistryObject<Item> TECHNICOLOR_GLASSES = registerItem("technicolor_glasses");
    public static final RegistryObject<Item> SERIOUS_GLASSES = registerItem("serious_glasses");
    public static final RegistryObject<Item> CRANE_CONTROL = registerItem("crane_control");
    public static final RegistryObject<Item> CRANE_BACKPACK = registerItem("crane_backpack");
    public static final RegistryObject<Item> SLIMALYZER = registerItem("slimalyzer");
    public static final RegistryObject<Item> XP_BUCKET = registerBucketItem("xp_bucket");
    public static final RegistryObject<Item> SLEEPING_BAG = registerItem("sleeping_bag");
    public static final RegistryObject<Item> PAINTBRUSH = registerItem("paintbrush");
    public static final RegistryObject<Item> STENCIL = registerItem("stencil");
    public static final RegistryObject<Item> SQUEEGEE = registerItem("squeegee");
    public static final RegistryObject<Item> HEIGHT_MAP = registerItem("height_map");
    public static final RegistryObject<Item> EMPTY_MAP = registerItem("empty_map");
    public static final RegistryObject<Item> CARTOGRAPHER = registerItem("cartographer");
    public static final RegistryObject<Item> TASTY_CLAY = registerItem("tasty_clay");
    public static final RegistryObject<Item> GOLDEN_EYE = registerItem("golden_eye");
    public static final RegistryObject<Item> GENERIC_UNSTACKABLE = registerItem("generic_unstackable");
    public static final RegistryObject<Item> CURSOR = registerItem("cursor");
    public static final RegistryObject<Item> INFO_BOOK = registerItem("info_book");
    public static final RegistryObject<Item> DEV_NULL = registerItem("dev_null");
    public static final RegistryObject<Item> SPONGE_ON_A_STICK = registerItem("sponge_on_a_stick");
    public static final RegistryObject<Item> PEDOMETER = registerItem("pedometer");
    public static final RegistryObject<Item> EPIC_ERASER = registerItem("epic_eraser");
    public static final RegistryObject<Item> WRENCH = registerItem("wrench");
    public static final RegistryObject<Item> GLYPH = registerItem("glyph");

    private OBItems() {}

    public static List<RegistryObject<Item>> all() {
        return Collections.unmodifiableList(CREATIVE_ORDER);
    }

    private static RegistryObject<Item> registerItem(final String id) {
        final RegistryObject<Item> item = ITEMS.register(id, () -> new Item(new Item.Properties()));
        CREATIVE_ORDER.add(item);
        return item;
    }

    private static RegistryObject<Item> registerBlockItem(final String id, final RegistryObject<Block> block) {
        final RegistryObject<Item> item = ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        CREATIVE_ORDER.add(item);
        return item;
    }

    private static RegistryObject<Item> registerBucketItem(final String id) {
        final RegistryObject<Item> item = ITEMS.register(id,
                () -> new BucketItem(OBFluids.XP_JUICE, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
        CREATIVE_ORDER.add(item);
        return item;
    }
}
