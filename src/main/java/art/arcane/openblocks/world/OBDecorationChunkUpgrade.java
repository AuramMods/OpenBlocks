package art.arcane.openblocks.world;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBDecorationChunkUpgrade {

    private OBDecorationChunkUpgrade() {}

    @SubscribeEvent
    public static void restoreMissingBlockEntities(final ChunkEvent.Load event) {
        if (event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) return;

        // Earlier 1.20.1 releases saved these blocks without their renderer block entities.
        // Stay inside the loading chunk so this repair can run before chunk packets are sent.
        final int minX = chunk.getPos().getMinBlockX();
        final int minZ = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < chunk.getSectionsCount(); i++) {
            final var section = chunk.getSection(i);
            if (!section.maybeHas(OBDecorationChunkUpgrade::needsRenderer)) continue;

            final int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (final BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, minX + 15, minY + 15, minZ + 15)) {
                final BlockState state = section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
                if (!needsRenderer(state) || chunk.getBlockEntity(pos) != null) continue;

                final BlockPos restoredPos = pos.immutable();
                chunk.getBlockEntity(restoredPos, LevelChunk.EntityCreationType.IMMEDIATE);
                chunk.setUnsaved(true);
                if (state.is(OBBlocks.SKY.get())) {
                    chunk.getBlockTicks().schedule(new ScheduledTick<>(state.getBlock(), restoredPos,
                            level.getGameTime() + 1, level.nextSubTickCount()));
                }
            }
        }
    }

    private static boolean needsRenderer(final BlockState state) {
        return state.is(OBBlocks.FLAG.get()) || state.is(OBBlocks.SKY.get()) || state.is(OBBlocks.IMAGINARY.get());
    }
}
