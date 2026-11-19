package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.item.OBPaintCanItem;
import art.arcane.openblocks.item.OBPaintColors;
import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OBPaintCanBlockEntity extends BlockEntity {

    private int color = OBPaintColors.WHITE;
    private int amount;

    public OBPaintCanBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.PAINT_CAN.get(), pos, state);
    }

    public int color() { return color; }
    public int amount() { return amount; }

    public void fillFrom(final ItemStack stack) {
        color = OBPaintCanItem.color(stack);
        amount = OBPaintCanItem.amount(stack);
        changed();
    }

    public void takeCharge() {
        if (amount > 0) {
            amount--;
            if (amount != 0) changed();
        }
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public ItemStack createStack() {
        return OBPaintCanItem.create(color, amount);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        color = tag.contains("color", Tag.TAG_ANY_NUMERIC) ? tag.getInt("color") & 0xFFFFFF : OBPaintColors.WHITE;
        amount = Mth.clamp(tag.getInt("amount"), 0, OBPaintCanItem.FULL);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("color", color);
        tag.putInt("amount", amount);
    }

    @Override
    public void onDataPacket(final Connection connection, final ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(connection, packet);
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
