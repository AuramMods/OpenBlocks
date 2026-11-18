package art.arcane.openblocks.menu;

import art.arcane.openblocks.block.entity.OBRedstoneMachineBlockEntity;
import art.arcane.openblocks.registry.OBMenuTypes;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class OBRedstoneMachineMenu extends AbstractContainerMenu {

    public static final int TOGGLE_REDSTONE_SCALING = 0;
    private static final int SLOTS = OBRedstoneMachineBlockEntity.INVENTORY_SIZE;
    private static final int SCALE = 0;
    private static final int SPEED = 1;
    private static final int MAXIMUM = 3;
    private static final int SETTINGS = 5;

    @Nullable private final OBRedstoneMachineBlockEntity machine;
    private final boolean dropper;
    @Nullable private final ContainerData data;

    public static OBRedstoneMachineMenu blockPlacer(final int id, final Inventory inventory) {
        return new OBRedstoneMachineMenu(id, inventory, null, false);
    }

    public static OBRedstoneMachineMenu itemDropper(final int id, final Inventory inventory) {
        return new OBRedstoneMachineMenu(id, inventory, null, true);
    }

    public OBRedstoneMachineMenu(final int id, final Inventory inventory, @Nullable final OBRedstoneMachineBlockEntity machine,
            final boolean dropper) {
        super((dropper ? OBMenuTypes.ITEM_DROPPER : OBMenuTypes.BLOCK_PLACER).get(), id);
        this.machine = machine;
        this.dropper = dropper;
        this.data = dropper ? machine == null ? new SimpleContainerData(SETTINGS) : settingsOf(machine) : null;
        if (data != null) addDataSlots(data);
        final ItemStackHandler items = machine == null ? new ItemStackHandler(SLOTS) : machine.items();
        final int machineX = dropper ? 8 : 62;
        final int playerX = dropper ? 26 : 8;
        for (int index = 0; index < SLOTS; index++) {
            final int slot = index;
            addSlot(new SlotItemHandler(items, slot, machineX + (slot % 3) * 18, 18 + (slot / 3) * 18) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    if (machine != null) machine.inventoryChanged(slot);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, playerX + column * 18, 85 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, playerX + column * 18, 143));
    }

    public boolean isDropper() {
        return dropper;
    }

    public int speedTenths() {
        return readInt(SPEED);
    }

    public int maxSpeedTenths() {
        return readInt(MAXIMUM);
    }

    public boolean scalesWithRedstone() {
        return data != null && data.get(SCALE) != 0;
    }

    private int readInt(final int index) {
        return data == null ? 0 : (data.get(index) & 0xffff) | (data.get(index + 1) << Short.SIZE);
    }

    private static ContainerData settingsOf(final OBRedstoneMachineBlockEntity machine) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case SCALE -> machine.scalesWithRedstone() ? 1 : 0;
                    case SPEED -> (short) machine.speedTenths();
                    case SPEED + 1 -> (short) (machine.speedTenths() >>> Short.SIZE);
                    case MAXIMUM -> (short) OBRedstoneMachineBlockEntity.maxSpeedTenths();
                    case MAXIMUM + 1 -> (short) (OBRedstoneMachineBlockEntity.maxSpeedTenths() >>> Short.SIZE);
                    default -> 0;
                };
            }

            @Override
            public void set(final int index, final int value) {}

            @Override
            public int getCount() {
                return SETTINGS;
            }
        };
    }

    public void setLaunchSpeed(final Player player, final int speed) {
        if (dropper && machine != null && !player.isSpectator() && stillValid(player)) machine.setSpeedTenths(speed);
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (id != TOGGLE_REDSTONE_SCALING || !dropper || machine == null || player.isSpectator() || !stillValid(player)) return false;
        machine.toggleRedstoneScaling();
        return true;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (machine == null) return true;
        final var pos = machine.getBlockPos();
        return !machine.isRemoved() && machine.getLevel() == player.level() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64
                && player.level().getBlockEntity(pos) == machine;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        final Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, SLOTS, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
