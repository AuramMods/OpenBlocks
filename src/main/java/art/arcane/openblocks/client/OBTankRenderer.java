package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.item.OBTankItem;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBTankRenderer implements BlockEntityRenderer<OBTankBlockEntity> {

    private static final float INSET = 0.001F;
    private static final Direction[] DIRECTIONS = Direction.values();

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.TANK.get(), context -> new OBTankRenderer());
    }

    @Override
    public void render(final OBTankBlockEntity tank, final float partialTick, final PoseStack pose,
            final MultiBufferSource buffers, final int light, final int overlay) {
        renderContents(tank.contents(), tank, partialTick, pose, buffers, light, overlay);
    }

    private static void renderContents(final FluidStack contents, @Nullable final OBTankBlockEntity tank,
            final float partialTick, final PoseStack pose, final MultiBufferSource buffers, final int light, final int overlay) {
        if (contents.isEmpty()) return;
        final var extensions = IClientFluidTypeExtensions.of(contents.getFluid());
        final var texture = extensions.getStillTexture(contents);
        if (texture == null) return;
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        final int tint = tank != null && tank.getLevel() != null && contents.getFluid().is(FluidTags.WATER)
                ? extensions.getTintColor(contents.getFluid().defaultFluidState(), tank.getLevel(), tank.getBlockPos())
                : extensions.getTintColor(contents);
        final float own = height(contents);
        final float[] adjacent = new float[DIRECTIONS.length];
        if (tank != null) {
            for (final Direction direction : DIRECTIONS) {
                final var neighbor = tank.neighbor(direction);
                adjacent[direction.ordinal()] = neighborHeight(neighbor, contents);
            }
        }

        final boolean bottomConnected = adjacent[Direction.DOWN.ordinal()] >= 1.0F;
        final boolean topConnected = own >= 1.0F && adjacent[Direction.UP.ordinal()] > 0.0F;
        final float[] surface = new float[4];
        for (int i = 0; i < surface.length; i++) {
            final Direction x = (i & 1) == 0 ? Direction.WEST : Direction.EAST;
            final Direction z = (i & 2) == 0 ? Direction.NORTH : Direction.SOUTH;
            surface[i] = cornerHeight(tank, contents, own, adjacent, x, z, topConnected, partialTick);
        }
        final VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
        final PoseStack.Pose transform = pose.last();
        for (final Direction direction : DIRECTIONS) {
            if (direction == Direction.DOWN && bottomConnected || direction == Direction.UP && topConnected
                    || direction.getAxis().isHorizontal() && adjacent[direction.ordinal()] > 0.0F) continue;
            final FaceInfo face = FaceInfo.fromFacing(direction);
            for (int i = 0; i < 4; i++) {
                final FaceInfo.VertexInfo vertex = face.getVertexInfo(i);
                final Direction xSide = DIRECTIONS[vertex.xFace];
                final Direction zSide = DIRECTIONS[vertex.zFace];
                final float x = edgePosition(xSide, adjacent[xSide.ordinal()]);
                final float z = edgePosition(zSide, adjacent[zSide.ordinal()]);
                final float y = vertex.yFace == FaceInfo.Constants.MAX_Y
                        ? surface[corner(xSide, zSide)] : (bottomConnected ? 0.0F : INSET);
                final float u = direction.getAxis() == Direction.Axis.X ? z : x;
                final float v = direction.getAxis().isVertical() ? z : 1.0F - y;
                vertices.vertex(transform.pose(), x, y, z)
                        .color(tint >> 16 & 255, tint >> 8 & 255, tint & 255, tint >>> 24)
                        .uv(sprite.getU(u * 16.0F), sprite.getV(v * 16.0F)).overlayCoords(overlay).uv2(light)
                        .normal(transform.normal(), direction.getStepX(), direction.getStepY(), direction.getStepZ()).endVertex();
            }
        }
    }

    private static float cornerHeight(@Nullable final OBTankBlockEntity tank, final FluidStack fluid, final float own,
            final float[] adjacent, final Direction xSide, final Direction zSide, final boolean topConnected, final float partialTick) {
        if (topConnected) return 1.0F;
        final float xHeight = adjacent[xSide.ordinal()];
        final float zHeight = adjacent[zSide.ordinal()];
        float sum = own + xHeight + zHeight;
        int samples = 1 + (xHeight > 0.0F ? 1 : 0) + (zHeight > 0.0F ? 1 : 0);
        if (tank != null && (xHeight > 0.0F || zHeight > 0.0F)) {
            final var side = tank.neighbor(xHeight > 0.0F ? xSide : zSide);
            final float diagonal = side == null ? 0.0F : neighborHeight(side.neighbor(xHeight > 0.0F ? zSide : xSide), fluid);
            if (diagonal > 0.0F) {
                sum += diagonal;
                samples++;
            }
        }
        float surface = sum / samples;
        if (tank != null && tank.getLevel() != null && surface < 1.0F) {
            final var pos = tank.getBlockPos();
            final double time = tank.getLevel().getGameTime() + (double) partialTick;
            final double phase = 0.5D * (pos.getX() + pos.getY() + pos.getZ() + (xSide.getStepX() + zSide.getStepZ()) * 0.5D);
            if (surface >= 0.95F) surface = 1.0F;
            surface = Math.max(0.05F, surface) + 0.025F * (float) Math.sin(0.1D * time + phase);
        }
        return Mth.clamp(surface, INSET * 2.0F, 1.0F - INSET);
    }

    private static int corner(final Direction x, final Direction z) {
        return (x == Direction.EAST ? 1 : 0) | (z == Direction.SOUTH ? 2 : 0);
    }

    private static float edgePosition(final Direction direction, final float adjacent) {
        final float inset = adjacent > 0.0F ? 0.0F : INSET;
        return direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F - inset : inset;
    }

    private static float height(final FluidStack fluid) {
        return Math.min(1.0F, (float) fluid.getAmount() / OBTankBlockEntity.capacity());
    }

    private static float neighborHeight(@Nullable final OBTankBlockEntity neighbor, final FluidStack fluid) {
        if (neighbor == null) return 0.0F;
        final FluidStack contents = neighbor.contents();
        return contents.isFluidEqual(fluid) ? height(contents) : 0.0F;
    }

    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {

        public ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(final ItemStack stack, final ItemDisplayContext context, final PoseStack pose,
                final MultiBufferSource buffers, final int light, final int overlay) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(OBBlocks.TANK.get().defaultBlockState(), pose,
                    buffers, light, overlay);
            renderContents(OBTankItem.contents(stack), null, 0.0F, pose, buffers, light, overlay);
        }
    }
}
