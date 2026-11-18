package art.arcane.openblocks.menu;

import art.arcane.openblocks.block.entity.OBDonationStationBlockEntity;
import art.arcane.openblocks.block.entity.OBSprinklerBlockEntity;
import art.arcane.openblocks.registry.OBMenuTypes;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class OBUtilityInventoryMenu extends AbstractContainerMenu {

    @Nullable private final BlockEntity owner;
    @Nullable private final ContainerData data;
    private final boolean sprinkler;
    private final int machineSlots;

    public static OBUtilityInventoryMenu sprinkler(final int id, final Inventory inventory) {
        return new OBUtilityInventoryMenu(id, inventory, null, new ItemStackHandler(OBSprinklerBlockEntity.SLOTS), true,
                new SimpleContainerData(OBFluidMenuData.VALUES + 1));
    }

    public static OBUtilityInventoryMenu donationStation(final int id, final Inventory inventory) {
        return new OBUtilityInventoryMenu(id, inventory, null, new ItemStackHandler(1), false, null);
    }

    public OBUtilityInventoryMenu(final int id, final Inventory inventory, final OBSprinklerBlockEntity sprinkler) {
        this(id, inventory, sprinkler, sprinkler.fertilizer(), true,
                OBFluidMenuData.of(sprinkler::contents, sprinkler::capacity, 1, index -> sprinkler.isActive() ? 1 : 0));
    }

    public OBUtilityInventoryMenu(final int id, final Inventory inventory, final OBDonationStationBlockEntity station) {
        this(id, inventory, station, station.items(), false, null);
    }

    private OBUtilityInventoryMenu(final int id, final Inventory inventory, @Nullable final BlockEntity owner,
            final ItemStackHandler items, final boolean sprinkler, @Nullable final ContainerData data) {
        super((sprinkler ? OBMenuTypes.SPRINKLER : OBMenuTypes.DONATION_STATION).get(), id);
        this.owner = owner;
        this.sprinkler = sprinkler;
        this.data = data;
        machineSlots = items.getSlots();
        if (data != null) addDataSlots(data);
        for (int index = 0; index < machineSlots; index++) {
            final int x = sprinkler ? 62 + index % 3 * 18 : 30;
            final int y = sprinkler ? 18 + index / 3 * 18 : 29;
            addSlot(new SlotItemHandler(items, index, x, y) {
                @Override public boolean mayPlace(final ItemStack stack) {
                    return (!sprinkler || stack.is(Items.BONE_MEAL)) && super.mayPlace(stack);
                }

                @Override public void setChanged() {
                    super.setChanged();
                    if (owner != null) owner.setChanged();
                }
            });
        }
        final int startY = sprinkler ? 85 : 90;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, startY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, startY + 58));
    }

    public boolean isSprinkler() { return sprinkler; }
    public ItemStack sample() { return getSlot(0).getItem(); }
    public FluidStack fluid() { return data == null ? FluidStack.EMPTY : OBFluidMenuData.fluid(data); }
    public int capacity() { return data == null ? 0 : OBFluidMenuData.capacity(data); }
    public boolean isActive() { return data != null && data.get(OBFluidMenuData.VALUES) != 0; }

    @Override
    public boolean stillValid(final Player player) {
        if (owner == null) return true;
        final var pos = owner.getBlockPos();
        return !owner.isRemoved() && owner.getLevel() == player.level() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64
                && player.level().getBlockEntity(pos) == owner;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        final Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if ((sprinkler && !stack.is(Items.BONE_MEAL)) || !moveItemStackTo(stack, 0, machineSlots, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
