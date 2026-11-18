package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBLuggage;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBLuggageRendering {

    private static final ModelLayerLocation LAYER = new ModelLayerLocation(OpenBlocks.id("luggage"), "main");
    private static final ResourceLocation NORMAL = OpenBlocks.id("textures/model/luggage.png");
    private static final ResourceLocation CHARGED = OpenBlocks.id("textures/model/luggage_special.png");
    private static final ResourceLocation ENERGY = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");

    private OBLuggageRendering() {}

    @SubscribeEvent
    public static void registerLayer(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, OBLuggageRendering::layer);
    }

    @SubscribeEvent
    public static void registerRenderer(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OBEntities.LUGGAGE.get(), LuggageRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(OBMenuTypes.LUGGAGE.get(), ContainerScreen::new);
            ItemProperties.register(OBItems.LUGGAGE.get(), OpenBlocks.id("charged"),
                    (stack, level, holder, seed) -> OBLuggage.isChargedItem(stack) ? 1 : 0);
        });
    }

    private static LayerDefinition layer() {
        final var mesh = new MeshDefinition();
        final var root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8, 0, -4, 16, 7, 8),
                PartPose.offset(0, 13, 0));
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 23).addBox(-8, -2, -8, 16, 2, 8),
                PartPose.offset(0, 13, 4));
        for (int x = -3; x <= 3; x++) {
            for (int z = -1; z <= 1; z++) {
                root.addOrReplaceChild(legName(x, z), CubeListBuilder.create().texOffs(0, 41).addBox(-0.5F, 0, -0.5F, 1, 4, 1),
                        PartPose.offset(x * 2, 20, z * 2));
            }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static String legName(final int x, final int z) {
        return "leg_" + x + "_" + z;
    }

    private static final class LuggageModel extends HierarchicalModel<OBLuggage> {

        private final ModelPart root;
        private final ModelPart lid;
        private final ModelPart[][] legs = new ModelPart[7][3];

        private LuggageModel(final ModelPart root) {
            this.root = root;
            this.lid = root.getChild("lid");
            for (int x = -3; x <= 3; x++) {
                for (int z = -1; z <= 1; z++) legs[x + 3][z + 1] = root.getChild(legName(x, z));
            }
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(final OBLuggage luggage, final float walk, final float amplitude, final float age,
                final float headYaw, final float headPitch) {
            lid.xRot = Math.min(0, Mth.cos(walk * 0.6662F) * 1.4F * amplitude);
            for (int x = -3; x <= 3; x++) {
                for (int z = -1; z <= 1; z++) legs[x + 3][z + 1].xRot = Mth.cos(walk + x * z * 0.6662F) * 1.4F * amplitude;
            }
        }
    }

    private static final class LuggageRenderer extends MobRenderer<OBLuggage, LuggageModel> {

        private LuggageRenderer(final EntityRendererProvider.Context context) {
            super(context, new LuggageModel(context.bakeLayer(LAYER)), 0.5F);
            addLayer(new EnergySwirlLayer<>(this) {
                @Override protected float xOffset(final float age) { return age * 0.01F; }
                @Override protected ResourceLocation getTextureLocation() { return ENERGY; }
                @Override protected LuggageModel model() { return getParentModel(); }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(final OBLuggage luggage) {
            return luggage.isPowered() ? CHARGED : NORMAL;
        }
    }
}
