package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBMagnet;
import art.arcane.openblocks.entity.OBMountedBlock;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBCraneRendering {

    private static final ModelLayerLocation BACKPACK_LAYER = layer("backpack");
    private static final ModelLayerLocation BOOM_LAYER = layer("boom");
    private static final ModelLayerLocation MAGNET_LAYER = layer("magnet");
    private static final ResourceLocation CRANE = OpenBlocks.id("textures/model/crane.png");
    private static final ResourceLocation MAGNET = OpenBlocks.id("textures/model/magnet.png");
    private static final double BOOM_LENGTH = 42 / 16D;

    private OBCraneRendering() {}

    @SubscribeEvent
    public static void registerLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BACKPACK_LAYER, () -> {
            final var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
            mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, 0, -2, 8, 12, 8)
                    .texOffs(32, 0).addBox(-1, -16, 6, 2, 24, 2), PartPose.ZERO);
            return LayerDefinition.create(mesh, 128, 64);
        });
        event.registerLayerDefinition(BOOM_LAYER, () -> {
            final var mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("boom", CubeListBuilder.create().texOffs(0, 0).addBox(-1, 0, 0, 2, 2, 42), PartPose.ZERO);
            return LayerDefinition.create(mesh, 128, 64);
        });
        event.registerLayerDefinition(MAGNET_LAYER, () -> {
            final var mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, 0, -3, 6, 1, 6)
                    .texOffs(0, 7).addBox(-2, 1, -2, 4, 1, 4).texOffs(0, 12).addBox(-1, 2, -1, 2, 1, 2), PartPose.ZERO);
            return LayerDefinition.create(mesh, 32, 32);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OBEntities.PLAYER_MAGNET.get(), MagnetRenderer::new);
        event.registerEntityRenderer(OBEntities.MAGNET.get(), MagnetRenderer::new);
        event.registerEntityRenderer(OBEntities.MOUNTED_BLOCK.get(), CargoRenderer::new);
    }

    @SubscribeEvent
    public static void registerControlModels(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(OBItems.CRANE_CONTROL.get(), OpenBlocks.id("lift"), (stack, level, entity, seed) -> {
                final OBMagnet magnet = entity instanceof Player player ? OBMagnet.forPlayer(player) : null;
                return magnet == null || entity.getUseItem() != stack ? 0 : magnet.liftState() * 0.5F;
            });
            ItemProperties.register(OBItems.CRANE_CONTROL.get(), OpenBlocks.id("magnet"), (stack, level, entity, seed) -> {
                final OBMagnet magnet = entity instanceof Player player ? OBMagnet.forPlayer(player) : null;
                return magnet == null ? 0 : magnet.magnetState() * 0.5F;
            });
        });
    }

    public static IClientItemExtensions backpackExtension() {
        return new IClientItemExtensions() {
            private HumanoidModel<LivingEntity> model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(final LivingEntity wearer, final ItemStack stack, final EquipmentSlot slot, final HumanoidModel<?> original) {
                if (model == null) model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(BACKPACK_LAYER)) {
                    @Override
                    public void renderToBuffer(final PoseStack pose, final VertexConsumer vertices, final int light, final int overlay,
                            final float red, final float green, final float blue, final float alpha) {
                        body.render(pose, vertices, light, overlay, red, green, blue, alpha);
                    }
                };
                return model;
            }
        };
    }

    private static ModelLayerLocation layer(final String part) {
        return new ModelLayerLocation(OpenBlocks.id("crane"), part);
    }

    private static final class MagnetRenderer extends EntityRenderer<OBMagnet> {
        private final ModelPart head;
        private final ModelPart boom;

        private MagnetRenderer(final EntityRendererProvider.Context context) {
            super(context);
            head = context.bakeLayer(MAGNET_LAYER);
            boom = context.bakeLayer(BOOM_LAYER);
        }

        @Override
        public boolean shouldRender(final OBMagnet magnet, final Frustum frustum, final double x, final double y, final double z) {
            final Player wearer = magnet.holder();
            return super.shouldRender(magnet, frustum, x, y, z) || wearer != null
                    && frustum.isVisible(magnet.getBoundingBox().minmax(wearer.getBoundingBox()).inflate(2.5));
        }

        @Override
        public void render(final OBMagnet magnet, final float entityYaw, final float partial, final PoseStack pose,
                final MultiBufferSource buffers, final int light) {
            pose.pushPose();
            pose.translate(0, 0.005, 0);
            pose.scale(2, 2, 2);
            head.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(MAGNET)), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();

            final Player wearer = magnet.holder();
            if (wearer == null || !OBMagnet.wearsBackpack(wearer)) return;
            final float yaw = Mth.rotLerp(partial, wearer.yRotO, wearer.getYRot());
            final Vec3 forward = Vec3.directionFromRotation(0, yaw);
            final Vec3 pivot = new Vec3(Mth.lerp(partial, wearer.xOld - magnet.xOld, wearer.getX() - magnet.getX()),
                    Mth.lerp(partial, wearer.yOld - magnet.yOld, wearer.getY() - magnet.getY()) + wearer.getEyeHeight() + 0.66,
                    Mth.lerp(partial, wearer.zOld - magnet.zOld, wearer.getZ() - magnet.getZ())).subtract(forward.scale(0.5));
            pose.pushPose();
            pose.translate(pivot.x, pivot.y, pivot.z);
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            boom.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CRANE)), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            drawCable(pose.last(), buffers.getBuffer(RenderType.lines()), pivot.add(forward.scale(BOOM_LENGTH)), new Vec3(0, 0.38, 0));
        }

        @Override
        public ResourceLocation getTextureLocation(final OBMagnet magnet) {
            return MAGNET;
        }

        private static void drawCable(final PoseStack.Pose pose, final VertexConsumer vertices, final Vec3 start, final Vec3 end) {
            final Vec3 normal = end.subtract(start).normalize();
            final int segments = Math.max(1, Mth.ceil(start.distanceTo(end) * 8));
            for (int segment = 0; segment < segments; segment++) {
                final int tone = segment % 2 == 0 ? 255 : 35;
                cableVertex(pose, vertices, start.lerp(end, segment / (double) segments), normal, tone);
                cableVertex(pose, vertices, start.lerp(end, (segment + 1) / (double) segments), normal, tone);
            }
        }

        private static void cableVertex(final PoseStack.Pose pose, final VertexConsumer vertices, final Vec3 point, final Vec3 normal, final int tone) {
            vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z).color(tone, tone, 0, 255)
                    .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
        }
    }

    private static final class CargoRenderer extends EntityRenderer<OBMountedBlock> {

        private CargoRenderer(final EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(final OBMountedBlock cargo, final float yaw, final float partial, final PoseStack pose,
                final MultiBufferSource buffers, final int light) {
            pose.pushPose();
            pose.translate(-0.5, 0, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(cargo.carriedState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(final OBMountedBlock cargo) {
            return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        }
    }
}
