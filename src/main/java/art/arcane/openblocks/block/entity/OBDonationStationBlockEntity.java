package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.menu.OBUtilityInventoryMenu;
import art.arcane.openblocks.registry.OBBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class OBDonationStationBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(final int slot) { setChanged(); }
    };
    private LazyOptional<IItemHandler> access = LazyOptional.of(() -> items);

    public OBDonationStationBlockEntity(final BlockPos pos, final BlockState state) { super(OBBlockEntities.DONATION_STATION.get(), pos, state); }

    public ItemStackHandler items() { return items; }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public OBUtilityInventoryMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new OBUtilityInventoryMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", items.serializeNBT());
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        final CompoundTag stored = tag.getCompound("inventory").copy();
        stored.putInt("Size", 1);
        items.deserializeNBT(stored);
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction face) {
        return !isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER ? access.cast() : super.getCapability(cap, face);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        access.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        access = LazyOptional.of(() -> items);
    }
}
