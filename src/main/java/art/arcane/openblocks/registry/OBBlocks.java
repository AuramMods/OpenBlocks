package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OpenBlocks.MODID);

    private static final List<RegistryObject<Block>> CREATIVE_ORDER = new ArrayList<>();

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
        final RegistryObject<Block> block = BLOCKS.register(id,
                () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)));
        CREATIVE_ORDER.add(block);
        return block;
    }
}
