package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBCanvasBlock;
import art.arcane.openblocks.item.OBStencilItem;
import art.arcane.openblocks.item.OBStencilPattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

public final class OBCanvasBlockEntity extends BlockEntity {

    public static final int UNPAINTED = -1;
    public static final ModelProperty<Surface> SURFACE = new ModelProperty<>();
    private static final Face BLANK = new Face(UNPAINTED, List.of(), null);

    private BlockState original = Blocks.AIR.defaultBlockState();
    private final Face[] faces = new Face[Direction.values().length];
    private ModelData modelData = ModelData.EMPTY;
    private boolean restoringOriginal;

    public record Surface(BlockState original, List<Face> faces) {
        public Face face(final Direction side) { return faces.get(side.get3DDataValue()); }
    }

    public record Mask(OBStencilPattern pattern, int rotation) {
        public Mask rotate() { return new Mask(pattern, (rotation + 1) & 3); }
    }

    public record Layer(Mask mask, int color) {}

    public record Face(int background, List<Layer> layers, @Nullable Mask cover) {
        public boolean decorated() { return cover != null || !layers.isEmpty(); }

        private boolean empty() { return background == UNPAINTED && !decorated(); }

        private Face stamp(final int rgb) {
            if (cover == null) return new Face(rgb, List.of(), null);
            final var next = new ArrayList<>(layers);
            // A later stamp through the same holes covers the older stamp completely.
            next.removeIf(layer -> layer.mask().equals(cover));
            next.add(new Layer(cover, rgb));
            return new Face(background, List.copyOf(next), cover);
        }
    }

