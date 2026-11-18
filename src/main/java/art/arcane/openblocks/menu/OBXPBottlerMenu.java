package art.arcane.openblocks.menu;

import art.arcane.openblocks.block.entity.OBXPBottlerBlockEntity;
import art.arcane.openblocks.block.entity.OBXPBottlerBlockEntity.Port;
import art.arcane.openblocks.registry.OBMenuTypes;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class OBXPBottlerMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 2;
    private static final int PROGRESS = OBFluidMenuData.VALUES;
    private static final int PORTS = PROGRESS + 1;
    private static final Port[] PORT_ORDER = Port.values();
    private static final int DATA_COUNT = PORTS + PORT_ORDER.length;
    private static final int AUTOMATIC = Direction.values().length;
    private static final int PORT_ACTIONS = AUTOMATIC + 1;

    @Nullable private final OBXPBottlerBlockEntity bottler;
    private final ContainerData data;

    public OBXPBottlerMenu(final int id, final Inventory inventory) {
        this(id, inventory, null, new ItemStackHandler(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT));
    }

    public OBXPBottlerMenu(final int id, final Inventory inventory, final OBXPBottlerBlockEntity bottler) {
        this(id, inventory, bottler, bottler.bottles(), OBFluidMenuData.of(bottler::contents, bottler::capacity,
                1 + PORT_ORDER.length, index -> index == 0 ? bottler.progress() : bottler.settings(PORT_ORDER[index - 1])));
    }

    private OBXPBottlerMenu(final int id, final Inventory inventory, @Nullable final OBXPBottlerBlockEntity bottler,
            final IItemHandler items, final ContainerData data) {
        super(OBMenuTypes.XP_BOTTLER.get(), id);
        this.bottler = bottler;
        this.data = data;
        addDataSlots(data);
        addSlot(bottleSlot(items, OBXPBottlerBlockEntity.INPUT, 48));
        addSlot(bottleSlot(items, OBXPBottlerBlockEntity.OUTPUT, 110));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 69 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 127));
        }
    }

    private SlotItemHandler bottleSlot(final IItemHandler items, final int index, final int x) {
        return new SlotItemHandler(items, index, x, 30) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return index == OBXPBottlerBlockEntity.INPUT && stack.is(Items.GLASS_BOTTLE);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if (bottler != null) bottler.setChanged();
            }
        };
    }

    public int progress() {
        return data.get(PROGRESS);
    }

    public int capacity() {
        return OBFluidMenuData.capacity(data);
    }

    public FluidStack fluid() {
        return OBFluidMenuData.fluid(data);
    }

    public boolean isSideEnabled(final Port port, final Direction side) {
        return (data.get(PORTS + port.ordinal()) & (1 << side.ordinal())) != 0;
    }

    public boolean isAutomatic(final Port port) {
        return (data.get(PORTS + port.ordinal()) & (1 << AUTOMATIC)) != 0;
    }

    public static int sideButton(final Port port, final Direction side) {
        return port.ordinal() * PORT_ACTIONS + side.ordinal();
    }

    public static int automaticButton(final Port port) {
        return port.ordinal() * PORT_ACTIONS + AUTOMATIC;
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (bottler == null || id < 0 || id >= PORT_ORDER.length * PORT_ACTIONS || !stillValid(player)) return false;
        final Port port = PORT_ORDER[id / PORT_ACTIONS];
        final int action = id % PORT_ACTIONS;
        if (action == AUTOMATIC) bottler.toggleAutomatic(port);
        else bottler.toggleSide(port, Direction.from3DDataValue(action));
        return true;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (bottler == null) return true;
        final var pos = bottler.getBlockPos();
        return !bottler.isRemoved() && bottler.getLevel() == player.level() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64
                && player.level().getBlockEntity(pos) == bottler;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        final Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, OBXPBottlerBlockEntity.INPUT, OBXPBottlerBlockEntity.OUTPUT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
