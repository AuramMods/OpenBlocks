package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBVillageHighlighterBlock;
import art.arcane.openblocks.block.entity.OBVillageHighlighterBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBVillageHighlighterRendering {

    private OBVillageHighlighterRendering() {}

    @SubscribeEvent
    public static void register(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.VILLAGE_HIGHLIGHTER.get(), context -> new Renderer());
    }

    private static final class Renderer implements BlockEntityRenderer<OBVillageHighlighterBlockEntity> {

        @Override public boolean shouldRenderOffScreen(final OBVillageHighlighterBlockEntity highlighter) { return true; }
        @Override public int getViewDistance() { return 256; }

        @Override
        public void render(final OBVillageHighlighterBlockEntity highlighter, final float partial, final PoseStack poses,
                final MultiBufferSource buffers, final int light, final int overlay) {
            if (!highlighter.getBlockState().getValue(OBVillageHighlighterBlock.POWERED)) return;
            final BlockPos origin = highlighter.getBlockPos();
            final var survey = highlighter.survey();
            if (!survey.boundary().isEmpty()) {
                final VertexConsumer dots = buffers.getBuffer(RenderType.debugQuads());
                final Matrix4f matrix = poses.last().pose();
                for (final var boundary : survey.boundary()) {
                    final var section = boundary.section();
                    final int x = section.minBlockX() - origin.getX();
                    final int y = section.minBlockY() - origin.getY();
                    final int z = section.minBlockZ() - origin.getZ();
                    for (final Direction side : Direction.values()) {
                        if (boundary.exposed(side)) drawFace(dots, matrix, x, y, z, side);
                    }
                }
            }

            if (!survey.golemOrigins().isEmpty()) {
                final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
                for (final BlockPos villager : survey.golemOrigins()) {
                    final int x = villager.getX() - origin.getX();
                    final int y = villager.getY() - origin.getY();
                    final int z = villager.getZ() - origin.getZ();
                    // SpawnUtil searches block positions within ±8 horizontally and ±6 vertically.
                    LevelRenderer.renderLineBox(poses, lines, x - 8, y - 6, z - 8, x + 9, y + 7, z + 9, 1, 0.75F, 0.25F, 1);
                }
            }
        }

        private static void drawFace(final VertexConsumer vertices, final Matrix4f matrix, final int x, final int y, final int z,
                final Direction side) {
            final Direction.Axis axis = side.getAxis();
            final float plane = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 : 0;
            for (int u = 2; u < 16; u += 4) {
                for (int v = 2; v < 16; v += 4) {
                    final float r = 0.18F;
                    dotCorner(vertices, matrix, x, y, z, axis, plane, u - r, v - r);
                    dotCorner(vertices, matrix, x, y, z, axis, plane, u + r, v - r);
                    dotCorner(vertices, matrix, x, y, z, axis, plane, u + r, v + r);
                    dotCorner(vertices, matrix, x, y, z, axis, plane, u - r, v + r);
                }
            }
        }

        private static void dotCorner(final VertexConsumer vertices, final Matrix4f matrix, final int x, final int y, final int z,
                final Direction.Axis axis, final float plane, final float u, final float v) {
            switch (axis) {
                case X -> dotVertex(vertices, matrix, x + plane, y + u, z + v);
                case Y -> dotVertex(vertices, matrix, x + u, y + plane, z + v);
                case Z -> dotVertex(vertices, matrix, x + u, y + v, z + plane);
            }
        }

        private static void dotVertex(final VertexConsumer vertices, final Matrix4f matrix, final float x, final float y, final float z) {
            vertices.vertex(matrix, x, y, z).color(0.25F, 0.95F, 1, 1).endVertex();
        }
    }
}
