package art.arcane.openblocks.menu;

import art.arcane.openblocks.block.OBVacuumHopperBlock;
import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity;
import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity.Output;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class OBVacuumHopperMenu extends AbstractContainerMenu {

    private static final int HOPPER_SLOTS = OBVacuumHopperBlockEntity.SLOTS;
    private static final Output[] OUTPUTS = Output.values();
    private static final int SIDES = Direction.values().length;
    private static final int ENABLED = OBFluidMenuData.VALUES + OUTPUTS.length;
    private static final int DATA_COUNT = ENABLED + 1;

    @Nullable private final OBVacuumHopperBlockEntity hopper;
    private final ContainerData data;

    public OBVacuumHopperMenu(final int id, final Inventory inventory) {
        this(id, inventory, null, new ItemStackHandler(HOPPER_SLOTS), new SimpleContainerData(DATA_COUNT));
    }

    public OBVacuumHopperMenu(final int id, final Inventory inventory, final OBVacuumHopperBlockEntity hopper) {
        this(id, inventory, hopper, hopper.items(), OBFluidMenuData.of(hopper::contents, hopper::capacity,
                OUTPUTS.length + 1, index -> index == OUTPUTS.length
                        ? (hopper.getBlockState().getValue(OBVacuumHopperBlock.ENABLED) ? 1 : 0) : hopper.outputMask(OUTPUTS[index])));
    }

    private OBVacuumHopperMenu(final int id, final Inventory inventory, @Nullable final OBVacuumHopperBlockEntity hopper,
            final ItemStackHandler items, final ContainerData data) {
        super(OBMenuTypes.VACUUM_HOPPER.get(), id);
        this.hopper = hopper;
        this.data = data;
        addDataSlots(data);
        for (int index = 0; index < HOPPER_SLOTS; index++) {
            addSlot(new SlotItemHandler(items, index, 44 + (index % 5) * 18, 20 + (index / 5) * 18) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    if (hopper != null) hopper.setChanged();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 69 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 127));
    }

    public int capacity() {
        return OBFluidMenuData.capacity(data);
    }

    public FluidStack fluid() {
        return OBFluidMenuData.fluid(data);
    }

    public boolean isEnabled() {
        return data.get(ENABLED) != 0;
    }

    public boolean isSideEnabled(final Output output, final Direction side) {
        return (data.get(OBFluidMenuData.VALUES + output.ordinal()) & (1 << side.ordinal())) != 0;
    }

    public static int sideButton(final Output output, final Direction side) {
        return output.ordinal() * SIDES + side.ordinal();
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (hopper == null || id < 0 || id >= OUTPUTS.length * SIDES || !stillValid(player)) return false;
        hopper.toggleOutput(OUTPUTS[id / SIDES], Direction.from3DDataValue(id % SIDES));
        return true;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (hopper == null) return true;
        final var pos = hopper.getBlockPos();
        return !hopper.isRemoved() && hopper.getLevel() == player.level() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64
                && player.level().getBlockEntity(pos) == hopper;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        final Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < HOPPER_SLOTS) {
            if (!moveItemStackTo(stack, HOPPER_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, HOPPER_SLOTS, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
