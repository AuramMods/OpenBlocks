package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBGoldenEggBlockEntity;
import art.arcane.openblocks.entity.OBMiniMe;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBGoldenEggRendering {

    private OBGoldenEggRendering() {}

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.GOLDEN_EGG.get(), context -> new EggRenderer());
        event.registerEntityRenderer(OBEntities.MINI_ME.get(), MiniMeRenderer::new);
    }

    private static final class EggRenderer implements BlockEntityRenderer<OBGoldenEggBlockEntity> {
        private static final float[] BRIGHTNESS = {1, 1, 1, 1};
        private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
        private static final int[] AURA_LIGHT = {FULL_BRIGHT, FULL_BRIGHT, FULL_BRIGHT, FULL_BRIGHT};
        private List<BakedQuad> quads;

        @Override
        public void render(final OBGoldenEggBlockEntity egg, final float partial, final PoseStack pose, final MultiBufferSource buffers,
                final int light, final int overlay) {
            if (quads == null) {
                final var state = egg.getBlockState();
                final var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
                final var random = RandomSource.create(0);
                quads = new ArrayList<>(model.getQuads(state, null, random));
                for (final Direction direction : Direction.values()) quads.addAll(model.getQuads(state, direction, random));
            }

            final float rotation = egg.rotation(partial);
            final float progress = egg.progress(partial);
            pose.pushPose();
            pose.translate(0.5, egg.offset(partial), 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            pose.pushPose();
            pose.translate(-0.5, 0, -0.5);
            final var solid = buffers.getBuffer(Sheets.solidBlockSheet());
            for (final var quad : quads) solid.putBulkData(pose.last(), quad, 1, 1, 1, light, overlay);
            pose.popPose();

            if (egg.stage().hasAura()) {
                pose.pushPose();
                final float alpha = 0.2F + 0.8F * progress;
                final float scale = 1.5F * alpha;
                pose.translate(0, -0.1F * progress, 0);
                pose.scale(scale, scale, scale);
                pose.translate(-0.5, 0, -0.5);
                final var phantom = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
                for (final var quad : quads) {
                    phantom.putBulkData(pose.last(), quad, BRIGHTNESS, 1, 1, 1, alpha * 0.55F, AURA_LIGHT, overlay, false);
                }
                pose.popPose();
                renderStar(pose, buffers.getBuffer(RenderType.lightning()), rotation, progress);
            }
            pose.popPose();
        }

        private static void renderStar(final PoseStack pose, final VertexConsumer out, final float rotation, final float progress) {
            pose.pushPose();
            pose.translate(0, 0.55, 0);
            pose.mulPose(Axis.YP.rotationDegrees(-0.2F * rotation));
            final var random = RandomSource.create(432L);
            final float fade = Math.max(0, (progress - 0.8F) / 0.2F);
            final int alpha = (int) (192 * (1 - fade));
            for (int ray = 0; ray < 30 * (progress + progress * progress); ray++) {
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360 + progress * 90));
                final float length = random.nextFloat() * 10 + 5 + fade * 10;
                final float width = random.nextFloat() * 2 + 1 + fade * 2;
                final var matrix = pose.last().pose();
                beam(out, matrix, alpha, length, -0.866F * width, -0.5F * width, 0.866F * width, -0.5F * width);
                beam(out, matrix, alpha, length, 0.866F * width, -0.5F * width, 0, width);
                beam(out, matrix, alpha, length, 0, width, -0.866F * width, -0.5F * width);
            }
            pose.popPose();
        }

        private static void beam(final VertexConsumer out, final Matrix4f matrix, final int alpha, final float length,
                final float x1, final float z1, final float x2, final float z2) {
            out.vertex(matrix, 0, 0, 0).color(255, 255, 255, alpha).endVertex();
            out.vertex(matrix, x1, length, z1).color(255, 0, 255, 0).endVertex();
            out.vertex(matrix, x2, length, z2).color(255, 0, 255, 0).endVertex();
            out.vertex(matrix, x2, length, z2).color(255, 0, 255, 0).endVertex();
        }
    }

    private static final class MiniMeRenderer extends MobRenderer<OBMiniMe, PlayerModel<OBMiniMe>> {
        private MiniMeRenderer(final EntityRendererProvider.Context context) {
            super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(final OBMiniMe mini) {
            final var appearance = mini.owner();
            return appearance == null ? DefaultPlayerSkin.getDefaultSkin()
                    : Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(appearance);
        }
    }
}
