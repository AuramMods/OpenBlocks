package art.arcane.openblocks.menu;

import art.arcane.openblocks.block.entity.OBDrawingTableBlockEntity;
import art.arcane.openblocks.item.OBStencilPattern;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBMenuTypes;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class OBDrawingTableMenu extends AbstractContainerMenu {

    public static final int CYCLE_MODE = 0;
    public static final int PREVIOUS = 1;
    public static final int NEXT = 2;
    private static final int INPUT = 0;
    private static final int RESULT = 1;
    private static final int PLAYER_START = 2;
    private static final int DATA_COUNT = 3;

    private final BlockPos pos;
    private final Level level;
    @Nullable private final OBDrawingTableBlockEntity table;
    private final ResultContainer result = new ResultContainer();
    private final ContainerData data;

    public OBDrawingTableMenu(final int id, final Inventory inventory, final FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos(), null, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
    }

    public OBDrawingTableMenu(final int id, final Inventory inventory, final OBDrawingTableBlockEntity table) {
        this(id, inventory, table.getBlockPos(), table, table.input(), new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case 0 -> table.glyphMode() ? 1 : 0;
                    case 1 -> table.pattern().ordinal();
                    default -> table.glyph();
                };
            }

            @Override public int getCount() { return DATA_COUNT; }
            @Override public void set(final int index, final int value) {}
        });
    }

    private OBDrawingTableMenu(final int id, final Inventory inventory, final BlockPos pos,
            @Nullable final OBDrawingTableBlockEntity table, final Container input, final ContainerData data) {
        super(OBMenuTypes.DRAWING_TABLE.get(), id);
        this.level = inventory.player.level();
        this.pos = pos;
        this.table = table;
        this.data = data;
        addSlot(new Slot(input, INPUT, 63, 34) {
            @Override public boolean mayPlace(final ItemStack stack) { return stack.is(OBItems.UNPREPARED_STENCIL.get()); }
        });
        addSlot(new OutputSlot());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 124 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 182));
        addDataSlots(data);
        refresh();
    }

    public boolean glyphMode() { return data.get(0) != 0; }
    public OBStencilPattern pattern() { return OBStencilPattern.cycle(data.get(1)); }
    public int glyph() { return data.get(2); }

    @Nullable
    public OBDrawingTableBlockEntity table() {
        return level.getBlockEntity(pos) instanceof OBDrawingTableBlockEntity found ? found : null;
    }

    private void refresh() {
        if (table != null) result.setItem(0, table.result());
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public void clicked(final int index, final int button, final ClickType type, final Player player) {
        if (!stillValid(player)) return;
        // A different viewer may have used the same stock since the last menu update.
        refresh();
        super.clicked(index, button, type, player);
        refresh();
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (table == null || !stillValid(player)) return false;
        switch (id) {
            case CYCLE_MODE -> table.cycleMode();
            case PREVIOUS -> table.cycleSelection(-1);
            case NEXT -> table.cycleSelection(1);
            default -> { return false; }
        }
        broadcastChanges();
        return true;
    }

    public void print(final Player player, final String text) {
        if (table != null && stillValid(player)) {
            table.print(text);
            broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(final Player player) {
        return table == null || !table.isRemoved() && player.level() == level && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64 && table() == table;
    }

    @Override
    public boolean canTakeItemForPickAll(final ItemStack stack, final Slot slot) { return slot != slots.get(RESULT); }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        refresh();
        final Slot slot = getSlot(index);
        final ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        final ItemStack original = stack.copy();
        if (index < PLAYER_START ? !moveItemStackTo(stack, PLAYER_START, slots.size(), true) : !moveItemStackTo(stack, INPUT, RESULT, false)) {
            return ItemStack.EMPTY;
        }
        if (index == RESULT) slot.onQuickCraft(stack, original);
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        refresh();
        return original;
    }

    private final class OutputSlot extends Slot {

        private int removed;

        private OutputSlot() { super(result, 0, 97, 34); }

        @Override public boolean mayPlace(final ItemStack stack) { return false; }

        @Override
        public ItemStack remove(final int count) {
            final ItemStack taken = getItem().split(count);
            removed += taken.getCount();
            return taken;
        }

        @Override protected void onQuickCraft(final ItemStack stack, final int count) { removed += count; }
        @Override protected void onSwapCraft(final int count) { removed += count; }

        @Override
        public void onTake(final Player player, final ItemStack stack) {
            final int consumed = removed;
            removed = 0;
            if (table != null) table.consume(consumed);
            super.onTake(player, stack);
        }
    }
}
