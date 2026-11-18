package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBSprinklerBlock;
import art.arcane.openblocks.block.entity.OBSprinklerBlockEntity;
import art.arcane.openblocks.registry.OBBlockEntities;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBSprinklerRendering {

    private static final ResourceLocation NOZZLE = OpenBlocks.id("block/sprinkler_moving");
    private static final ResourceLocation WATER = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final int[] SPRAY_INTERVAL = {1, 5, 20};
    private static final int[] OUTLETS = {6, 5, 3};

    private OBSprinklerRendering() { }

    @SubscribeEvent public static void registerModels(final ModelEvent.RegisterAdditional event) { event.register(NOZZLE); }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OBBlockEntities.SPRINKLER.get(), context -> (sprinkler, partial, pose, buffers, light, overlay) -> {
            final var client = Minecraft.getInstance();
            final var state = sprinkler.getBlockState();
            final var level = sprinkler.getLevel();
            if (level == null) return;
            pose.pushPose();
            pose.translate(0.5, 3.0 / 16.0, 0.5);
            if (state.getValue(OBSprinklerBlock.AXIS) == Direction.Axis.X) pose.mulPose(Axis.YP.rotationDegrees(90));
            if (sprinkler.isActive()) pose.mulPose(Axis.ZP.rotationDegrees(OBSprinklerBlockEntity.sprayAngle(level, partial)));
            pose.translate(-0.5, -0.5, -0.5);
            final var nozzle = client.getModelManager().getModel(NOZZLE);
            client.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), state,
                    nozzle, 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
            pose.popPose();
        });
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final OBSprinklerBlockEntity sprinkler) {
        if (!sprinkler.isActive() || !(level instanceof ClientLevel clientLevel)) return;
        final var client = Minecraft.getInstance();
        final int detail = client.options.particles().get().getId();
        if (level.getGameTime() % SPRAY_INTERVAL[detail] != 0
                || client.gameRenderer.getMainCamera().getPosition().distanceToSqr(Vec3.atCenterOf(pos)) > 32 * 32) return;
        final int outlets = OUTLETS[detail];
        final boolean alongX = state.getValue(OBSprinklerBlock.AXIS) == Direction.Axis.X;
        final double velocity = Math.sin(Math.toRadians(OBSprinklerBlockEntity.sprayAngle(level, 0))) * 0.5;
        final var sprite = client.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WATER);
        for (int index = 0; index < outlets; index++) {
            final double offset = 0.6 * (index / (double) (outlets - 1) - 0.5);
            final double scatter = Math.sin(Math.toRadians(25) * (level.random.nextDouble() - 0.5));
            final double x = pos.getX() + 0.5 + (alongX ? offset : 0);
            final double z = pos.getZ() + 0.5 + (alongX ? 0 : offset);
            client.particleEngine.add(new WaterSpray(clientLevel, sprite, x, pos.getY() + 0.2, z,
                    alongX ? scatter : velocity, alongX ? -velocity : scatter));
        }
    }

    private static final class WaterSpray extends TextureSheetParticle {
        private WaterSpray(final ClientLevel level, final TextureAtlasSprite sprite, final double x, final double y, final double z,
                final double vx, final double vz) {
            super(level, x, y, z);
            setSprite(sprite);
            setSize(0.2F, 0.2F);
            quadSize = 0.03F;
            gravity = 0.7F;
            lifetime = 50;
            xd = vx;
            yd = 0.35;
            zd = vz;
        }

        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.TERRAIN_SHEET; }
    }
}
