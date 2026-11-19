package art.arcane.openblocks.menu;

import art.arcane.openblocks.block.entity.OBProjectorBlockEntity;
import art.arcane.openblocks.registry.OBMenuTypes;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class OBProjectorMenu extends AbstractContainerMenu {

    public static final int ROTATE_LEFT = 0;
    public static final int ROTATE_RIGHT = 1;

    private final Level level;
    private final BlockPos pos;
    @Nullable private final OBProjectorBlockEntity serverProjector;
    private int clientRotation;

    public OBProjectorMenu(final int id, final Inventory player, final FriendlyByteBuf data) {
        this(id, player, data.readBlockPos(), null, new ItemStackHandler(1) {
            @Override public int getSlotLimit(final int slot) { return 1; }
            @Override public boolean isItemValid(final int slot, final ItemStack stack) { return OBProjectorBlockEntity.acceptsMap(stack); }
        });
    }

    public OBProjectorMenu(final int id, final Inventory player, final OBProjectorBlockEntity projector) {
        this(id, player, projector.getBlockPos(), projector, projector.inventory());
    }

    private OBProjectorMenu(final int id, final Inventory player, final BlockPos pos, @Nullable final OBProjectorBlockEntity projector,
            final ItemStackHandler maps) {
        super(OBMenuTypes.PROJECTOR.get(), id);
        this.level = player.player.level();
        this.pos = pos;
        this.serverProjector = projector;
        addSlot(new SlotItemHandler(maps, 0, 79, 130));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 152 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 210));
        addDataSlot(new DataSlot() {
            @Override public int get() { return rotation(); }
            @Override public void set(final int value) { clientRotation = value & 3; }
        });
    }

    public int rotation() {
        return serverProjector == null ? clientRotation : serverProjector.rotation();
    }

    @Nullable
    public OBProjectorBlockEntity projector() {
        return level.getBlockEntity(pos) instanceof OBProjectorBlockEntity projector ? projector : null;
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (serverProjector == null || !stillValid(player) || (id != ROTATE_LEFT && id != ROTATE_RIGHT)) return false;
        serverProjector.rotate(id == ROTATE_LEFT ? -1 : 1);
        return true;
    }

    @Override
    public boolean stillValid(final Player player) {
        return serverProjector == null || !serverProjector.isRemoved() && player.level() == level
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64 && projector() == serverProjector;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        final Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
