package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBSonicGlassesRendering {

    private static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.id("sonic_glasses"), "head");

    private OBSonicGlassesRendering() {}

    @SubscribeEvent
    public static void registerOverlay(final RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("sonic_glasses", OBSonicGlassesOverlay::render);
    }

    @SubscribeEvent
    public static void registerModel(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, () -> {
            final var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
            final var glasses = CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                    .texOffs(0, 16).addBox(-5, -4, -5, 10, 1, 1)
                    .texOffs(0, 22).addBox(-6, -5, -1, 2, 3, 3).addBox(4, -5, -1, 2, 3, 3)
                    .texOffs(0, 28).addBox(-3, -5, -5, 2, 3, 1).addBox(1, -5, -5, 2, 3, 1)
                    .texOffs(0, 18).addBox(-5, -4, -4, 1, 1, 3).addBox(4, -4, -4, 1, 1, 3)
                    .texOffs(10, 18).addBox(-5, -10, 0, 1, 5, 1).addBox(4, -10, 0, 1, 5, 1)
                    .texOffs(32, 0).addBox(-6, -11, -3, 3, 3, 3).addBox(3, -11, -3, 3, 3, 3)
                    .texOffs(32, 6).addBox(-7, -12, -7, 5, 5, 4).addBox(2, -12, -7, 5, 5, 4)
                    .texOffs(32, 15).addBox(-8, -13, -9, 7, 7, 2).addBox(1, -13, -9, 7, 7, 2);
            mesh.getRoot().addOrReplaceChild("head", glasses, PartPose.ZERO);
            return LayerDefinition.create(mesh, 64, 32);
        });
    }

    public static IClientItemExtensions extension() {
        return new IClientItemExtensions() {
            private HumanoidModel<LivingEntity> model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(final LivingEntity wearer, final ItemStack stack,
                    final EquipmentSlot slot, final HumanoidModel<?> original) {
                if (model == null) model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(LAYER)) {
                    @Override
                    public void renderToBuffer(final PoseStack pose, final VertexConsumer vertices, final int light, final int overlay,
                            final float red, final float green, final float blue, final float alpha) {
                        pose.pushPose();
                        pose.scale(1.2F, 1.2F, 1.2F);
                        head.render(pose, vertices, light, overlay, red, green, blue, alpha);
                        pose.popPose();
                    }
                };
                return model;
            }
        };
    }
}
