package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBVillageHighlighterBlock;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.world.OBVillageSurvey;
import art.arcane.openblocks.world.OBVillageSurvey.Boundary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OBVillageHighlighterBlockEntity extends BlockEntity {

    private OBVillageSurvey survey = OBVillageSurvey.EMPTY;
    private boolean checkingInput;
    private long nextSurvey;

    public OBVillageHighlighterBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.VILLAGE_HIGHLIGHTER.get(), pos, state);
    }

    public OBVillageSurvey survey() { return survey; }
    public int signal() { return !checkingInput && survey.vacantBed() ? 15 : 0; }

    public void refreshInput(final ServerLevel level) {
        final BlockState state = getBlockState();
        final boolean powered;
        // An adjacent solid block can conduct our own strong power back to the highlighter.
        checkingInput = true;
        try {
            powered = level.hasNeighborSignal(worldPosition);
        } finally {
            checkingInput = false;
        }
        if (state.getValue(OBVillageHighlighterBlock.POWERED) != powered) {
            nextSurvey = 0;
            level.setBlock(worldPosition, state.setValue(OBVillageHighlighterBlock.POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBVillageHighlighterBlockEntity highlighter) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() < highlighter.nextSurvey) return;
        highlighter.refreshInput(server);
        highlighter.nextSurvey = level.getGameTime() + 10;
        final BlockState current = highlighter.getBlockState();
        final OBVillageSurvey previous = highlighter.survey;
        final OBVillageSurvey next = OBVillageSurvey.measure(server, pos, current.getValue(OBVillageHighlighterBlock.POWERED));
        highlighter.survey = next;
        if (next.vacantBed() != previous.vacantBed()) level.updateNeighborsAt(pos, current.getBlock());
        if (!next.equals(previous)) level.sendBlockUpdated(pos, current, current, Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag() {
        final var tag = new CompoundTag();
        final var boundary = survey.boundary();
        final long[] faces = new long[boundary.size() * 2];
        for (int i = 0; i < boundary.size(); i++) {
            faces[2 * i] = boundary.get(i).section().asLong();
            faces[2 * i + 1] = boundary.get(i).faces();
        }
        tag.putLongArray("boundary", faces);
        tag.putLongArray("golem_origins", survey.golemOrigins().stream().mapToLong(BlockPos::asLong).toArray());
        tag.putBoolean("vacant_bed", survey.vacantBed());
        return tag;
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        final long[] faces = tag.getLongArray("boundary");
        final var boundary = new ArrayList<Boundary>(faces.length / 2);
        for (int i = 0; i + 1 < faces.length; i += 2) {
            boundary.add(new Boundary(SectionPos.of(faces[i]), (int) faces[i + 1]));
        }
        final var golemOrigins = new ArrayList<BlockPos>();
        for (final long pos : tag.getLongArray("golem_origins")) golemOrigins.add(BlockPos.of(pos));
        survey = new OBVillageSurvey(List.copyOf(boundary), List.copyOf(golemOrigins), tag.getBoolean("vacant_bed"));
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
