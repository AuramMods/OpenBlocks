package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBBearTrapBlock;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBFloorDeviceRendering {

    private static final ResourceLocation FAN_FRAME = OpenBlocks.id("block/fan_frame");
    private static final ResourceLocation FAN_BLADES = OpenBlocks.id("block/fan_blades");
    private static final ResourceLocation TRAP_BASE = OpenBlocks.id("block/beartrap");
    private static final ResourceLocation TRAP_WEST_JAW = OpenBlocks.id("block/beartrap_west_jaw");
    private static final ResourceLocation TRAP_EAST_JAW = OpenBlocks.id("block/beartrap_east_jaw");
    private static final double FAN_HUB = 10.75 / 16.0;
    private static final double TRAP_BASE_OFFSET = 7.5 / 16.0;

    private OBFloorDeviceRendering() {}

    @SubscribeEvent
    public static void registerModels(final ModelEvent.RegisterAdditional event) {
        event.register(FAN_FRAME);
        event.register(FAN_BLADES);
        event.register(TRAP_BASE);
        event.register(TRAP_WEST_JAW);
        event.register(TRAP_EAST_JAW);
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.FAN.get(), context -> (fan, partial, pose, buffers, light, overlay) ->
                renderFan(pose, buffers, fan.getBlockState(), fan.yaw(), fan.bladeRotation(partial), light, overlay));
        event.registerBlockEntityRenderer(OBBlockEntities.BEARTRAP.get(), context -> (trap, partial, pose, buffers, light, overlay) ->
                renderTrap(pose, buffers, trap.getBlockState(), trap.closure(partial), light, overlay));
    }

    private static void renderFan(final PoseStack pose, final MultiBufferSource buffers, final BlockState state,
            final float yaw, final float spin, final int light, final int overlay) {
        pose.pushPose();
        rotateAroundCenter(pose, -yaw);
        draw(pose, buffers, state, FAN_FRAME, light, overlay);
        pose.translate(0.5, FAN_HUB, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        pose.translate(-0.5, -0.5, -0.5);
        draw(pose, buffers, state, FAN_BLADES, light, overlay);
        pose.popPose();
    }

    private static void renderTrap(final PoseStack pose, final MultiBufferSource buffers, final BlockState state,
            final float closure, final int light, final int overlay) {
        pose.pushPose();
        if (state.getValue(OBBearTrapBlock.AXIS) == Direction.Axis.X) rotateAroundCenter(pose, 90);
        pose.translate(0, -TRAP_BASE_OFFSET, 0);
        draw(pose, buffers, state, TRAP_BASE, light, overlay);
        renderJaw(pose, buffers, state, TRAP_WEST_JAW, -closure * 75, light, overlay);
        renderJaw(pose, buffers, state, TRAP_EAST_JAW, closure * 75, light, overlay);
        pose.popPose();
    }

    private static void renderJaw(final PoseStack pose, final MultiBufferSource buffers, final BlockState state,
            final ResourceLocation model, final float angle, final int light, final int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-0.5, -0.5, -0.5);
        draw(pose, buffers, state, model, light, overlay);
        pose.popPose();
    }

    private static void rotateAroundCenter(final PoseStack pose, final float yaw) {
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5, 0, -0.5);
    }

    private static void draw(final PoseStack pose, final MultiBufferSource buffers, final BlockState state,
            final ResourceLocation model, final int light, final int overlay) {
        final var client = Minecraft.getInstance();
        client.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), state,
                client.getModelManager().getModel(model), 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {

        public ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(final ItemStack stack, final ItemDisplayContext context, final PoseStack pose,
                final MultiBufferSource buffers, final int light, final int overlay) {
            if (stack.is(OBItems.FAN.get())) renderFan(pose, buffers, OBBlocks.FAN.get().defaultBlockState(), 0, 0, light, overlay);
            else renderTrap(pose, buffers, OBBlocks.BEARTRAP.get().defaultBlockState(), 0.25F, light, overlay);
        }
    }
}
