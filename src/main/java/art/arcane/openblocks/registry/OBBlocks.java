package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBGraveBlock;
import art.arcane.openblocks.block.OBHealBlock;
import art.arcane.openblocks.block.OBPathBlock;
import art.arcane.openblocks.block.OBShapeBlock;
import art.arcane.openblocks.block.OBSpongeBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OpenBlocks.MODID);

    private static final List<RegistryObject<Block>> CREATIVE_ORDER = new ArrayList<>();
    private static final VoxelShape SHAPE_PATH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.6D, 16.0D);
    private static final VoxelShape SHAPE_XP_DRAIN = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final VoxelShape SHAPE_LADDER = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
    private static final VoxelShape SHAPE_ROPE_LADDER = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape SHAPE_BIG_BUTTON = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 2.0D, 15.0D);
    private static final VoxelShape SHAPE_FLAG = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape SHAPE_TROPHY = Block.box(3.2D, 0.0D, 3.2D, 12.8D, 3.2D, 12.8D);
    private static final VoxelShape SHAPE_VACUUM_HOPPER = Block.box(4.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D);
    private static final VoxelShape SHAPE_GOLDEN_EGG = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape SHAPE_AUTO_ENCHANT = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    private static final VoxelShape SHAPE_AUTO_ANVIL = Block.box(2.0D, 0.0D, 0.0D, 14.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_PAINT_CAN = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 11.0D, 12.0D);
    private static final VoxelShape SHAPE_PAINT_MIXER = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape SHAPE_XP_SHOWER = Block.box(7.0D, 7.0D, 7.0D, 9.0D, 9.0D, 16.0D);
    private static final VoxelShape SHAPE_PROJECTOR = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final VoxelShape SHAPE_VILLAGE_HIGHLIGHTER = Block.box(2.0D, 0.0D, 1.0D, 14.0D, 16.0D, 15.0D);
    private static final VoxelShape SHAPE_FAN = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private static final VoxelShape SHAPE_BEARTRAP = Block.box(1.6D, 0.0D, 1.6D, 14.4D, 6.4D, 14.4D);
    private static final VoxelShape SHAPE_CANNON = Block.box(4.8D, 0.0D, 4.8D, 9.6D, 11.2D, 11.2D);
    private static final VoxelShape SHAPE_TANK_FRAME = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D),
            Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 1.0D, 1.0D, 16.0D, 15.0D),
            Block.box(15.0D, 0.0D, 1.0D, 16.0D, 16.0D, 15.0D));
    private static final VoxelShape SHAPE_GRAVE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D),
            Block.box(2.0D, 1.0D, 14.0D, 14.0D, 16.0D, 16.0D));
    private static final VoxelShape SHAPE_TARGET = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 16.0D),
            Block.box(15.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D),
            Block.box(0.0D, 1.0D, 0.0D, 16.0D, 2.0D, 15.0D));
    private static final VoxelShape SHAPE_SPRINKLER = Block.box(4.8D, 0.0D, 0.0D, 11.2D, 4.8D, 16.0D);
    private static final VoxelShape SHAPE_SCAFFOLDING = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 2.0D, 0.0D, 2.0D, 14.0D, 2.0D),
            Block.box(14.0D, 2.0D, 0.0D, 16.0D, 14.0D, 2.0D),
            Block.box(0.0D, 2.0D, 14.0D, 2.0D, 14.0D, 16.0D),
            Block.box(14.0D, 2.0D, 14.0D, 16.0D, 14.0D, 16.0D));

    public static final RegistryObject<Block> LADDER = register("ladder");
    public static final RegistryObject<Block> GUIDE = register("guide");
    public static final RegistryObject<Block> BUILDER_GUIDE = register("builder_guide");
    public static final RegistryObject<Block> ELEVATOR = register("elevator");
    public static final RegistryObject<Block> ELEVATOR_ROTATING = register("elevator_rotating");
    public static final RegistryObject<Block> HEAL = register("heal");
    public static final RegistryObject<Block> TARGET = register("target");
    public static final RegistryObject<Block> GRAVE = register("grave");
    public static final RegistryObject<Block> FLAG = register("flag");
    public static final RegistryObject<Block> TANK = register("tank");
    public static final RegistryObject<Block> TROPHY = register("trophy");
    public static final RegistryObject<Block> BEARTRAP = register("beartrap");
    public static final RegistryObject<Block> SPRINKLER = register("sprinkler");
    public static final RegistryObject<Block> CANNON = register("cannon");
    public static final RegistryObject<Block> VACUUM_HOPPER = register("vacuum_hopper");
    public static final RegistryObject<Block> SPONGE = register("sponge");
    public static final RegistryObject<Block> BIG_BUTTON = register("big_button");
    public static final RegistryObject<Block> BIG_BUTTON_WOOD = register("big_button_wood");
    public static final RegistryObject<Block> IMAGINARY = register("imaginary");
    public static final RegistryObject<Block> FAN = register("fan");
    public static final RegistryObject<Block> XP_BOTTLER = register("xp_bottler");
    public static final RegistryObject<Block> VILLAGE_HIGHLIGHTER = register("village_highlighter");
    public static final RegistryObject<Block> PATH = register("path");
    public static final RegistryObject<Block> AUTO_ANVIL = register("auto_anvil");
    public static final RegistryObject<Block> AUTO_ENCHANTMENT_TABLE = register("auto_enchantment_table");
    public static final RegistryObject<Block> XP_DRAIN = register("xp_drain");
    public static final RegistryObject<Block> BLOCK_BREAKER = register("block_breaker");
    public static final RegistryObject<Block> BLOCK_PLACER = register("block_placer");
    public static final RegistryObject<Block> ITEM_DROPPER = register("item_dropper");
    public static final RegistryObject<Block> ROPE_LADDER = register("rope_ladder");
    public static final RegistryObject<Block> DONATION_STATION = register("donation_station");
    public static final RegistryObject<Block> PAINT_MIXER = register("paint_mixer");
    public static final RegistryObject<Block> CANVAS = register("canvas");
    public static final RegistryObject<Block> PAINT_CAN = register("paint_can");
    public static final RegistryObject<Block> CANVAS_GLASS = register("canvas_glass");
    public static final RegistryObject<Block> PROJECTOR = register("projector");
    public static final RegistryObject<Block> DRAWING_TABLE = register("drawing_table");
    public static final RegistryObject<Block> SKY = register("sky");
    public static final RegistryObject<Block> XP_SHOWER = register("xp_shower");
    public static final RegistryObject<Block> GOLDEN_EGG = register("golden_egg");
    public static final RegistryObject<Block> SCAFFOLDING = register("scaffolding");

    private OBBlocks() {}

    public static List<RegistryObject<Block>> all() {
        return Collections.unmodifiableList(CREATIVE_ORDER);
    }

    private static RegistryObject<Block> register(final String id) {
        final RegistryObject<Block> block = BLOCKS.register(id, () -> createBlock(id));
        CREATIVE_ORDER.add(block);
        return block;
    }

    private static Block createBlock(final String id) {
        final VoxelShape shape = shapeFor(id);
        final VoxelShape collisionShape = collisionShapeFor(id, shape);
        final BlockBehaviour.Properties properties = isNonOccludingModel(id)
                ? BlockBehaviour.Properties.copy(Blocks.STONE).noOcclusion()
                : BlockBehaviour.Properties.copy(Blocks.STONE);

        if ("heal".equals(id)) return new OBHealBlock(properties);
        if ("path".equals(id)) return new OBPathBlock(properties, shape, collisionShape);
        if ("sponge".equals(id)) return new OBSpongeBlock(BlockBehaviour.Properties.copy(Blocks.SPONGE));
        if ("grave".equals(id)) return new OBGraveBlock(properties, shape, collisionShape);
        if (shape != null) return new OBShapeBlock(properties, shape, collisionShape);
        return new Block(properties);
    }

    private static boolean isNonOccludingModel(final String id) {
        return switch (id) {
            case "ladder", "guide", "builder_guide", "target", "grave", "flag",
                    "tank", "trophy", "beartrap", "sprinkler", "cannon", "vacuum_hopper",
                    "big_button", "big_button_wood", "imaginary", "fan", "village_highlighter",
                    "path", "auto_anvil", "auto_enchantment_table", "xp_drain", "rope_ladder",
                    "donation_station", "paint_mixer", "canvas", "paint_can", "canvas_glass",
                    "projector", "xp_shower", "golden_egg", "scaffolding" -> true;
            default -> false;
        };
    }

    private static VoxelShape shapeFor(final String id) {
        return switch (id) {
            case "ladder" -> SHAPE_LADDER;
            case "rope_ladder" -> SHAPE_ROPE_LADDER;
            case "path" -> SHAPE_PATH;
            case "xp_drain" -> SHAPE_XP_DRAIN;
            case "sprinkler" -> SHAPE_SPRINKLER;
            case "target" -> SHAPE_TARGET;
            case "flag" -> SHAPE_FLAG;
            case "grave" -> SHAPE_GRAVE;
            case "tank" -> SHAPE_TANK_FRAME;
            case "trophy" -> SHAPE_TROPHY;
            case "beartrap" -> SHAPE_BEARTRAP;
            case "cannon" -> SHAPE_CANNON;
            case "vacuum_hopper" -> SHAPE_VACUUM_HOPPER;
            case "fan" -> SHAPE_FAN;
            case "golden_egg" -> SHAPE_GOLDEN_EGG;
            case "auto_anvil" -> SHAPE_AUTO_ANVIL;
            case "auto_enchantment_table" -> SHAPE_AUTO_ENCHANT;
            case "paint_mixer" -> SHAPE_PAINT_MIXER;
            case "paint_can" -> SHAPE_PAINT_CAN;
            case "projector" -> SHAPE_PROJECTOR;
            case "xp_shower" -> SHAPE_XP_SHOWER;
            case "village_highlighter" -> SHAPE_VILLAGE_HIGHLIGHTER;
            case "big_button", "big_button_wood" -> SHAPE_BIG_BUTTON;
            case "scaffolding" -> SHAPE_SCAFFOLDING;
            default -> null;
        };
    }

    private static VoxelShape collisionShapeFor(final String id, final VoxelShape shape) {
        return switch (id) {
            case "path", "cannon", "flag" -> Shapes.empty();
            default -> shape == null ? Shapes.block() : shape;
        };
    }
}
