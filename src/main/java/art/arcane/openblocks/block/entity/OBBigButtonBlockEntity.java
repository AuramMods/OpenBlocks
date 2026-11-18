package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.menu.OBBigButtonMenu;
import art.arcane.openblocks.registry.OBBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class OBBigButtonBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOTS = 8;
    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override protected void onContentsChanged(final int slot) { setChanged(); }
    };
    private LazyOptional<IItemHandler> access = LazyOptional.of(() -> items);

    public OBBigButtonBlockEntity(final boolean wooden, final BlockPos pos, final BlockState state) {
        super((wooden ? OBBlockEntities.BIG_BUTTON_WOOD : OBBlockEntities.BIG_BUTTON).get(), pos, state);
    }

    public ItemStackHandler items() { return items; }
    public int pulseLength() { return pulseLength(items); }

    public static int pulseLength(final IItemHandler items) {
        int total = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) total += items.getStackInSlot(slot).getCount();
        return Math.max(1, total);
    }

    public void dropItems(final Level level) {
        for (int slot = 0; slot < SLOTS; slot++) Block.popResource(level, worldPosition, items.extractItem(slot, items.getSlotLimit(slot), false));
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public OBBigButtonMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new OBBigButtonMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", items.serializeNBT());
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        final var stored = tag.getCompound("inventory").copy();
        stored.putInt("Size", SLOTS);
        items.deserializeNBT(stored);
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction side) {
        return !isRemoved() && capability == ForgeCapabilities.ITEM_HANDLER ? access.cast() : super.getCapability(capability, side);
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
