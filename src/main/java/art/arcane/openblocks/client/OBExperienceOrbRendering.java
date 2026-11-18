package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBEntities;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBExperienceOrbRendering {

    private OBExperienceOrbRendering() {}

    @SubscribeEvent
    public static void register(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OBEntities.XP_ORB_NO_FLY.get(), ExperienceOrbRenderer::new);
    }
}
