package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.menu.OBXPBottlerMenu;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBSounds;
import art.arcane.openblocks.world.OBLiquidExperience;
import art.arcane.openblocks.world.OBMachineItems;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class OBXPBottlerBlockEntity extends BlockEntity implements MenuProvider {

    public enum Port { INPUT, XP, OUTPUT }

    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final int PROCESS_TICKS = 40;
    private static final int FLUID_PER_TICK = 10;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int ALL_SIDES = (1 << DIRECTIONS.length) - 1;
    private static final int ALL_PORTS = (1 << Port.values().length) - 1;
    private static final ItemStack RESULT = new ItemStack(Items.EXPERIENCE_BOTTLE);

    private final ItemStackHandler bottles = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return slot == INPUT && stack.is(Items.GLASS_BOTTLE);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            setChanged();
        }
    };
    private int nominalCapacity = OBLiquidExperience.bottleCapacity();
    private final FluidTank tank = new FluidTank(nominalCapacity, fluid -> OBLiquidExperience.ratio(fluid) > 0) {
        @Override
        protected void onContentsChanged() {
            setCapacity(Math.max(nominalCapacity, getFluidAmount()));
            setChanged();
        }
    };
    private final int[] sides = new int[Port.values().length];
    private int automatic;
    private int progress;

    private final Map<Direction, LazyOptional<IItemHandler>> itemFaces = new EnumMap<>(Direction.class);
    private final Map<Direction, LazyOptional<IFluidHandler>> fluidFaces = new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> unsidedItems = LazyOptional.of(() -> new ItemPort(null));
    private LazyOptional<IFluidHandler> unsidedFluid = LazyOptional.of(() -> new FluidPort(null));

    public OBXPBottlerBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.XP_BOTTLER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        nominalCapacity = OBLiquidExperience.bottleCapacity();
        tank.setCapacity(Math.max(nominalCapacity, tank.getFluidAmount()));
    }

    public ItemStackHandler bottles() {
        return bottles;
    }

    public FluidStack contents() {
        return tank.getFluid().copy();
    }

    public int capacity() {
        return tank.getCapacity();
    }

    public int progress() {
        return progress;
    }

    public int settings(final Port port) {
        return sides[port.ordinal()] | (isAutomatic(port) ? 1 << DIRECTIONS.length : 0);
    }

    private boolean hasSide(final Port port, @Nullable final Direction face) {
        return !isRemoved() && (face == null || (sides[port.ordinal()] & (1 << face.ordinal())) != 0);
    }

    private boolean isAutomatic(final Port port) {
        return (automatic & (1 << port.ordinal())) != 0;
    }

    public void toggleSide(final Port port, final Direction face) {
        sides[port.ordinal()] ^= 1 << face.ordinal();
        if (port == Port.XP) {
            final var previous = fluidFaces.remove(face);
            if (previous != null) previous.invalidate();
        } else {
            final var previous = itemFaces.remove(face);
            if (previous != null) previous.invalidate();
        }
        setChanged();
    }

    public void toggleAutomatic(final Port port) {
        automatic ^= 1 << port.ordinal();
        setChanged();
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBXPBottlerBlockEntity bottler) {
        if (bottler.isAutomatic(Port.XP) && bottler.tank.getSpace() > 0) bottler.pullFluid(level, pos);
        if (bottler.isAutomatic(Port.OUTPUT) && !bottler.bottles.getStackInSlot(OUTPUT).isEmpty()) bottler.ejectBottle(level, pos);
        if (bottler.isAutomatic(Port.INPUT) && bottler.bottles.getStackInSlot(INPUT).isEmpty()) bottler.pullBottle(level, pos);
        bottler.work(level, pos);
    }

    private void pullFluid(final Level level, final BlockPos pos) {
        final int first = level.random.nextInt(DIRECTIONS.length);
        int remaining = FLUID_PER_TICK;
        for (int index = 0; index < DIRECTIONS.length && remaining > 0 && tank.getSpace() > 0; index++) {
            final Direction face = DIRECTIONS[(index + first) % DIRECTIONS.length];
            if (hasSide(Port.XP, face)) {
                remaining -= OBLiquidExperience.pull(level, pos.relative(face), face.getOpposite(), tank, remaining);
            }
        }
    }

    private void pullBottle(final Level level, final BlockPos pos) {
        final Direction face = OBMachineItems.chooseFace(sides[Port.INPUT.ordinal()], level.random);
        if (face == null) return;
        final IItemHandler source = OBMachineItems.adjacent(level, pos, face);
        if (source != null) OBMachineItems.pullOne(level, pos, source, bottles, INPUT);
    }

    private void ejectBottle(final Level level, final BlockPos pos) {
        final Direction face = OBMachineItems.chooseFace(sides[Port.OUTPUT.ordinal()], level.random);
        if (face == null) return;
        final IItemHandler destination = OBMachineItems.adjacent(level, pos, face);
        if (destination != null) OBMachineItems.push(level, pos, bottles, OUTPUT, destination, 1);
    }

    private void work(final Level level, final BlockPos pos) {
        final ItemStack output = bottles.getStackInSlot(OUTPUT);
        final int cost = OBLiquidExperience.ratio(tank.getFluid()) * OBLiquidExperience.XP_PER_BOTTLE;
        final boolean canWork = cost > 0 && tank.getFluidAmount() >= cost && bottles.getStackInSlot(INPUT).is(Items.GLASS_BOTTLE)
                && (output.isEmpty() || (ItemStack.isSameItemSameTags(output, RESULT) && output.getCount() < output.getMaxStackSize()));
        if (!canWork) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (progress < PROCESS_TICKS) {
            progress++;
            setChanged();
            return;
        }

        progress = 0;
        tank.drain(cost, FluidAction.EXECUTE);
        bottles.extractItem(INPUT, 1, false);
        bottles.setStackInSlot(OUTPUT, RESULT.copyWithCount(output.getCount() + 1));
        level.playSound(null, pos, OBSounds.BOTTLER_SIGNAL.get(), SoundSource.BLOCKS, 0.5F, 0.8F);
    }

    public void dropBottles(final Level level, final BlockPos pos) {
        for (int slot = 0; slot < bottles.getSlots(); slot++) {
            Block.popResource(level, pos, bottles.extractItem(slot, bottles.getSlotLimit(slot), false));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory playerInventory, final Player player) {
        return new OBXPBottlerMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", bottles.serializeNBT());
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.putIntArray("sides", sides);
        tag.putInt("automatic", automatic);
        tag.putInt("progress", progress);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        final CompoundTag stored = tag.getCompound("inventory").copy();
        stored.putInt("Size", bottles.getSlots());
        bottles.deserializeNBT(stored);
        tank.readFromNBT(tag.getCompound("tank"));
        tank.setCapacity(Math.max(nominalCapacity, tank.getFluidAmount()));
        final int[] savedSides = tag.getIntArray("sides");
        for (int index = 0; index < sides.length; index++) sides[index] = index < savedSides.length ? savedSides[index] & ALL_SIDES : 0;
        automatic = tag.getInt("automatic") & ALL_PORTS;
        progress = Mth.clamp(tag.getInt("progress"), 0, PROCESS_TICKS);
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction face) {
        if (isRemoved()) return super.getCapability(capability, face);
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            if (face == null) return unsidedItems.cast();
            if (hasSide(Port.INPUT, face) || hasSide(Port.OUTPUT, face)) {
                return itemFaces.computeIfAbsent(face, side -> LazyOptional.of(() -> new ItemPort(side))).cast();
            }
        } else if (capability == ForgeCapabilities.FLUID_HANDLER) {
            if (face == null) return unsidedFluid.cast();
            if (hasSide(Port.XP, face)) {
                return fluidFaces.computeIfAbsent(face, side -> LazyOptional.of(() -> new FluidPort(side))).cast();
            }
        }
        return super.getCapability(capability, face);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        unsidedItems.invalidate();
        unsidedFluid.invalidate();
        itemFaces.values().forEach(LazyOptional::invalidate);
        fluidFaces.values().forEach(LazyOptional::invalidate);
        itemFaces.clear();
        fluidFaces.clear();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        unsidedItems = LazyOptional.of(() -> new ItemPort(null));
        unsidedFluid = LazyOptional.of(() -> new FluidPort(null));
    }

    private final class ItemPort implements IItemHandler {
        @Nullable private final Direction side;
        private final boolean input;
        private final boolean output;

        private ItemPort(@Nullable final Direction side) {
            this.side = side;
            input = hasSide(Port.INPUT, side);
            output = hasSide(Port.OUTPUT, side);
        }

        private int machineSlot(final int slot) {
            if (input && slot == 0 && hasSide(Port.INPUT, side)) return INPUT;
            if (output && slot == (input ? 1 : 0) && hasSide(Port.OUTPUT, side)) return OUTPUT;
            return -1;
        }

        @Override
        public int getSlots() {
            return (input ? 1 : 0) + (output ? 1 : 0);
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            final int mapped = machineSlot(slot);
            return mapped >= 0 ? bottles.getStackInSlot(mapped) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return isItemValid(slot, stack) ? bottles.insertItem(INPUT, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return machineSlot(slot) == OUTPUT ? bottles.extractItem(OUTPUT, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(final int slot) {
            final int mapped = machineSlot(slot);
            return mapped >= 0 ? bottles.getSlotLimit(mapped) : 0;
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return machineSlot(slot) == INPUT && bottles.isItemValid(INPUT, stack);
        }
    }

    private final class FluidPort implements IFluidHandler {
        @Nullable private final Direction side;

        private FluidPort(@Nullable final Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return hasSide(Port.XP, side) ? 1 : 0;
        }

        @Override
        public FluidStack getFluidInTank(final int index) {
            return index == 0 && getTanks() > 0 ? contents() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(final int index) {
            return index == 0 && getTanks() > 0 ? capacity() : 0;
        }

        @Override
        public boolean isFluidValid(final int index, final FluidStack fluid) {
            return index == 0 && getTanks() > 0 && tank.isFluidValid(fluid);
        }

        @Override
        public int fill(final FluidStack fluid, final FluidAction action) {
            return getTanks() > 0 ? tank.fill(fluid, action) : 0;
        }

        @Override
        public FluidStack drain(final FluidStack fluid, final FluidAction action) {
            return side == null && !isRemoved() ? tank.drain(fluid, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(final int amount, final FluidAction action) {
            return side == null && !isRemoved() && amount > 0 ? tank.drain(amount, action) : FluidStack.EMPTY;
        }
    }
}
