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
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBSleepingBagRendering {

    private static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.id("sleeping_bag"), "bag");

    private OBSleepingBagRendering() {}

    @SubscribeEvent
    public static void registerLayer(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, () -> {
            final var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
            final var bag = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).mirror()
                    .addBox(-9, 0, -3, 18, 26, 7), PartPose.ZERO);
            bag.addOrReplaceChild("pillow", CubeListBuilder.create().texOffs(50, 0).mirror().addBox(-8, -9, 0, 8, 18, 1),
                    PartPose.offsetAndRotation(0, 0, 3, 0, 0, (float) (Math.PI / 2)));
            return LayerDefinition.create(mesh, 128, 64);
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
                        body.render(pose, vertices, light, overlay, red, green, blue, alpha);
                    }
                };
                return model;
            }
        };
    }
}
