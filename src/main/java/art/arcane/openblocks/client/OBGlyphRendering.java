package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBGlyph;
import art.arcane.openblocks.item.OBGlyphItem;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class OBGlyphRendering {

    private static final ResourceLocation PLATE = OpenBlocks.id("item/glyph_plate");

    private OBGlyphRendering() {}

    @Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void models(final ModelEvent.RegisterAdditional event) { event.register(PLATE); }

        @SubscribeEvent
        public static void entities(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(OBEntities.GLYPH.get(), GlyphRenderer::new);
        }
    }

    private static void drawGlyph(final int index, final float size, final PoseStack poses, final MultiBufferSource buffers,
            final int light, final int overlay) {
        final Minecraft client = Minecraft.getInstance();
        poses.pushPose();
        poses.scale(size, size, size);
        poses.pushPose();
        poses.translate(-0.5, -0.5, -0.5);
        client.getBlockRenderer().getModelRenderer().renderModel(poses.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()),
                Blocks.AIR.defaultBlockState(), client.getModelManager().getModel(PLATE), 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
        poses.popPose();

        final Font font = client.font;
        final String text = String.valueOf(OBGlyphItem.character(index));
        poses.translate(0, 0, 0.033);
        poses.scale(0.109375F, -0.109375F, 0.109375F);
        font.drawInBatch(text, -font.width(text) * 0.5F, -4F, 0xFF000000, false, poses.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
        poses.popPose();
    }

    private static final class GlyphRenderer extends EntityRenderer<OBGlyph> {

        private GlyphRenderer(final EntityRendererProvider.Context context) { super(context); }

        @Override
        public void render(final OBGlyph glyph, final float yaw, final float partial, final PoseStack poses, final MultiBufferSource buffers, final int light) {
            poses.pushPose();
            poses.mulPose(Axis.YP.rotationDegrees(-glyph.getDirection().toYRot()));
            drawGlyph(glyph.glyph(), 0.5F, poses, buffers, light, OverlayTexture.NO_OVERLAY);
            poses.popPose();
            super.render(glyph, yaw, partial, poses, buffers, light);
        }

        @Override public ResourceLocation getTextureLocation(final OBGlyph glyph) { return TextureAtlas.LOCATION_BLOCKS; }
    }

    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {

        public ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(final ItemStack stack, final ItemDisplayContext context, final PoseStack poses,
                final MultiBufferSource buffers, final int light, final int overlay) {
            poses.pushPose();
            poses.translate(0.5, 0.5, 0.5);
            drawGlyph(OBGlyphItem.index(stack), 1F, poses, buffers, light, overlay);
            poses.popPose();
        }
    }

    @Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT)
    public static final class PlacementGrid {

        @SubscribeEvent
        public static void highlight(final RenderHighlightEvent.Block event) {
            final var player = Minecraft.getInstance().player;
            if (player == null || !player.getMainHandItem().is(OBItems.GLYPH.get())) return;
            final var hit = event.getTarget();
            final Direction face = hit.getDirection();
            if (!face.getAxis().isHorizontal()) return;
            final var block = hit.getBlockPos();
            final Vec3 camera = event.getCamera().getPosition();
            final Vec3 offset = hit.getLocation().subtract(Vec3.atLowerCornerOf(block));
            final boolean wallAlongX = face.getAxis() == Direction.Axis.Z;
            final double depth = (wallAlongX ? offset.z : offset.x) + (wallAlongX ? face.getStepZ() : face.getStepX()) * 0.002;
            final double center = OBGlyphItem.pixel(wallAlongX ? offset.x : offset.z) / 16.0;
            final double height = OBGlyphItem.pixel(offset.y) / 16.0;

            final PoseStack poses = event.getPoseStack();
            poses.pushPose();
            poses.translate(block.getX() - camera.x, block.getY() - camera.y, block.getZ() - camera.z);
            final var lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
            for (int i = 0; i <= 16; i++) {
                final double step = i / 16.0;
                if (wallAlongX) {
                    line(poses.last(), lines, step, 0, depth, step, 1, depth);
                    line(poses.last(), lines, 0, step, depth, 1, step, depth);
                } else {
                    line(poses.last(), lines, depth, 0, step, depth, 1, step);
                    line(poses.last(), lines, depth, step, 0, depth, step, 1);
                }
            }
            if (wallAlongX) {
                LevelRenderer.renderLineBox(poses, lines, center - 0.25, height - 0.25, depth, center + 0.25, height + 0.25, depth,
                        1, 1, 1, 0.85F);
            } else {
                LevelRenderer.renderLineBox(poses, lines, depth, height - 0.25, center - 0.25, depth, height + 0.25, center + 0.25,
                        1, 1, 1, 0.85F);
            }
            poses.popPose();
        }

        private static void line(final PoseStack.Pose pose, final VertexConsumer lines, final double x, final double y, final double z,
                final double nextX, final double nextY, final double nextZ) {
            final float nx = (float) (nextX - x);
            final float ny = (float) (nextY - y);
            final float nz = (float) (nextZ - z);
            lines.vertex(pose.pose(), (float) x, (float) y, (float) z).color(0.65F, 0.65F, 0.65F, 0.5F).normal(pose.normal(), nx, ny, nz).endVertex();
            lines.vertex(pose.pose(), (float) nextX, (float) nextY, (float) nextZ).color(0.65F, 0.65F, 0.65F, 0.5F).normal(pose.normal(), nx, ny, nz).endVertex();
        }
    }
}
