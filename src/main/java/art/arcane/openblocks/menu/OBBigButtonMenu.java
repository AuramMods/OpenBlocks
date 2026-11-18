package art.arcane.openblocks.menu;

import art.arcane.openblocks.block.entity.OBBigButtonBlockEntity;
import art.arcane.openblocks.registry.OBMenuTypes;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class OBBigButtonMenu extends AbstractContainerMenu {

    @Nullable private final OBBigButtonBlockEntity button;
    private final ItemStackHandler items;

    public OBBigButtonMenu(final int id, final Inventory inventory) {
        this(id, inventory, null, new ItemStackHandler(OBBigButtonBlockEntity.SLOTS));
    }

    public OBBigButtonMenu(final int id, final Inventory inventory, final OBBigButtonBlockEntity button) {
        this(id, inventory, button, button.items());
    }

    private OBBigButtonMenu(final int id, final Inventory inventory, @Nullable final OBBigButtonBlockEntity button,
            final ItemStackHandler items) {
        super(OBMenuTypes.BIG_BUTTON.get(), id);
        this.button = button;
        this.items = items;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            addSlot(new SlotItemHandler(items, slot, 53 + slot % 4 * 18, 30 + slot / 4 * 18) {
                @Override public void setChanged() {
                    super.setChanged();
                    if (button != null) button.setChanged();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 100 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 158));
    }

    public int pulseLength() { return OBBigButtonBlockEntity.pulseLength(items); }

    @Override
    public boolean stillValid(final Player player) {
        if (button == null) return true;
        final var pos = button.getBlockPos();
        return !button.isRemoved() && button.getLevel() == player.level() && player.level().getBlockEntity(pos) == button
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        final Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        final int count = items.getSlots();
        if (index < count) {
            if (!moveItemStackTo(stack, count, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, count, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
