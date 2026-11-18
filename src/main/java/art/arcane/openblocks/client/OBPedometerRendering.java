package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBPedometerRendering {

    private OBPedometerRendering() {}

    @SubscribeEvent
    public static void register(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(OBItems.PEDOMETER.get(), OpenBlocks.id("speed"),
                (stack, level, holder, seed) -> holder == null ? 0 : (float) holder.getDeltaMovement().length()));
    }
}
