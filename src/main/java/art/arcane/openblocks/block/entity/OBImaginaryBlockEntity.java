package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBImaginaryBlock.Shape;
import art.arcane.openblocks.item.OBImaginationGlasses;
import art.arcane.openblocks.registry.OBBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OBImaginaryBlockEntity extends BlockEntity {

    @Nullable private Integer color;
    private Shape shape = Shape.BLOCK;
    private boolean inverted;

    public OBImaginaryBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.IMAGINARY.get(), pos, state);
    }

    @Nullable public Integer color() { return color; }
    public Shape shape() { return shape; }
    public boolean inverted() { return inverted; }

    public void setDesign(@Nullable final Integer rgb, final Shape shape, final boolean inverted) {
        color = rgb;
        this.shape = shape;
        this.inverted = inverted;
        setChanged();
        if (level != null && !level.captureBlockSnapshots) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean visibleTo(final Player player) {
        if (player.isSpectator()) return true;
        final var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.getItem() instanceof OBImaginationGlasses glasses
                && glasses.kind() == OBImaginationGlasses.Kind.TECHNICOLOR || selectableBy(player);
    }

    public boolean selectableBy(final Player player) {
        final var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof OBImaginationGlasses glasses)) return inverted;
        return switch (glasses.kind()) {
            case PENCIL -> (color == null) ^ inverted;
            case CRAYON -> (color != null && color == OBImaginationGlasses.color(helmet)) ^ inverted;
            case TECHNICOLOR -> inverted;
            case SERIOUS -> true;
        };
    }

    public boolean solidFor(final Player player) {
        return color == null || selectableBy(player);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        if (color != null) tag.putInt("Color", color);
        tag.putByte("Shape", (byte) shape.ordinal());
        tag.putBoolean("IsInverted", inverted);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        color = tag.contains("Color", Tag.TAG_ANY_NUMERIC) ? tag.getInt("Color") & 0xFFFFFF : null;
        inverted = tag.getBoolean("IsInverted");
        shape = tag.contains("Type", Tag.TAG_BYTE) ? switch (tag.getByte("Type")) {
            case 1 -> tag.getFloat("PanelHeight") < 0.75F ? Shape.HALF_PANEL : Shape.PANEL;
            case 2 -> Shape.STAIRS;
            default -> Shape.BLOCK;
        } : Shape.fromId(tag.getByte("Shape"));
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
