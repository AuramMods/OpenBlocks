package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBSkyBlock;
import art.arcane.openblocks.block.entity.OBSkyBlockEntity;
import art.arcane.openblocks.config.OBClientConfig;
import art.arcane.openblocks.registry.OBBlockEntities;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL30;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT)
public final class OBSkyRendering {

    private static final Direction[] FACES = Direction.values();
    private static final BufferBuilder FACES_BUFFER = new BufferBuilder(4096);
    private static ShaderInstance shader;
    private static RenderTarget capturedSky;
    private static boolean requested;
    private static boolean captured;

    private OBSkyRendering() {}

    @SubscribeEvent
    public static void renderLevelStage(final RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            // The previous frame's visible blocks request a copy before any terrain is drawn.
            if (OBClientConfig.RENDER_SKY_BLOCKS.get() && requested && shader != null) {
                captureSky(Minecraft.getInstance().getMainRenderTarget());
                captured = true;
            } else {
                captured = false;
            }
            requested = false;
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES && FACES_BUFFER.building()) {
            SkyRenderType.SKY.end(FACES_BUFFER, VertexSorting.DISTANCE_TO_ORIGIN);
        }
    }

    private static void captureSky(final RenderTarget main) {
        if (capturedSky == null) {
            capturedSky = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        } else if (capturedSky.width != main.width || capturedSky.height != main.height) {
            capturedSky.resize(main.width, main.height, Minecraft.ON_OSX);
        }

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, capturedSky.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.viewWidth, main.viewHeight, 0, 0, main.viewWidth, main.viewHeight,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
        main.bindWrite(false);
    }

    @SubscribeEvent
    public static void hideOutline(final RenderHighlightEvent.Block event) {
        final var level = Minecraft.getInstance().level;
        if (level == null) return;
        final var state = level.getBlockState(event.getTarget().getBlockPos());
        if (state.getBlock() instanceof OBSkyBlock && OBSkyBlock.isActive(state)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void releaseSky(final ClientPlayerNetworkEvent.LoggingOut event) {
        requested = false;
        captured = false;
        if (capturedSky != null) {
            capturedSky.destroyBuffers();
            capturedSky = null;
        }
    }

    @Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {

        @SubscribeEvent
        public static void registerShader(final RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), OpenBlocks.id("sky_block"),
                    DefaultVertexFormat.POSITION), instance -> shader = instance);
        }

        @SubscribeEvent
        public static void registerRenderer(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(OBBlockEntities.SKY.get(), context -> new SkyRenderer());
        }
    }

    private static final class SkyRenderer implements BlockEntityRenderer<OBSkyBlockEntity> {

        private final RandomSource fallbackRandom = RandomSource.create();

        @Override
        public void render(final OBSkyBlockEntity sky, final float partial, final PoseStack pose,
                final MultiBufferSource buffers, final int light, final int overlay) {
            final var state = sky.getBlockState();
            final var level = sky.getLevel();
            if (!OBSkyBlock.isActive(state) || level == null) return;

            final BlockPos pos = sky.getBlockPos();
            requested = true;
            if (!captured) {
                final var renderer = Minecraft.getInstance().getBlockRenderer();
                pose.pushPose();
                renderer.getModelRenderer().tesselateBlock(level, renderer.getBlockModel(state), state, pos, pose,
                        buffers.getBuffer(Sheets.solidBlockSheet()), true, fallbackRandom, state.getSeed(pos), overlay,
                        ModelData.EMPTY, RenderType.solid());
                pose.popPose();
                return;
            }

            final var neighbor = new BlockPos.MutableBlockPos();
            for (final Direction face : FACES) {
                if (!Block.shouldRenderFace(state, level, pos, face, neighbor.setWithOffset(pos, face))) continue;
                if (!FACES_BUFFER.building()) FACES_BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                final FaceInfo faceInfo = FaceInfo.fromFacing(face);
                for (int index = 0; index < 4; index++) {
                    final FaceInfo.VertexInfo vertex = faceInfo.getVertexInfo(index);
                    FACES_BUFFER.vertex(pose.last().pose(), vertex.xFace == FaceInfo.Constants.MAX_X ? 1 : 0,
                            vertex.yFace == FaceInfo.Constants.MAX_Y ? 1 : 0,
                            vertex.zFace == FaceInfo.Constants.MAX_Z ? 1 : 0).endVertex();
                }
            }
        }

        @Override public int getViewDistance() { return 256; }
    }

    private abstract static class SkyRenderType extends RenderType {

        private static final RenderType SKY = create("open_blocks_sky", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS,
                4096, false, false, CompositeState.builder().setShaderState(new ShaderStateShard(() -> shader))
                        .setTextureState(new EmptyTextureStateShard(
                                () -> RenderSystem.setShaderTexture(0, capturedSky.getColorTextureId()), () -> {}))
                        .createCompositeState(false));

        private SkyRenderType(final Runnable setup, final Runnable clear) {
            super("open_blocks_sky", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 4096, false, false, setup, clear);
        }
    }
}
