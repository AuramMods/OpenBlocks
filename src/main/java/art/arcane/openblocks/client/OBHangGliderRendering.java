package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBHangGlider;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class OBHangGliderRendering {

    private static final ResourceLocation TEXTURE = OpenBlocks.id("textures/model/hang_glider.png");
    private static final float HALF_WING = 2.4F;

    private OBHangGliderRendering() {}

    @Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Models {

        @SubscribeEvent
        public static void registerRenderer(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(OBEntities.HANG_GLIDER.get(), GliderRenderer::new);
        }

        @SubscribeEvent
        public static void clientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(OBItems.HANG_GLIDER.get(), OpenBlocks.id("armed"),
                    (stack, level, holder, seed) -> holder instanceof Player player && OBHangGlider.isHeldGlider(player, stack) ? 1 : 0));
        }
    }

    @Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT)
    public static final class PlayerPose {

        @SubscribeEvent
        public static void posePlayer(final RenderPlayerEvent.Pre event) {
            final Player player = event.getEntity();
            final OBHangGlider glider = OBHangGlider.activeFor(player);
            if (glider == null || !glider.isFlying()) return;

            player.walkAnimation.setSpeed(0);
            player.walkAnimation.update(0, 1);
            final float yaw = 180 - Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
            final PoseStack pose = event.getPoseStack();
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.XP.rotationDegrees(-75));
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        }
    }

    private static final class GliderRenderer extends EntityRenderer<OBHangGlider> {

        private GliderRenderer(final EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public boolean shouldRender(final OBHangGlider glider, final Frustum frustum, final double x, final double y, final double z) {
            final Player player = glider.holder();
            return player != null && frustum.isVisible(player.getBoundingBox().inflate(HALF_WING + 0.6));
        }

        @Override
        public void render(final OBHangGlider glider, final float yaw, final float partial, final PoseStack pose,
                final MultiBufferSource buffers, final int light) {
            final Player player = glider.holder();
            if (player == null || OBHangGlider.activeFor(player) != glider
                    || !OBHangGlider.isHeldGlider(player, player.getItemInHand(glider.hand()))) return;
            final Minecraft minecraft = Minecraft.getInstance();
            final boolean firstPerson = player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
            final boolean flying = glider.isFlying();
            if (firstPerson && !flying) return;

            pose.pushPose();
            pose.translate(Mth.lerp(partial, player.xOld - glider.xOld, player.getX() - glider.getX()),
                    Mth.lerp(partial, player.yOld - glider.yOld, player.getY() - glider.getY()) + OBHangGlider.HEIGHT_ABOVE_FEET,
                    Mth.lerp(partial, player.zOld - glider.zOld, player.getZ() - glider.getZ()));
            pose.mulPose(Axis.YP.rotationDegrees(180 - Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot)));
            if (firstPerson) {
                pose.translate(0, 0.7, 0);
            } else if (flying) {
                pose.translate(0, -0.5, -1);
            } else {
                pose.translate(0, 0.2, 0.3);
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.scale(0.4F, 1, 0.4F);
            }
            final VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            final PoseStack.Pose matrices = pose.last();
            vertex(vertices, matrices, HALF_WING, HALF_WING, 1, 1, light);
            vertex(vertices, matrices, -HALF_WING, HALF_WING, 0, 1, light);
            vertex(vertices, matrices, -HALF_WING, -HALF_WING, 0, 0, light);
            vertex(vertices, matrices, HALF_WING, -HALF_WING, 1, 0, light);
            pose.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(final OBHangGlider glider) {
            return TEXTURE;
        }

        private static void vertex(final VertexConsumer vertices, final PoseStack.Pose matrices,
                final float x, final float z, final int u, final int v, final int light) {
            vertices.vertex(matrices.pose(), x, 0, z).color(255, 255, 255, 255).uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(matrices.normal(), 0, 1, 0).endVertex();
        }
    }
}
