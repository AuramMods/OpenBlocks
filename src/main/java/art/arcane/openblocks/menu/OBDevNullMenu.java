package art.arcane.openblocks.menu;

import art.arcane.openblocks.advancement.OBCriterions;
import art.arcane.openblocks.item.OBDevNullItem;
import art.arcane.openblocks.registry.OBMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class OBDevNullMenu extends AbstractContainerMenu {

    private final Inventory playerInventory;
    private final int hotbarSlot;
    private final ItemStack device;
    private final SimpleContainer contents = new SimpleContainer(1);
    private boolean refreshing;

    public OBDevNullMenu(final int id, final Inventory inventory, final FriendlyByteBuf data) {
        this(id, inventory, data.readVarInt());
    }

    public OBDevNullMenu(final int id, final Inventory inventory, final int hotbarSlot) {
        super(OBMenuTypes.DEV_NULL.get(), id);
        this.playerInventory = inventory;
        this.hotbarSlot = hotbarSlot;
        this.device = inventory.getItem(hotbarSlot);
        contents.setItem(0, OBDevNullItem.getStored(device));
        contents.addListener(container -> saveContents());

        addSlot(new Slot(contents, 0, 80, 22) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return OBDevNullItem.contents(stack).depth() < OBDevNullItem.STACK_LIMIT;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 55 + row * 18));
            }
        }
        for (int column = 0; column < Inventory.getSelectionSize(); column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 113) {
                @Override public boolean mayPlace(final ItemStack stack) { return !isProtected(index); }
                @Override public boolean mayPickup(final Player player) { return !isProtected(index); }
                @Override public boolean isActive() { return !isProtected(index); }
            });
        }
    }

    private boolean isProtected(final int slot) {
        return slot == 28 + hotbarSlot;
    }

    private void saveContents() {
        if (refreshing || !(playerInventory.player instanceof ServerPlayer player) || !stillValid(player)) return;
        final ItemStack stored = contents.getItem(0);
        if (ItemStack.matches(stored, OBDevNullItem.getStored(device))) return;
        OBDevNullItem.setStored(device, stored);
        playerInventory.setChanged();
        OBCriterions.DEV_NULL_STACK.trigger(player, OBDevNullItem.contents(device).depth());
    }

    private void refreshContents() {
        if (playerInventory.player.level().isClientSide || !stillValid(playerInventory.player)) return;
        final ItemStack stored = OBDevNullItem.getStored(device);
        if (ItemStack.matches(contents.getItem(0), stored)) return;
        refreshing = true;
        try {
            contents.setItem(0, stored);
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void broadcastChanges() {
        refreshContents();
        super.broadcastChanges();
    }

    @Override
    public void broadcastFullState() {
        refreshContents();
        super.broadcastFullState();
    }

    @Override
    public boolean stillValid(final Player player) {
        return player == playerInventory.player && (player.level().isClientSide
                || (!device.isEmpty() && device.getItem() instanceof OBDevNullItem && playerInventory.getItem(hotbarSlot) == device));
    }

    @Override
    public void clicked(final int slot, final int button, final ClickType type, final Player player) {
        if (!stillValid(player) || isProtected(slot) || (type == ClickType.SWAP && button == hotbarSlot)) return;
        refreshContents();
        super.clicked(slot, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player) || isProtected(index)) return ItemStack.EMPTY;
        refreshContents();
        final Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final ItemStack current = slot.getItem();
        final ItemStack original = current.copy();

        // Exclude the backing item from both halves of the hotbar when routing a shift-click.
        final int protectedSlot = 28 + hotbarSlot;
        if (index == 0) {
            final boolean moved = moveItemStackTo(current, protectedSlot + 1, slots.size(), true);
            final boolean movedEarlier = !current.isEmpty() && moveItemStackTo(current, 1, protectedSlot, true);
            if (!moved && !movedEarlier) return ItemStack.EMPTY;
        } else {
            if (!getSlot(0).mayPlace(current) || !moveItemStackTo(current, 0, 1, false)) return ItemStack.EMPTY;
        }

        if (current.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, current);
        return original;
    }
}
