package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBXpWorkbenchRendering {

    private OBXpWorkbenchRendering() {}

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.AUTO_ENCHANTMENT_TABLE.get(), context -> {
            final var renderer = new EnchantTableRenderer(context);
            return (machine, partial, pose, buffers, light, overlay) -> {
                final var book = machine.animatedBook();
                if (book != null) renderer.render(book, partial, pose, buffers, light, overlay);
            };
        });
    }
}
