package art.arcane.openblocks.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

public record OBVillageSurvey(List<Boundary> boundary, List<BlockPos> golemOrigins, boolean vacantBed) {

    public static final OBVillageSurvey EMPTY = new OBVillageSurvey(List.of(), List.of(), false);
    private static final int SECTION_RADIUS = 4;

    public static OBVillageSurvey measure(final ServerLevel level, final BlockPos pos, final boolean showOverlay) {
        final SectionPos origin = SectionPos.of(pos);
        if (!level.isVillage(origin)) return EMPTY;

        final Set<SectionPos> visited = new HashSet<>();
        final Set<SectionPos> settlement = new HashSet<>();
        final var pending = new ArrayDeque<SectionPos>();
        final var boundary = new ArrayList<Boundary>();
        pending.add(origin);
        visited.add(origin);
        while (!pending.isEmpty()) {
            final SectionPos section = pending.remove();
            settlement.add(section);
            int exposed = 0;
            for (final Direction side : Direction.values()) {
                final SectionPos next = SectionPos.of(section.x() + side.getStepX(), section.y() + side.getStepY(), section.z() + side.getStepZ());
                if (level.isOutsideBuildHeight(next.minBlockY()) || !level.isVillage(next)) {
                    exposed |= 1 << side.ordinal();
                } else if (Math.abs(next.x() - origin.x()) <= SECTION_RADIUS && Math.abs(next.y() - origin.y()) <= SECTION_RADIUS
                        && Math.abs(next.z() - origin.z()) <= SECTION_RADIUS && visited.add(next)) {
                    pending.add(next);
                }
            }
            if (showOverlay && exposed != 0) boundary.add(new Boundary(section, exposed));
        }

        // POI queries read the village records without loading terrain or reserving tickets.
        final var manager = level.getPoiManager();
        final boolean vacantBed = settlement.stream().map(SectionPos::chunk).distinct()
                .filter(chunk -> level.hasChunk(chunk.x, chunk.z))
                .flatMap(chunk -> manager.getInChunk(type -> type.is(PoiTypes.HOME), chunk, PoiManager.Occupancy.HAS_SPACE))
                .anyMatch(bed -> settlement.contains(SectionPos.of(bed.getPos())));
        if (!showOverlay) return new OBVillageSurvey(List.of(), List.of(), vacantBed);

        final int radius = SECTION_RADIUS * SectionPos.SECTION_SIZE;
        final var search = new AABB(origin.minBlockX() - radius, origin.minBlockY() - radius, origin.minBlockZ() - radius,
                origin.maxBlockX() + radius + 1, origin.maxBlockY() + radius + 1, origin.maxBlockZ() + radius + 1);
        final var villagers = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.isAlive() && !villager.isBaby() && settlement.contains(SectionPos.of(villager.blockPosition())));
        final var golemOrigins = villagers.stream().map(Villager::blockPosition).distinct().sorted().toList();
        return new OBVillageSurvey(List.copyOf(boundary), golemOrigins, vacantBed);
    }

    public record Boundary(SectionPos section, int faces) {
        public boolean exposed(final Direction side) { return (faces & (1 << side.ordinal())) != 0; }
    }
}
