package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.item.OBAnimatedFloorItem;
import art.arcane.openblocks.item.OBCannonItem;
import art.arcane.openblocks.item.OBCraneBackpackItem;
import art.arcane.openblocks.item.OBCartographerItem;
import art.arcane.openblocks.item.OBCraneControlItem;
import art.arcane.openblocks.item.OBDevNullItem;
import art.arcane.openblocks.item.OBElevatorItem;
import art.arcane.openblocks.item.OBEmptyMapItem;
import art.arcane.openblocks.item.OBFlagItem;
import art.arcane.openblocks.item.OBGlyphItem;
import art.arcane.openblocks.item.OBGuideItem;
import art.arcane.openblocks.item.OBHangGliderItem;
import art.arcane.openblocks.item.OBHeightMapItem;
import art.arcane.openblocks.item.OBLuggageItem;
import art.arcane.openblocks.item.OBPedometerItem;
import art.arcane.openblocks.item.OBPaintBrushItem;
import art.arcane.openblocks.item.OBPaintCanItem;
import art.arcane.openblocks.item.OBPointerItem;
import art.arcane.openblocks.item.OBSqueegeeItem;
import art.arcane.openblocks.item.OBStencilItem;
import art.arcane.openblocks.item.OBScaffoldingItem;
import art.arcane.openblocks.item.OBSkyItem;
import art.arcane.openblocks.item.OBSpongeOnAStickItem;
import art.arcane.openblocks.item.OBTankItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
    public static final RegistryObject<Item> GUIDE = registerItem("guide", () -> new OBGuideItem(OBBlocks.GUIDE.get()));
    public static final RegistryObject<Item> BUILDER_GUIDE = registerItem("builder_guide", () -> new OBGuideItem(OBBlocks.BUILDER_GUIDE.get()));
    public static final RegistryObject<Item> ELEVATOR = registerItem("elevator",
            () -> new OBElevatorItem(OBBlocks.ELEVATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ELEVATOR_ROTATING = registerItem("elevator_rotating",
            () -> new OBElevatorItem(OBBlocks.ELEVATOR_ROTATING.get(), new Item.Properties()));
    public static final RegistryObject<Item> HEAL = registerBlockItem("heal", OBBlocks.HEAL);
    public static final RegistryObject<Item> TARGET = registerBlockItem("target", OBBlocks.TARGET);
    public static final RegistryObject<Item> GRAVE = registerBlockItem("grave", OBBlocks.GRAVE);
    public static final RegistryObject<Item> FLAG = registerItem("flag", () -> new OBFlagItem(OBBlocks.FLAG.get(), new Item.Properties()));
    public static final RegistryObject<Item> TANK = registerItem("tank", () -> new OBTankItem(OBBlocks.TANK.get(), new Item.Properties()));
    public static final RegistryObject<Item> TROPHY = registerBlockItem("trophy", OBBlocks.TROPHY);
    public static final RegistryObject<Item> BEARTRAP = registerItem("beartrap", () -> new OBAnimatedFloorItem(OBBlocks.BEARTRAP.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPRINKLER = registerBlockItem("sprinkler", OBBlocks.SPRINKLER);
    public static final RegistryObject<Item> CANNON = registerItem("cannon", () -> new OBCannonItem(OBBlocks.CANNON.get(), new Item.Properties()));
    public static final RegistryObject<Item> VACUUM_HOPPER = registerBlockItem("vacuum_hopper", OBBlocks.VACUUM_HOPPER);
    public static final RegistryObject<Item> SPONGE = registerBlockItem("sponge", OBBlocks.SPONGE);
    public static final RegistryObject<Item> BIG_BUTTON = registerBlockItem("big_button", OBBlocks.BIG_BUTTON);
    public static final RegistryObject<Item> BIG_BUTTON_WOOD = registerBlockItem("big_button_wood", OBBlocks.BIG_BUTTON_WOOD);
    public static final RegistryObject<Item> IMAGINARY = registerBlockItem("imaginary", OBBlocks.IMAGINARY);
    public static final RegistryObject<Item> FAN = registerItem("fan", () -> new OBAnimatedFloorItem(OBBlocks.FAN.get(), new Item.Properties()));
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
    public static final RegistryObject<Item> PAINT_CAN = registerItem("paint_can", () -> new OBPaintCanItem(OBBlocks.PAINT_CAN.get()));
    public static final RegistryObject<Item> CANVAS_GLASS = registerBlockItem("canvas_glass", OBBlocks.CANVAS_GLASS);
    public static final RegistryObject<Item> PROJECTOR = registerBlockItem("projector", OBBlocks.PROJECTOR);
    public static final RegistryObject<Item> DRAWING_TABLE = registerBlockItem("drawing_table", OBBlocks.DRAWING_TABLE);
    public static final RegistryObject<Item> SKY = registerItem("sky", () -> new OBSkyItem(OBBlocks.SKY.get(), new Item.Properties()));
    public static final RegistryObject<Item> XP_SHOWER = registerBlockItem("xp_shower", OBBlocks.XP_SHOWER);
    public static final RegistryObject<Item> GOLDEN_EGG = registerBlockItem("golden_egg", OBBlocks.GOLDEN_EGG);
    public static final RegistryObject<Item> SCAFFOLDING = registerItem("scaffolding",
            () -> new OBScaffoldingItem(OBBlocks.SCAFFOLDING.get(), new Item.Properties()));

    // Standalone items
    public static final RegistryObject<Item> HANG_GLIDER = registerItem("hang_glider", OBHangGliderItem::new);
    public static final RegistryObject<Item> GLIDER_WING = registerItem("glider_wing");
    public static final RegistryObject<Item> BEAM = registerItem("beam");
    public static final RegistryObject<Item> CRANE_ENGINE = registerItem("crane_engine");
    public static final RegistryObject<Item> CRANE_MAGNET = registerItem("crane_magnet");
    public static final RegistryObject<Item> MIRACLE_MAGNET = registerItem("miracle_magnet", () -> new Item(new Item.Properties()) {
        @Override
        public boolean isFoil(final ItemStack stack) {
            return true;
        }
    });
    public static final RegistryObject<Item> LINE = registerItem("line");
    public static final RegistryObject<Item> MAP_CONTROLLER = registerItem("map_controller");
    public static final RegistryObject<Item> MAP_MEMORY = registerItem("map_memory");
    public static final RegistryObject<Item> ASSISTANT_BASE = registerItem("assistant_base");
    public static final RegistryObject<Item> UNPREPARED_STENCIL = registerItem("unprepared_stencil");
    public static final RegistryObject<Item> SKETCHING_PENCIL = registerItem("sketching_pencil");
    public static final RegistryObject<Item> POINTER = registerItem("pointer", () -> new OBPointerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LUGGAGE = registerItem("luggage", OBLuggageItem::new);
    public static final RegistryObject<Item> SONIC_GLASSES = registerItem("sonic_glasses");
    public static final RegistryObject<Item> PENCIL_GLASSES = registerItem("pencil_glasses");
    public static final RegistryObject<Item> CRAYON_GLASSES = registerItem("crayon_glasses");
    public static final RegistryObject<Item> TECHNICOLOR_GLASSES = registerItem("technicolor_glasses");
    public static final RegistryObject<Item> SERIOUS_GLASSES = registerItem("serious_glasses");
    public static final RegistryObject<Item> CRANE_CONTROL = registerItem("crane_control", OBCraneControlItem::new);
    public static final RegistryObject<Item> CRANE_BACKPACK = registerItem("crane_backpack", OBCraneBackpackItem::new);
    public static final RegistryObject<Item> SLIMALYZER = registerItem("slimalyzer");
    public static final RegistryObject<Item> XP_BUCKET = registerBucketItem("xp_bucket");
    public static final RegistryObject<Item> SLEEPING_BAG = registerItem("sleeping_bag");
    public static final RegistryObject<Item> PAINTBRUSH = registerItem("paintbrush", OBPaintBrushItem::new);
    public static final RegistryObject<Item> STENCIL = registerItem("stencil", OBStencilItem::new);
    public static final RegistryObject<Item> SQUEEGEE = registerItem("squeegee", OBSqueegeeItem::new);
    public static final RegistryObject<Item> HEIGHT_MAP = registerItem("height_map", OBHeightMapItem::new);
    public static final RegistryObject<Item> EMPTY_MAP = registerItem("empty_map", OBEmptyMapItem::new);
    public static final RegistryObject<Item> CARTOGRAPHER = registerItem("cartographer", OBCartographerItem::new);
    public static final RegistryObject<Item> TASTY_CLAY = registerItem("tasty_clay", new Item.Properties().food(
            new FoodProperties.Builder().nutrition(1).saturationMod(0.1f).alwaysEat().build()));
    public static final RegistryObject<Item> GOLDEN_EYE = registerItem("golden_eye");
    public static final RegistryObject<Item> CURSOR = registerItem("cursor");
    public static final RegistryObject<Item> INFO_BOOK = registerItem("info_book");
    public static final RegistryObject<Item> DEV_NULL = registerItem("dev_null", () -> new OBDevNullItem(new Item.Properties()));
    public static final RegistryObject<Item> SPONGE_ON_A_STICK = registerItem("sponge_on_a_stick", OBSpongeOnAStickItem::new);
    public static final RegistryObject<Item> PEDOMETER = registerItem("pedometer", OBPedometerItem::new);
    public static final RegistryObject<Item> EPIC_ERASER = registerItem("epic_eraser");
    public static final RegistryObject<Item> WRENCH = registerItem("wrench");
    public static final RegistryObject<Item> GLYPH = registerItem("glyph", OBGlyphItem::new);

    // Keep the scaffold IDs loadable in existing worlds.
    public static final RegistryObject<Item> GENERIC = ITEMS.register("generic", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GENERIC_UNSTACKABLE = ITEMS.register("generic_unstackable",
            () -> new Item(new Item.Properties().stacksTo(1)));

    private OBItems() {}

    public static List<RegistryObject<Item>> all() {
        return Collections.unmodifiableList(CREATIVE_ORDER);
    }

    private static RegistryObject<Item> registerItem(final String id) {
        return registerItem(id, new Item.Properties());
    }

    private static RegistryObject<Item> registerItem(final String id, final Item.Properties properties) {
        final RegistryObject<Item> item = ITEMS.register(id, () -> new Item(properties));
        CREATIVE_ORDER.add(item);
        return item;
    }

    private static RegistryObject<Item> registerItem(final String id, final Supplier<? extends Item> factory) {
        final RegistryObject<Item> item = ITEMS.register(id, factory);
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
