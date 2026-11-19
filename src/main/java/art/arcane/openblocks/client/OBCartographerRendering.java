package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBCartographer;
import art.arcane.openblocks.registry.OBEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.BitSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBCartographerRendering {

    private static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.id("cartographer"), "main");
    private static final ResourceLocation TEXTURE = OpenBlocks.id("textures/model/cartographer.png");

    private OBCartographerRendering() {}

    @SubscribeEvent
    public static void registerLayer(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, () -> {
            final var mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.5F, -1.5F, -2.5F, 5, 2, 5), PartPose.ZERO);
            mesh.getRoot().addOrReplaceChild("holder", CubeListBuilder.create()
                    .texOffs(0, 7).addBox(-1.5F, 0.5F, -1.5F, 3, 1, 3)
                    .texOffs(0, 11).addBox(-0.5F, 0.5F, -2.5F, 1, 4, 1)
                    .texOffs(4, 11).addBox(-0.5F, 0.5F, 1.5F, 1, 4, 1), PartPose.ZERO);
            return LayerDefinition.create(mesh, 32, 32);
        });
    }

    @SubscribeEvent
    public static void registerRenderer(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OBEntities.CARTOGRAPHER.get(), Renderer::new);
    }

    private static final class Renderer extends EntityRenderer<OBCartographer> {
        private static final float HALF_PANEL = 0.16F;
        private static final float PANEL_Z = -0.29F;
        private final ItemStack eye = new ItemStack(Items.ENDER_EYE);
        private final ItemRenderer itemRenderer;
        private final ModelPart model;
        private final ModelPart holder;

        private Renderer(final EntityRendererProvider.Context context) {
            super(context);
            this.itemRenderer = context.getItemRenderer();
            model = context.bakeLayer(LAYER);
            holder = model.getChild("holder");
        }

        @Override
        public void render(final OBCartographer cartographer, final float yaw, final float partial, final PoseStack poses,
                final MultiBufferSource buffers, final int light) {
            final float age = cartographer.tickCount + partial;
            final float rotation = Mth.sin(age * 0.052F) * 2.25F;
            final float pitch = Mth.sin(age * 0.083F) * 23F;
            final float bob = Mth.sin(age * 0.16F) * 0.024F;

            poses.pushPose();
            poses.translate(0, 0.097F + bob, 0);
            holder.yRot = rotation;
            model.render(poses, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            poses.translate(0, 0.18F, 0);
            poses.mulPose(Axis.YP.rotation(rotation + Mth.HALF_PI));
            poses.mulPose(Axis.XP.rotationDegrees(pitch));
            poses.scale(0.23F, 0.23F, 0.23F);
            itemRenderer.renderStatic(eye, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poses, buffers,
                    cartographer.level(), cartographer.getId());
            poses.popPose();

            if (Minecraft.getInstance().hitResult instanceof EntityHitResult hit && hit.getEntity() == cartographer) {
                drawStatus(cartographer, 0.28F + bob, poses, buffers);
            } else {
                super.render(cartographer, yaw, partial, poses, buffers, light);
            }
        }

        private void drawStatus(final OBCartographer cartographer, final float height, final PoseStack poses, final MultiBufferSource buffers) {
            poses.pushPose();
            poses.translate(0, height, 0);
            poses.mulPose(entityRenderDispatcher.cameraOrientation());
            final Matrix4f matrix = poses.last().pose();
            final VertexConsumer vertices = buffers.getBuffer(RenderType.debugQuads());
            final float edge = HALF_PANEL;
            coneFace(vertices, matrix, -edge, -edge, -edge, edge);
            coneFace(vertices, matrix, -edge, edge, edge, edge);
            coneFace(vertices, matrix, edge, edge, edge, -edge);
            coneFace(vertices, matrix, edge, -edge, -edge, -edge);
            quad(vertices, matrix, -edge, -edge, edge, edge, PANEL_Z, 0xEE113D3F);

            final var progress = cartographer.progress();
            final int size = progress.getInt("Size");
            if (size > 0 && size <= 64) {
                final BitSet chunks = BitSet.valueOf(progress.getLongArray("Chunks"));
                final float step = 2 * edge / size;
                for (int z = 0; z < size; z++) {
                    final int end = (z + 1) * size;
                    int start = chunks.nextSetBit(z * size);
                    while (start >= 0 && start < end) {
                        final int stop = Math.min(chunks.nextClearBit(start), end);
                        final float x0 = edge - (stop - z * size) * step;
                        final float x1 = edge - (start - z * size) * step;
                        final float y = edge - z * step;
                        quad(vertices, matrix, x0, y - step, x1, y, PANEL_Z - 0.001F, 0xFF3AFA61);
                        start = chunks.nextSetBit(stop);
                    }
                }
            } else {
                final String coords = cartographer.centerX() + ", " + cartographer.centerZ();
                final Font font = getFont();
                final float scale = Math.min(0.017F, 0.29F / Math.max(1, font.width(coords)));
                poses.translate(0, 0, PANEL_Z - 0.002F);
                poses.scale(-scale, -scale, scale);
                font.drawInBatch(coords, -font.width(coords) / 2F, -font.lineHeight / 2F, 0xFFBBFFF4, false,
                        poses.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
            poses.popPose();
        }

        private static void coneFace(final VertexConsumer vertices, final Matrix4f matrix, final float x0, final float y0,
                final float x1, final float y1) {
            vertex(vertices, matrix, 0, 0, -0.03F, 0x3040FFE5);
            vertex(vertices, matrix, x0, y0, PANEL_Z, 0x3040FFE5);
            vertex(vertices, matrix, x1, y1, PANEL_Z, 0x3040FFE5);
            vertex(vertices, matrix, 0, 0, -0.03F, 0x3040FFE5);
        }

        private static void quad(final VertexConsumer vertices, final Matrix4f matrix, final float x0, final float y0,
                final float x1, final float y1, final float z, final int argb) {
            vertex(vertices, matrix, x0, y0, z, argb);
            vertex(vertices, matrix, x1, y0, z, argb);
            vertex(vertices, matrix, x1, y1, z, argb);
            vertex(vertices, matrix, x0, y1, z, argb);
        }

        private static void vertex(final VertexConsumer vertices, final Matrix4f matrix, final float x, final float y,
                final float z, final int argb) {
            vertices.vertex(matrix, x, y, z).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).endVertex();
        }

        @Override
        public ResourceLocation getTextureLocation(final OBCartographer cartographer) {
            return TEXTURE;
        }
    }
}
