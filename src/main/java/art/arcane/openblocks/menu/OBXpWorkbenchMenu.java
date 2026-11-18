package art.arcane.openblocks.menu;

import art.arcane.openblocks.block.entity.OBXpWorkbenchBlockEntity;
import art.arcane.openblocks.block.entity.OBXpWorkbenchBlockEntity.Port;
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
import net.minecraftforge.common.Tags;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class OBXpWorkbenchMenu extends AbstractContainerMenu {

    private static final Direction[] FACES = Direction.values();
    private static final Port[] PORTS = Port.values();
    private static final int AUTO_BUTTONS = PORTS.length * FACES.length;
    private static final int TIER_BUTTONS = AUTO_BUTTONS + PORTS.length;
    private static final int LIMIT_BUTTONS = TIER_BUTTONS + 3;
    private static final int SETTINGS = PORTS.length + 3;
    private static final int SLOTS = OBXpWorkbenchBlockEntity.SLOT_COUNT;

    @Nullable private final OBXpWorkbenchBlockEntity machine;
    private final boolean enchanting;
    private final ContainerData data;

    public static OBXpWorkbenchMenu anvil(final int id, final Inventory player) { return new OBXpWorkbenchMenu(id, player, null, false); }
    public static OBXpWorkbenchMenu enchantmentTable(final int id, final Inventory player) { return new OBXpWorkbenchMenu(id, player, null, true); }

    public OBXpWorkbenchMenu(final int id, final Inventory player, final OBXpWorkbenchBlockEntity machine) {
        this(id, player, machine, machine.enchanting());
    }

    private OBXpWorkbenchMenu(final int id, final Inventory player, @Nullable final OBXpWorkbenchBlockEntity machine, final boolean enchanting) {
        super((enchanting ? OBMenuTypes.AUTO_ENCHANTMENT_TABLE : OBMenuTypes.AUTO_ANVIL).get(), id);
        this.enchanting = enchanting;
        this.machine = machine;
        data = machine == null ? new SimpleContainerData(OBFluidMenuData.VALUES + SETTINGS)
                : OBFluidMenuData.of(machine::contents, machine::capacity, SETTINGS, index -> switch (index - PORTS.length) {
                    case 0 -> machine.tier();
                    case 1 -> machine.powerLimit();
                    case 2 -> machine.availablePower();
                    default -> machine.settings(PORTS[index]);
                });
        addDataSlots(data);
        final ItemStackHandler items = machine != null ? machine.items() : new ItemStackHandler(SLOTS) {
            @Override
            public boolean isItemValid(final int slot, final ItemStack stack) { return OBXpWorkbenchBlockEntity.accepts(enchanting, slot, stack); }
            @Override
            public int getSlotLimit(final int slot) { return enchanting && slot == Port.TOOL.ordinal() ? 1 : super.getSlotLimit(slot); }
        };
        final int[] slotX = {12, 60, 112};
        for (int slot = 0; slot < SLOTS; slot++) {
            addSlot(new SlotItemHandler(items, slot, slotX[slot], 33) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    if (machine != null) machine.setChanged();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(player, 9 + row * 9 + column, 8 + column * 18, 94 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(player, column, 8 + column * 18, 152));
    }

    public boolean enchanting() { return enchanting; }
    public FluidStack fluid() { return OBFluidMenuData.fluid(data); }
    public int capacity() { return OBFluidMenuData.capacity(data); }
    public int tier() { return data.get(OBFluidMenuData.VALUES + PORTS.length); }
    public int powerLimit() { return data.get(OBFluidMenuData.VALUES + PORTS.length + 1); }
    public int availablePower() { return data.get(OBFluidMenuData.VALUES + PORTS.length + 2); }

    public boolean isSideEnabled(final Port port, final Direction face) {
        return (data.get(OBFluidMenuData.VALUES + port.ordinal()) & 1 << face.ordinal()) != 0;
    }

    public boolean isAutomatic(final Port port) {
        return (data.get(OBFluidMenuData.VALUES + port.ordinal()) & 1 << FACES.length) != 0;
    }

    public static int sideButton(final Port port, final Direction face) { return port.ordinal() * FACES.length + face.ordinal(); }
    public static int autoButton(final Port port) { return AUTO_BUTTONS + port.ordinal(); }
    public static int tierButton(final int tier) { return TIER_BUTTONS + tier - 1; }
    public static int powerButton(final int power) { return LIMIT_BUTTONS + power - 1; }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (machine == null || player.isSpectator() || !stillValid(player) || id < 0) return false;
        if (id < AUTO_BUTTONS) machine.toggleSide(PORTS[id / FACES.length], FACES[id % FACES.length]);
        else if (id < TIER_BUTTONS) machine.toggleAutomatic(PORTS[id - AUTO_BUTTONS]);
        else if (enchanting && id < LIMIT_BUTTONS) machine.selectTier(id - TIER_BUTTONS + 1);
        else if (enchanting && id < LIMIT_BUTTONS + OBXpWorkbenchBlockEntity.MAXIMUM_POWER) machine.setPowerLimit(id - LIMIT_BUTTONS + 1);
        else return false;
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
        } else {
            final int target = enchanting ? (stack.is(Tags.Items.ENCHANTING_FUELS) ? Port.REAGENT.ordinal() : Port.TOOL.ordinal())
                    : (getSlot(Port.TOOL.ordinal()).hasItem() ? Port.REAGENT.ordinal() : Port.TOOL.ordinal());
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
