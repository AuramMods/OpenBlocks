package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBFlagBlock;
import art.arcane.openblocks.item.OBFlagItem;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBFlagRendering {

    private OBFlagRendering() {}

    @SubscribeEvent
    public static void registerBlocks(final RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 0 ? rgb(state.getValue(OBFlagBlock.COLOR)) : -1, OBBlocks.FLAG.get());
    }

    @SubscribeEvent
    public static void registerItems(final RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? rgb(OBFlagItem.getColor(stack)) : -1, OBItems.FLAG.get());
    }

    @SubscribeEvent
    public static void registerRenderer(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.FLAG.get(), context -> (flag, partial, pose, buffers, light, overlay) -> {
            final var state = flag.getBlockState();
            if (state.getValue(OBFlagBlock.FACING) != Direction.UP) return;

            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(180 - flag.yaw()));
            pose.translate(-0.5, 0, -0.5);
            final float[] color = state.getValue(OBFlagBlock.COLOR).getTextureDiffuseColors();
            final var renderer = Minecraft.getInstance().getBlockRenderer();
            renderer.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), state,
                    renderer.getBlockModel(state), color[0], color[1], color[2], light, overlay, ModelData.EMPTY, RenderType.cutout());
            pose.popPose();
        });
    }

    private static int rgb(final DyeColor dye) {
        final float[] color = dye.getTextureDiffuseColors();
        return FastColor.ARGB32.color(255, Math.round(color[0] * 255), Math.round(color[1] * 255), Math.round(color[2] * 255));
    }
}
