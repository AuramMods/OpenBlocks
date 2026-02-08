package art.arcane.openblocks;

import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBCreativeTabs;
import art.arcane.openblocks.registry.OBEnchantments;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBFluidTypes;
import art.arcane.openblocks.registry.OBFluids;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBMenuTypes;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import art.arcane.openblocks.registry.OBSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OpenBlocks.MODID)
public class OpenBlocks {

    public static final String MODID = "open_blocks";

    public OpenBlocks(final FMLJavaModLoadingContext context) {
        final IEventBus modEventBus = context.getModEventBus();

        OBFluidTypes.FLUID_TYPES.register(modEventBus);
        OBFluids.FLUIDS.register(modEventBus);
        OBFluids.FLUID_BLOCKS.register(modEventBus);

        OBBlocks.BLOCKS.register(modEventBus);
        OBItems.ITEMS.register(modEventBus);
        OBCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        OBBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        OBEntities.ENTITY_TYPES.register(modEventBus);
        OBSounds.SOUND_EVENTS.register(modEventBus);
        OBEnchantments.ENCHANTMENTS.register(modEventBus);
        OBMenuTypes.MENU_TYPES.register(modEventBus);
        OBRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
    }
}
