package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import art.arcane.openblocks.block.entity.OBPaintCanBlockEntity;
import art.arcane.openblocks.block.entity.OBPaintMixerBlockEntity;
import art.arcane.openblocks.item.OBPaintBrushItem;
import art.arcane.openblocks.item.OBPaintCanItem;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBMenuTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBPaintRendering {

    private static final ResourceLocation MIXER_CAN = OpenBlocks.id("block/paint_mixer_can");

    private OBPaintRendering() {}

    @SubscribeEvent
    public static void registerModels(final ModelEvent.RegisterAdditional event) {
        event.register(MIXER_CAN);
    }

    @SubscribeEvent
    public static void registerBlocks(final RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 1 && level != null && pos != null
                && level.getBlockEntity(pos) instanceof OBPaintCanBlockEntity can ? can.color() : -1, OBBlocks.PAINT_CAN.get());
    }

    @SubscribeEvent
    public static void registerItems(final RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 1 ? OBPaintCanItem.color(stack) : -1, OBItems.PAINT_CAN.get());
        event.register((stack, tint) -> {
            final int color = OBPaintBrushItem.color(stack);
            return tint == 1 && color != OBCanvasBlockEntity.UNPAINTED ? color : -1;
        }, OBItems.PAINTBRUSH.get());
    }

    @SubscribeEvent
    public static void registerScreen(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(OBMenuTypes.PAINT_MIXER.get(), OBPaintMixerScreen::new));
    }

    @SubscribeEvent
    public static void registerRenderer(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.PAINT_MIXER.get(), context -> OBPaintRendering::renderMixer);
    }

    private static void renderMixer(final OBPaintMixerBlockEntity mixer, final float partial, final PoseStack pose,
            final MultiBufferSource buffers, final int light, final int overlay) {
        if (!mixer.hasCan() || mixer.getLevel() == null) return;
        final var state = mixer.getBlockState();
        final var client = Minecraft.getInstance();
        final int rgb = mixer.animatedColor(partial);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
        if (mixer.mixing()) {
            final float time = ((mixer.getLevel().getGameTime() % 80) + partial) * Mth.TWO_PI / 80;
            pose.translate(0, 0.2F * Math.abs(Mth.sin(time * 6)), 0.125);
            pose.mulPose(Axis.YP.rotationDegrees(10 * Mth.sin(time)));
            pose.translate(0, 0, -0.125);
        }
        pose.translate(-0.5, 0, -0.5);
        client.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), state,
                client.getModelManager().getModel(MIXER_CAN), ((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F,
                (rgb & 255) / 255F, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }
}
