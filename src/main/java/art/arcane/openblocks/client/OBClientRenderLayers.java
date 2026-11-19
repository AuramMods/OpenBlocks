package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBFluids;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBClientRenderLayers {

    private OBClientRenderLayers() {}

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            setCutout(OBBlocks.XP_DRAIN);
            setCutout(OBBlocks.SPRINKLER);
            setCutout(OBBlocks.FLAG);

            ItemBlockRenderTypes.setRenderLayer(OBFluids.XP_JUICE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(OBFluids.FLOWING_XP_JUICE.get(), RenderType.translucent());
        });
    }

    private static void setCutout(final RegistryObject<Block> block) {
        ItemBlockRenderTypes.setRenderLayer(block.get(), renderType -> renderType == RenderType.cutout());
    }
}
