package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBCanvasBlock;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public final class OBCanvasBlockEntity extends BlockEntity {

    public static final int UNPAINTED = -1;
    public static final ModelProperty<Surface> SURFACE = new ModelProperty<>();

    private BlockState original = Blocks.AIR.defaultBlockState();
    private final int[] paint = {UNPAINTED, UNPAINTED, UNPAINTED, UNPAINTED, UNPAINTED, UNPAINTED};
    private ModelData modelData = ModelData.EMPTY;

    public record Surface(BlockState original, List<Integer> colors) {
        public int color(final Direction face) { return colors.get(face.get3DDataValue()); }
    }

    public OBCanvasBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public BlockState original() {
        return original.hasProperty(BlockStateProperties.WATERLOGGED)
                ? original.setValue(BlockStateProperties.WATERLOGGED, getBlockState().getValue(BlockStateProperties.WATERLOGGED))
                : original;
    }

    public int color(final Direction face) {
        return paint[face.get3DDataValue()];
    }

    public void remember(final BlockState state) {
        original = state;
        change();
        if (level != null) level.getLightEngine().checkBlock(worldPosition);
    }

    public boolean paint(final int rgb, final Direction... sides) {
        boolean changed = false;
        for (final Direction face : sides) {
            final int index = face.get3DDataValue();
            changed |= paint[index] != rgb;
            paint[index] = rgb;
        }
        if (changed) change();
        return changed;
    }

    public boolean rinse(final Direction... sides) {
        if (level == null) return false;
        final int[] remaining = paint.clone();
        for (final Direction face : sides) remaining[face.get3DDataValue()] = UNPAINTED;
        if (Arrays.equals(paint, remaining)) return false;
        final BlockState restored = original();
        if (!restored.isAir() && Arrays.stream(remaining).allMatch(rgb -> rgb == UNPAINTED)) {
            // Forge snapshots the live block entity before removing it.
            return level.setBlockAndUpdate(worldPosition, restored);
        }
        System.arraycopy(remaining, 0, paint, 0, paint.length);
        change();
        return true;
    }

    private void change() {
        if (level == null) return;
        if (level.captureBlockSnapshots && level.capturedBlockSnapshots.stream().anyMatch(snapshot -> snapshot.getPos().equals(worldPosition))) {
            level.blockEntityChanged(worldPosition);
        } else {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Original", Tag.TAG_COMPOUND)) {
            final BlockState saved = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Original"));
            original = saved.getBlock() instanceof OBCanvasBlock || saved.hasBlockEntity() ? Blocks.AIR.defaultBlockState() : saved;
        } else {
            original = Blocks.AIR.defaultBlockState();
        }
        final int[] savedPaint = tag.getIntArray("Paint");
        Arrays.fill(paint, UNPAINTED);
        for (int i = 0; i < Math.min(paint.length, savedPaint.length); i++) {
            paint[i] = savedPaint[i] == UNPAINTED ? UNPAINTED : savedPaint[i] & 0xFFFFFF;
        }
        if (level != null && level.isClientSide()) refreshModel();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) refreshModel();
    }

    private void refreshModel() {
        modelData = ModelData.builder().with(SURFACE, new Surface(original(), Arrays.stream(paint).boxed().toList())).build();
        requestModelDataUpdate();
    }

    @Override public ModelData getModelData() { return modelData; }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        if (!original.isAir()) tag.put("Original", NbtUtils.writeBlockState(original()));
        tag.putIntArray("Paint", paint);
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void onDataPacket(final Connection connection, final ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() == null) return;
        handleUpdateTag(packet.getTag());
        if (level != null) {
            level.getLightEngine().checkBlock(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
}
