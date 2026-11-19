package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBGraveBlock;
import art.arcane.openblocks.registry.OBBlockEntities;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OBGraveRendering {

    private OBGraveRendering() {}

    @SubscribeEvent
    public static void registerGraveRenderer(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.GRAVE.get(), context -> (grave, partial, poses, buffers, light, overlay) -> {
            final String text = grave.epitaph();
            final Font font = context.getFont();
            final int width = font.width(text);
            if (width == 0) return;

            final var state = grave.getBlockState();
            final float scale = Math.min(0.02F, 0.5F / width);
            poses.pushPose();
            poses.translate(0.5, state.getValue(OBGraveBlock.BASE) ? 10.0 / 16 : 9.0 / 16, 0.5);
            poses.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(OBGraveBlock.FACING).toYRot()));
            poses.translate(0, 0, 0.375 - 0.001);
            poses.scale(-scale, -scale, scale);
            font.drawInBatch(text, -width / 2F, -font.lineHeight / 2F, 0xFF000000, false,
                    poses.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, light);
            poses.popPose();
        });
    }
}
