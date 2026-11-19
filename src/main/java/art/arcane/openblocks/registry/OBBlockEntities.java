package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBRedstoneMachineBlock.Kind;
import art.arcane.openblocks.block.entity.OBBearTrapBlockEntity;
import art.arcane.openblocks.block.entity.OBBigButtonBlockEntity;
import art.arcane.openblocks.block.entity.OBCannonBlockEntity;
import art.arcane.openblocks.block.entity.OBDonationStationBlockEntity;
import art.arcane.openblocks.block.entity.OBFanBlockEntity;
import art.arcane.openblocks.block.entity.OBFlagBlockEntity;
import art.arcane.openblocks.block.entity.OBFoldingTargetBlockEntity;
import art.arcane.openblocks.block.entity.OBGraveBlockEntity;
import art.arcane.openblocks.block.entity.OBGoldenEggBlockEntity;
import art.arcane.openblocks.block.entity.OBHealBlockEntity;
import art.arcane.openblocks.block.entity.OBImaginaryBlockEntity;
import art.arcane.openblocks.block.entity.OBPlaceholderBlockEntity;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import art.arcane.openblocks.block.entity.OBGuideBlockEntity;
import art.arcane.openblocks.block.entity.OBDrawingTableBlockEntity;
import art.arcane.openblocks.block.entity.OBPaintCanBlockEntity;
import art.arcane.openblocks.block.entity.OBPaintMixerBlockEntity;
import art.arcane.openblocks.block.entity.OBProjectorBlockEntity;
import art.arcane.openblocks.block.entity.OBRedstoneMachineBlockEntity;
import art.arcane.openblocks.block.entity.OBSkyBlockEntity;
import art.arcane.openblocks.block.entity.OBSprinklerBlockEntity;
import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity;
import art.arcane.openblocks.block.entity.OBXPBottlerBlockEntity;
import art.arcane.openblocks.block.entity.OBXPDrainBlockEntity;
import art.arcane.openblocks.block.entity.OBXPShowerBlockEntity;
import art.arcane.openblocks.block.entity.OBXpWorkbenchBlockEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OpenBlocks.MODID);

    private static final List<RegistryObject<BlockEntityType<?>>> ALL = new ArrayList<>();

    public static final RegistryObject<BlockEntityType<OBGuideBlockEntity>> GUIDE = register("guide", (pos, state) -> guide(false, pos, state), OBBlocks.GUIDE);
    public static final RegistryObject<BlockEntityType<OBGuideBlockEntity>> BUILDER_GUIDE = register("builder_guide", (pos, state) -> guide(true, pos, state), OBBlocks.BUILDER_GUIDE);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> ELEVATOR_ROTATING = register("elevator_rotating", OBBlocks.ELEVATOR_ROTATING);
    public static final RegistryObject<BlockEntityType<OBHealBlockEntity>> HEAL = register("heal", OBHealBlockEntity::new, OBBlocks.HEAL);
    public static final RegistryObject<BlockEntityType<OBFoldingTargetBlockEntity>> TARGET = register("target", OBFoldingTargetBlockEntity::new, OBBlocks.TARGET);
    public static final RegistryObject<BlockEntityType<OBGraveBlockEntity>> GRAVE = register("grave", OBGraveBlockEntity::new, OBBlocks.GRAVE);
    public static final RegistryObject<BlockEntityType<OBFlagBlockEntity>> FLAG = register("flag", OBFlagBlockEntity::new, OBBlocks.FLAG);
    public static final RegistryObject<BlockEntityType<OBTankBlockEntity>> TANK = register("tank", OBTankBlockEntity::new, OBBlocks.TANK);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> TROPHY = register("trophy", OBBlocks.TROPHY);
    public static final RegistryObject<BlockEntityType<OBBearTrapBlockEntity>> BEARTRAP = register("beartrap", OBBearTrapBlockEntity::new, OBBlocks.BEARTRAP);
    public static final RegistryObject<BlockEntityType<OBSprinklerBlockEntity>> SPRINKLER = register("sprinkler", OBSprinklerBlockEntity::new, OBBlocks.SPRINKLER);
    public static final RegistryObject<BlockEntityType<OBCannonBlockEntity>> CANNON = register("cannon", OBCannonBlockEntity::new, OBBlocks.CANNON);
    public static final RegistryObject<BlockEntityType<OBVacuumHopperBlockEntity>> VACUUM_HOPPER = register("vacuum_hopper", OBVacuumHopperBlockEntity::new, OBBlocks.VACUUM_HOPPER);
    public static final RegistryObject<BlockEntityType<OBBigButtonBlockEntity>> BIG_BUTTON = register("big_button",
            (pos, state) -> new OBBigButtonBlockEntity(false, pos, state), OBBlocks.BIG_BUTTON);
    public static final RegistryObject<BlockEntityType<OBBigButtonBlockEntity>> BIG_BUTTON_WOOD = register("big_button_wood",
            (pos, state) -> new OBBigButtonBlockEntity(true, pos, state), OBBlocks.BIG_BUTTON_WOOD);
    public static final RegistryObject<BlockEntityType<OBImaginaryBlockEntity>> IMAGINARY = BLOCK_ENTITY_TYPES.register("imaginary",
            () -> BlockEntityType.Builder.of(OBImaginaryBlockEntity::new, OBBlocks.IMAGINARY.get()).build(null));
    public static final RegistryObject<BlockEntityType<OBFanBlockEntity>> FAN = register("fan", OBFanBlockEntity::new, OBBlocks.FAN);
    public static final RegistryObject<BlockEntityType<OBXPBottlerBlockEntity>> XP_BOTTLER = register("xp_bottler", OBXPBottlerBlockEntity::new, OBBlocks.XP_BOTTLER);
    public static final RegistryObject<BlockEntityType<OBPlaceholderBlockEntity>> VILLAGE_HIGHLIGHTER = register("village_highlighter", OBBlocks.VILLAGE_HIGHLIGHTER);
    public static final RegistryObject<BlockEntityType<OBXpWorkbenchBlockEntity>> AUTO_ANVIL = register("auto_anvil",
            (pos, state) -> new OBXpWorkbenchBlockEntity(false, pos, state), OBBlocks.AUTO_ANVIL);
    public static final RegistryObject<BlockEntityType<OBXpWorkbenchBlockEntity>> AUTO_ENCHANTMENT_TABLE = register("auto_enchantment_table",
            (pos, state) -> new OBXpWorkbenchBlockEntity(true, pos, state), OBBlocks.AUTO_ENCHANTMENT_TABLE);
    public static final RegistryObject<BlockEntityType<OBXPDrainBlockEntity>> XP_DRAIN = register("xp_drain", OBXPDrainBlockEntity::new, OBBlocks.XP_DRAIN);
    public static final RegistryObject<BlockEntityType<OBRedstoneMachineBlockEntity>> BLOCK_BREAKER = register("block_breaker",
            (pos, state) -> new OBRedstoneMachineBlockEntity(Kind.BREAKER, pos, state), OBBlocks.BLOCK_BREAKER);
    public static final RegistryObject<BlockEntityType<OBRedstoneMachineBlockEntity>> BLOCK_PLACER = register("block_placer",
            (pos, state) -> new OBRedstoneMachineBlockEntity(Kind.PLACER, pos, state), OBBlocks.BLOCK_PLACER);
    public static final RegistryObject<BlockEntityType<OBRedstoneMachineBlockEntity>> ITEM_DROPPER = register("item_dropper",
            (pos, state) -> new OBRedstoneMachineBlockEntity(Kind.DROPPER, pos, state), OBBlocks.ITEM_DROPPER);
    public static final RegistryObject<BlockEntityType<OBDonationStationBlockEntity>> DONATION_STATION = register("donation_station", OBDonationStationBlockEntity::new, OBBlocks.DONATION_STATION);
    public static final RegistryObject<BlockEntityType<OBPaintMixerBlockEntity>> PAINT_MIXER = register("paint_mixer", OBPaintMixerBlockEntity::new, OBBlocks.PAINT_MIXER);
    public static final RegistryObject<BlockEntityType<OBCanvasBlockEntity>> CANVAS = register("canvas", (pos, state) -> canvas(false, pos, state), OBBlocks.CANVAS);
    public static final RegistryObject<BlockEntityType<OBPaintCanBlockEntity>> PAINT_CAN = register("paint_can", OBPaintCanBlockEntity::new, OBBlocks.PAINT_CAN);
    public static final RegistryObject<BlockEntityType<OBCanvasBlockEntity>> CANVAS_GLASS = register("canvas_glass", (pos, state) -> canvas(true, pos, state), OBBlocks.CANVAS_GLASS);
    public static final RegistryObject<BlockEntityType<OBProjectorBlockEntity>> PROJECTOR = register("projector", OBProjectorBlockEntity::new, OBBlocks.PROJECTOR);
    public static final RegistryObject<BlockEntityType<OBDrawingTableBlockEntity>> DRAWING_TABLE = register("drawing_table", OBDrawingTableBlockEntity::new, OBBlocks.DRAWING_TABLE);
    public static final RegistryObject<BlockEntityType<OBSkyBlockEntity>> SKY = register("sky", OBSkyBlockEntity::new, OBBlocks.SKY);
    public static final RegistryObject<BlockEntityType<OBXPShowerBlockEntity>> XP_SHOWER = register("xp_shower", OBXPShowerBlockEntity::new, OBBlocks.XP_SHOWER);
    public static final RegistryObject<BlockEntityType<OBGoldenEggBlockEntity>> GOLDEN_EGG = BLOCK_ENTITY_TYPES.register("golden_egg",
            () -> BlockEntityType.Builder.of(OBGoldenEggBlockEntity::new, OBBlocks.GOLDEN_EGG.get()).build(null));

    private OBBlockEntities() {}

    private static OBGuideBlockEntity guide(final boolean builder, final net.minecraft.core.BlockPos pos,
            final net.minecraft.world.level.block.state.BlockState state) {
        return new OBGuideBlockEntity((builder ? BUILDER_GUIDE : GUIDE).get(), pos, state);
    }

    private static OBCanvasBlockEntity canvas(final boolean glass, final net.minecraft.core.BlockPos pos,
            final net.minecraft.world.level.block.state.BlockState state) {
        return new OBCanvasBlockEntity((glass ? CANVAS_GLASS : CANVAS).get(), pos, state);
    }

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

    @SafeVarargs
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(final String id,
            final BlockEntityType.BlockEntitySupplier<T> factory, final RegistryObject<Block>... blocks) {
        final RegistryObject<BlockEntityType<T>> holder = BLOCK_ENTITY_TYPES.register(id,
                () -> BlockEntityType.Builder.of(factory, resolveBlocks(blocks)).build(null));
        ALL.add((RegistryObject) holder);
        return holder;
    }

    private static Block[] resolveBlocks(final RegistryObject<Block>[] blocks) {
        final Block[] resolved = new Block[blocks.length];
        for (int i = 0; i < blocks.length; i++) resolved[i] = blocks[i].get();
        return resolved;
    }
}
