package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBTrophyBlock;
import art.arcane.openblocks.block.entity.OBTrophyBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.trophy.OBTrophyType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBTrophyRendering {

    private static final Map<OBTrophyType, Entity> MODELS = new EnumMap<>(OBTrophyType.class);
    private static ClientLevel modelLevel;

    private OBTrophyRendering() {}

    @SubscribeEvent
    public static void registerRenderer(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.TROPHY.get(), context -> OBTrophyRendering::renderPlaced);
    }

    @SubscribeEvent
    public static void registerLevelUnload(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(OBTrophyRendering::onLevelUnload);
    }

    private static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel() != modelLevel) return;
        MODELS.clear();
        modelLevel = null;
    }

    private static void renderPlaced(final OBTrophyBlockEntity trophy, final float partial, final PoseStack pose,
            final MultiBufferSource buffers, final int light, final int overlay) {
        renderMob(trophy.trophy(), -trophy.getBlockState().getValue(OBTrophyBlock.FACING).toYRot(), pose, buffers, light);
    }

    private static void renderMob(final OBTrophyType type, final float yaw, final PoseStack pose,
            final MultiBufferSource buffers, final int light) {
        final var client = Minecraft.getInstance();
        if (client.level != modelLevel) {
            MODELS.clear();
            modelLevel = client.level;
        }
        if (modelLevel == null) return;
        final Entity mob = MODELS.computeIfAbsent(type, OBTrophyRendering::createDisplayMob);
        if (mob == null) return;

        pose.pushPose();
        pose.translate(0.5, 0.2 + type.verticalOffset(), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        final float scale = (float) type.scale();
        pose.scale(scale, scale, scale);
        // Call the vanilla renderer without the entity dispatcher's shadow and hitbox passes.
        client.getEntityRenderDispatcher().getRenderer(mob).render(mob, 0, 0, pose, buffers, light);
        pose.popPose();
    }

    private static Entity createDisplayMob(final OBTrophyType type) {
        final var mob = type.entityType().create(modelLevel);
        if (mob instanceof Slime slime) slime.setSize(1, false);
        if (mob instanceof Bat bat) bat.setResting(true);
        return mob;
    }

    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {

        public ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(final ItemStack stack, final ItemDisplayContext context, final PoseStack pose,
                final MultiBufferSource buffers, final int light, final int overlay) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(OBBlocks.TROPHY.get().defaultBlockState(), pose, buffers, light, overlay);
            renderMob(OBTrophyType.readOrDefault(stack.getTag()), 0, pose, buffers, light);
        }
    }
}
