package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBPlaceholderBlockEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OpenBlocks.MODID);

    private static final List<RegistryObject<BlockEntityType<?>>> ALL = new ArrayList<>();

    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> GUIDE = register("guide", OBBlocks.GUIDE);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> BUILDER_GUIDE = register("builder_guide", OBBlocks.BUILDER_GUIDE);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> ELEVATOR_ROTATING = register("elevator_rotating", OBBlocks.ELEVATOR_ROTATING);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> HEAL = register("heal", OBBlocks.HEAL);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> TARGET = register("target", OBBlocks.TARGET);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> GRAVE = register("grave", OBBlocks.GRAVE);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> FLAG = register("flag", OBBlocks.FLAG);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> TANK = register("tank", OBBlocks.TANK);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> TROPHY = register("trophy", OBBlocks.TROPHY);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> BEARTRAP = register("beartrap", OBBlocks.BEARTRAP);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> SPRINKLER = register("sprinkler", OBBlocks.SPRINKLER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> CANNON = register("cannon", OBBlocks.CANNON);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> VACUUM_HOPPER = register("vacuum_hopper", OBBlocks.VACUUM_HOPPER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> BIG_BUTTON = register("big_button", OBBlocks.BIG_BUTTON);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> BIG_BUTTON_WOOD = register("big_button_wood", OBBlocks.BIG_BUTTON_WOOD);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> IMAGINARY = register("imaginary", OBBlocks.IMAGINARY);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> FAN = register("fan", OBBlocks.FAN);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> XP_BOTTLER = register("xp_bottler", OBBlocks.XP_BOTTLER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> VILLAGE_HIGHLIGHTER = register("village_highlighter", OBBlocks.VILLAGE_HIGHLIGHTER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> AUTO_ANVIL = register("auto_anvil", OBBlocks.AUTO_ANVIL);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> AUTO_ENCHANTMENT_TABLE = register("auto_enchantment_table", OBBlocks.AUTO_ENCHANTMENT_TABLE);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> XP_DRAIN = register("xp_drain", OBBlocks.XP_DRAIN);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> BLOCK_BREAKER = register("block_breaker", OBBlocks.BLOCK_BREAKER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> BLOCK_PLACER = register("block_placer", OBBlocks.BLOCK_PLACER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> ITEM_DROPPER = register("item_dropper", OBBlocks.ITEM_DROPPER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> DONATION_STATION = register("donation_station", OBBlocks.DONATION_STATION);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> PAINT_MIXER = register("paint_mixer", OBBlocks.PAINT_MIXER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> CANVAS = register("canvas", OBBlocks.CANVAS);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> PAINT_CAN = register("paint_can", OBBlocks.PAINT_CAN);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> CANVAS_GLASS = register("canvas_glass", OBBlocks.CANVAS_GLASS);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> PROJECTOR = register("projector", OBBlocks.PROJECTOR);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> DRAWING_TABLE = register("drawing_table", OBBlocks.DRAWING_TABLE);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> SKY = register("sky", OBBlocks.SKY);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> XP_SHOWER = register("xp_shower", OBBlocks.XP_SHOWER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> GOLDEN_EGG = register("golden_egg", OBBlocks.GOLDEN_EGG);

    private OBBlockEntities() {}

    public static List<RegistryObject<BlockEntityType<?>>> all() {
        return Collections.unmodifiableList(ALL);
    }

    @SafeVarargs
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> register(final String id, final RegistryObject<Block>... blocks) {
        final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>>[] holder = new RegistryObject[1];
        holder[0] = BLOCK_ENTITY_TYPES.register(id, () -> BlockEntityType.Builder.of(
                (pos, state) -> new OBPlaceholderBlockEntity(holder[0], pos, state),
                resolveBlocks(blocks)).build(null));
        ALL.add((RegistryObject) holder[0]);
        return holder[0];
    }

    private static Block[] resolveBlocks(final RegistryObject<Block>[] blocks) {
        final Block[] resolved = new Block[blocks.length];
        for (int i = 0; i < blocks.length; i++) resolved[i] = blocks[i].get();
        return resolved;
    }
}
