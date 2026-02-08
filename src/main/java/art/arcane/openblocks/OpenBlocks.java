package art.arcane.openblocks;

import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBCreativeTabs;
import art.arcane.openblocks.registry.OBItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OpenBlocks.MODID)
public class OpenBlocks {

    public static final String MODID = "open_blocks";

    public OpenBlocks(final FMLJavaModLoadingContext context) {
        final IEventBus modEventBus = context.getModEventBus();
        OBBlocks.BLOCKS.register(modEventBus);
        OBItems.ITEMS.register(modEventBus);
        OBCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
    }
}
