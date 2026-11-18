package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBClientConfig;
import art.arcane.openblocks.item.OBDevNullItem;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBDevNullRendering extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation FRAME = OpenBlocks.id("block/dev_null_2d");
    private static final ResourceLocation OVERFLOW = OpenBlocks.id("block/dev_null_2d_overflow");
    private static final ResourceLocation FRAME_3D = OpenBlocks.id("block/dev_null_3d");
    private static final ResourceLocation OVERFLOW_3D = OpenBlocks.id("block/dev_null_3d_overflow");
    private static final ResourceLocation WALL = OpenBlocks.id("item/dev_full");
    private static final ResourceLocation OVERFLOW_WALL = OpenBlocks.id("item/dev_zero_over_zero");
    private static final float NESTING_SCALE = 0.875F;
    private static final Direction[] FACES = Direction.values();

    public OBDevNullRendering() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @SubscribeEvent
    public static void registerModels(final ModelEvent.RegisterAdditional event) {
        event.register(FRAME);
        event.register(OVERFLOW);
        event.register(FRAME_3D);
        event.register(OVERFLOW_3D);
    }

    @SubscribeEvent
    public static void registerCount(final RegisterItemDecorationsEvent event) {
        event.register(OBItems.DEV_NULL.get(), (graphics, font, stack, x, y) -> {
            final int count = OBDevNullItem.contents(stack).item().getCount();
            if (count <= 1) return false;
            final String label = Integer.toString(count);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.drawString(font, label, x + 17 - font.width(label), y + 9, OBClientConfig.DEV_NULL_COUNT_COLOR.get(), true);
            graphics.pose().popPose();
            return true;
        });
    }

    @Override
    public void renderByItem(final ItemStack stack, final ItemDisplayContext context, final PoseStack pose,
            final MultiBufferSource buffers, final int light, final int overlay) {
        final var contents = OBDevNullItem.contents(stack);
        final boolean gui = context == ItemDisplayContext.GUI;
        final boolean overflow = contents.depth() == OBDevNullItem.STACK_LIMIT;

        for (int depth = 0; depth < contents.depth(); depth++) {
            renderFrame(stack, pose, buffers, light, overlay, gui, depth, false);
        }
        if (overflow) renderFrame(stack, pose, buffers, light, overlay, gui, contents.depth(), true);
        if (contents.item().isEmpty()) return;

        final var client = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(0.5, 0.5, gui ? 0.5 + contents.depth() * 0.025 : 0.5);
        final float scale = (float) Math.pow(NESTING_SCALE, contents.depth());
        pose.scale(scale, scale, scale);
        client.getItemRenderer().renderStatic(contents.item(), gui ? ItemDisplayContext.GUI : ItemDisplayContext.NONE,
                light, overlay, pose, buffers, client.level, 0);
        pose.popPose();
    }

    private static void renderFrame(final ItemStack stack, final PoseStack pose, final MultiBufferSource buffers,
            final int light, final int overlay, final boolean gui, final int depth, final boolean overflow) {
        pose.pushPose();
        final float scale = (float) Math.pow(NESTING_SCALE, depth);
        pose.translate(0.5, 0.5, gui ? 0.5 + depth * 0.025 : 0.5);
        pose.scale(scale, scale, gui ? 1 : scale);
        final var client = Minecraft.getInstance();
        if (gui) {
            client.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, overlay,
                    client.getModelManager().getModel(overflow ? OVERFLOW : FRAME));
        } else {
            pose.translate(-0.5, -0.5, -0.5);
            renderInsideFaces(pose, buffers, light, overlay, overflow ? OVERFLOW_WALL : WALL);
        }
        pose.popPose();
    }

    private static void renderInsideFaces(final PoseStack pose, final MultiBufferSource buffers,
            final int light, final int overlay, final ResourceLocation texture) {
        final var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        final var vertices = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        final var transform = pose.last();
        for (final Direction face : FACES) {
            final var info = FaceInfo.fromFacing(face);
            final var inward = face.getOpposite();
            // Reverse the winding so the near walls are culled and the contents and far walls stay visible.
            for (int i = 3; i >= 0; i--) {
                final var corner = info.getVertexInfo(i);
                vertices.vertex(transform.pose(), corner.xFace == FaceInfo.Constants.MAX_X ? 1 : 0,
                                corner.yFace == FaceInfo.Constants.MAX_Y ? 1 : 0, corner.zFace == FaceInfo.Constants.MAX_Z ? 1 : 0)
                        .color(255, 255, 255, 255)
                        .uv(i < 2 ? sprite.getU0() : sprite.getU1(), i == 0 || i == 3 ? sprite.getV0() : sprite.getV1())
                        .overlayCoords(overlay).uv2(light)
                        .normal(transform.normal(), inward.getStepX(), inward.getStepY(), inward.getStepZ()).endVertex();
            }
        }
    }
}
