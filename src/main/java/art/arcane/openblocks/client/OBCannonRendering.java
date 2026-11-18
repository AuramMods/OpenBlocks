package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBCannonBlockEntity;
import art.arcane.openblocks.entity.OBItemProjectile;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBCannonRendering {

    private static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.id("cannon"), "main");
    private static final ResourceLocation TEXTURE = OpenBlocks.id("textures/models/cannon.png");
    private static final float[] RECOIL = {0, 0.5F, 1, 0.9F, 0.8F, 0.7F, 0.6F, 0.5F, 0.4F, 0.3F, 0.2F, 0.1F, 0};

    private OBCannonRendering() {}

    @SubscribeEvent
    public static void registerLayer(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, OBCannonRendering::layer);
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OBEntities.ITEM_PROJECTILE.get(), ItemEntityRenderer::new);
        event.registerBlockEntityRenderer(OBBlockEntities.CANNON.get(), context -> {
            final var model = new CannonModel(context.bakeLayer(LAYER));
            return (cannon, partial, pose, buffers, light, overlay) -> {
                final int tick = cannon.recoilTicks();
                final float recoil = Mth.lerp(partial, RECOIL[tick], RECOIL[Math.min(tick + 1, RECOIL.length - 1)]);
                model.render(cannon.yaw(partial), cannon.pitch(partial), recoil, pose, buffers, light, overlay);
                final var player = Minecraft.getInstance().player;
                if (player != null && player.getMainHandItem().is(OBItems.POINTER.get())) renderTrajectory(cannon, pose, buffers);
            };
        });
    }

    private static LayerDefinition layer() {
        final var mesh = new MeshDefinition();
        final var root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -5, -3, 6, 6, 6), PartPose.ZERO);
        root.addOrReplaceChild("barrel", CubeListBuilder.create().texOffs(34, 0).addBox(-2, -4, 2, 4, 4, 6), PartPose.ZERO);
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(14, 19).addBox(-6, 0, -6, 12, 1, 12), PartPose.offset(0, 15, 0));
        root.addOrReplaceChild("wheel", CubeListBuilder.create().texOffs(0, 20).addBox(3, -3, -3, 1, 6, 6), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void renderTrajectory(final OBCannonBlockEntity cannon, final PoseStack pose, final MultiBufferSource buffers) {
        Vec3 velocity = cannon.motion();
        Vec3 start = cannon.origin().subtract(Vec3.atLowerCornerOf(cannon.getBlockPos()));
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        final PoseStack.Pose matrices = pose.last();
        for (int tick = 0; tick < 200; tick++) {
            velocity = velocity.add(0, -OBItemProjectile.GRAVITY, 0);
            final Vec3 end = start.add(velocity);
            final Vec3 direction = velocity.normalize();
            lineVertex(lines, matrices, start, direction);
            lineVertex(lines, matrices, end, direction);
            start = end;
        }
    }

    private static void lineVertex(final VertexConsumer buffer, final PoseStack.Pose pose, final Vec3 point, final Vec3 direction) {
        buffer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z).color(0, 0, 0, 255)
                .normal(pose.normal(), (float) direction.x, (float) direction.y, (float) direction.z).endVertex();
    }

    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        private final CannonModel model;

        public ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
            model = new CannonModel(Minecraft.getInstance().getEntityModels().bakeLayer(LAYER));
        }

        @Override
        public void renderByItem(final ItemStack stack, final ItemDisplayContext context, final PoseStack pose,
                final MultiBufferSource buffers, final int light, final int overlay) {
            model.render(0, 45, 0, pose, buffers, light, overlay);
        }
    }

    private static final class CannonModel {
        private final ModelPart body;
        private final ModelPart barrel;
        private final ModelPart base;
        private final ModelPart wheel;

        private CannonModel(final ModelPart root) {
            body = root.getChild("body");
            barrel = root.getChild("barrel");
            base = root.getChild("base");
            wheel = root.getChild("wheel");
        }

        private void render(final double yaw, final double pitch, final float recoil, final PoseStack pose,
                final MultiBufferSource buffers, final int light, final int overlay) {
            pose.pushPose();
            pose.translate(0.5, 1.0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees((float) (180 - yaw)));
            pose.mulPose(Axis.XP.rotationDegrees(180));

            final float tilt = (float) Math.toRadians(pitch);
            final float slide = 4 * recoil;
            body.setPos(0, 11, 3 - slide);
            barrel.setPos(0, 11 + 3 * recoil * Mth.sin(tilt), 3 - 3 * recoil * Mth.cos(tilt) - slide);
            body.xRot = barrel.xRot = tilt + 0.5F * recoil;
            final var vertices = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
            body.render(pose, vertices, light, overlay);
            barrel.render(pose, vertices, light, overlay);
            base.render(pose, vertices, light, overlay);

            for (int side = 1; side >= -1; side -= 2) {
                wheel.zRot = side == 1 ? 0 : Mth.PI;
                for (int spoke = 0; spoke < 3; spoke++) {
                    wheel.setPos(spoke * -0.01F, 11, -slide);
                    wheel.xRot = side * recoil + spoke * Mth.PI / 6;
                    wheel.render(pose, vertices, light, overlay);
                }
            }
            pose.popPose();
        }
    }
}