    public OBCanvasBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        Arrays.fill(faces, BLANK);
    }

    public BlockState original() {
        return original.hasProperty(BlockStateProperties.WATERLOGGED)
                ? original.setValue(BlockStateProperties.WATERLOGGED, getBlockState().getValue(BlockStateProperties.WATERLOGGED))
                : original;
    }

    public Face face(final Direction side) { return faces[side.get3DDataValue()]; }

    public int color(final Direction side) { return face(side).background(); }

    public void remember(final BlockState state) {
        original = state;
        change();
        if (level != null) level.getLightEngine().checkBlock(worldPosition);
    }

    public boolean paint(final int rgb, final Direction... sides) {
        boolean changed = false;
        for (final Direction side : sides) {
            final int index = side.get3DDataValue();
            final Face painted = faces[index].stamp(rgb & 0xFFFFFF);
            changed |= !painted.equals(faces[index]);
            faces[index] = painted;
        }
        if (changed) change();
        return changed;
    }

    public void attach(final Direction side, final OBStencilPattern pattern) {
        final Face face = face(side);
        if (face.cover() != null) return;
        faces[side.get3DDataValue()] = new Face(face.background(), face.layers(), new Mask(pattern, 0));
        change();
    }

    public boolean rotateStencil(final Direction side) {
        final Face face = face(side);
        if (face.cover() == null) return false;
        faces[side.get3DDataValue()] = new Face(face.background(), face.layers(), face.cover().rotate());
        change();
        return true;
    }

    public boolean recoverStencil(final Player player, final Direction side) {
        final Face face = face(side);
        if (face.cover() == null) return false;
        final Face[] remaining = faces.clone();
        remaining[side.get3DDataValue()] = new Face(face.background(), face.layers(), null);
        if (!applyCleanup(player, side, remaining)) return false;
        dropStencil(side, face.cover());
        return true;
    }

    public boolean rinse(final Player player, final Direction side, final boolean all) {
        final Face[] before = faces.clone();
        final Face[] remaining = faces.clone();
        if (all) Arrays.fill(remaining, BLANK);
        else remaining[side.get3DDataValue()] = BLANK;
        if (Arrays.equals(faces, remaining) || !applyCleanup(player, side, remaining)) return false;
        for (final Direction direction : Direction.values()) {
            if (all || direction == side) dropStencil(direction, before[direction.get3DDataValue()].cover());
        }
        return true;
    }

    private boolean applyCleanup(final Player player, final Direction usedFace, final Face[] remaining) {
        if (level == null) return false;
        final BlockState restored = original();
        if (!restored.isAir() && Arrays.stream(remaining).allMatch(Face::empty)) {
            final int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
            final BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, worldPosition, flags);
            restoringOriginal = true;
            try {
                if (!level.setBlock(worldPosition, restored, flags)) return false;
            } finally {
                restoringOriginal = false;
            }
            if (ForgeEventFactory.onBlockPlace(player, snapshot, usedFace) || level.getBlockState(worldPosition) != restored) {
                snapshot.restore(true, true);
                return false;
            }
            level.updateNeighborsAt(worldPosition, restored.getBlock());
            return true;
        }
        System.arraycopy(remaining, 0, faces, 0, faces.length);
        change();
        return true;
    }

    public void dropStencils() {
        if (restoringOriginal) return;
        for (final Direction side : Direction.values()) dropStencil(side, face(side).cover());
    }

    private void dropStencil(final Direction side, @Nullable final Mask cover) {
        if (level != null && cover != null) Block.popResourceFromFace(level, worldPosition, side, OBStencilItem.create(cover.pattern(), 1));
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
        final int[] paint = tag.getIntArray("Paint");
        Arrays.fill(faces, BLANK);
        for (int i = 0; i < Math.min(faces.length, paint.length); i++) {
            if (paint[i] != UNPAINTED) faces[i] = new Face(paint[i] & 0xFFFFFF, List.of(), null);
        }
        final ListTag stencils = tag.getList("Stencils", Tag.TAG_COMPOUND);
        for (int i = 0; i < stencils.size(); i++) {
            final CompoundTag encoded = stencils.getCompound(i);
            final int side = encoded.getByte("Face");
            if (side < 0 || side >= faces.length) continue;
            final var layers = new ArrayList<Layer>();
            final ListTag stamps = encoded.getList("Layers", Tag.TAG_COMPOUND);
            for (int index = 0; index < stamps.size(); index++) {
                final CompoundTag stamp = stamps.getCompound(index);
                final Mask mask = readMask(stamp);
                if (mask == null) continue;
                layers.removeIf(layer -> layer.mask().equals(mask));
                layers.add(new Layer(mask, stamp.getInt("Color") & 0xFFFFFF));
            }
            faces[side] = new Face(faces[side].background(), List.copyOf(layers), readMask(encoded.getCompound("Cover")));
        }
        if (level != null && level.isClientSide()) refreshModel();
    }

    @Nullable
    private static Mask readMask(final CompoundTag tag) {
        final OBStencilPattern pattern = OBStencilPattern.byName(tag.getString("Pattern"));
        return pattern == null ? null : new Mask(pattern, tag.getByte("Rotation") & 3);
    }

    private static CompoundTag writeMask(final Mask mask) {
        final var tag = new CompoundTag();
        tag.putString("Pattern", mask.pattern().getSerializedName());
        tag.putByte("Rotation", (byte) mask.rotation());
        return tag;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) refreshModel();
    }

    private void refreshModel() {
        modelData = ModelData.builder().with(SURFACE, new Surface(original(), List.of(faces))).build();
        requestModelDataUpdate();
    }

    @Override public ModelData getModelData() { return modelData; }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        if (!original.isAir()) tag.put("Original", NbtUtils.writeBlockState(original()));
        tag.putIntArray("Paint", Arrays.stream(faces).mapToInt(Face::background).toArray());
        final var stencils = new ListTag();
        for (int i = 0; i < faces.length; i++) {
            final Face face = faces[i];
            if (!face.decorated()) continue;
            final var encoded = new CompoundTag();
            encoded.putByte("Face", (byte) i);
            if (face.cover() != null) encoded.put("Cover", writeMask(face.cover()));
            final var layers = new ListTag();
            for (final Layer layer : face.layers()) {
                final CompoundTag stamp = writeMask(layer.mask());
                stamp.putInt("Color", layer.color());
                layers.add(stamp);
            }
            if (!layers.isEmpty()) encoded.put("Layers", layers);
            stencils.add(encoded);
        }
        if (!stencils.isEmpty()) tag.put("Stencils", stencils);
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
