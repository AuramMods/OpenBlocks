package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OBFlagBlockEntity extends BlockEntity {

    private int yaw;

    public OBFlagBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.FLAG.get(), pos, state);
    }

    public int yaw() {
        return yaw;
    }

    public void setYaw(final int angle) {
        final int next = Mth.wrapDegrees(angle);
        if (yaw == next) return;
        yaw = next;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("yaw", yaw);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        yaw = Mth.wrapDegrees(tag.getInt("yaw"));
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
