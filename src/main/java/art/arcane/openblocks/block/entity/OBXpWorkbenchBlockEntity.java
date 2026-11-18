package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.menu.OBXpWorkbenchMenu;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.world.OBLiquidExperience;
import art.arcane.openblocks.world.OBMachineItems;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class OBXpWorkbenchBlockEntity extends BlockEntity implements MenuProvider {

    public enum Port { TOOL, REAGENT, OUTPUT, XP }

    public static final int SLOT_COUNT = 3;
    public static final int MAXIMUM_POWER = 30;
    private static final Direction[] FACES = Direction.values();
    private static final Port[] PORTS = Port.values();
    private static final int FACE_MASK = (1 << FACES.length) - 1;
    private static final int AUTOMATIC_FLAG = 1 << FACES.length;
    private static final int ANVIL_COOLDOWN = 40;
    private static final GameProfile ANVIL_ACTOR = new GameProfile(
            UUID.nameUUIDFromBytes("[OpenBlocks Anvil]".getBytes(StandardCharsets.UTF_8)), "[OpenBlocks Anvil]");

    private final boolean enchanting;
    private final int storagePoints;
    private int nominalCapacity;
    private final FluidTank tank;
    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return accepts(enchanting, slot, stack);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return enchanting && slot == Port.TOOL.ordinal() ? 1 : super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            setChanged();
        }
    };

    private final int[] settings = new int[PORTS.length];
    private final EnumMap<Direction, LazyOptional<IItemHandler>> itemFaces = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IFluidHandler>> xpFaces = new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> unsidedItems = LazyOptional.of(() -> new ItemPort(null));
    private LazyOptional<IFluidHandler> unsidedXp = LazyOptional.of(() -> new XpPort(null));
    private int cooldown;
    private int powerCountdown;
    private int availablePower;
    private int powerLimit = 1;
    private int tier = 1;
    private long seed = RandomSource.create().nextLong();
    @Nullable private EnchantmentTableBlockEntity book;

    public OBXpWorkbenchBlockEntity(final boolean enchanting, final BlockPos pos, final BlockState state) {
        super((enchanting ? OBBlockEntities.AUTO_ENCHANTMENT_TABLE : OBBlockEntities.AUTO_ANVIL).get(), pos, state);
        this.enchanting = enchanting;
        storagePoints = OBLiquidExperience.pointsAtLevel(enchanting ? 30 : 45);
        nominalCapacity = OBLiquidExperience.capacityForPoints(storagePoints);
        tank = new FluidTank(nominalCapacity, fluid -> OBLiquidExperience.ratio(fluid) > 0) {
            @Override
            protected void onContentsChanged() {
                setCapacity(Math.max(nominalCapacity, getFluidAmount()));
                setChanged();
            }
        };
    }

    public boolean enchanting() { return enchanting; }
    public ItemStackHandler items() { return items; }
    public FluidStack contents() { return tank.getFluid().copy(); }
    public int capacity() { return tank.getCapacity(); }
    public int settings(final Port port) { return settings[port.ordinal()]; }
    public int tier() { return tier; }
    public int powerLimit() { return powerLimit; }
    public int availablePower() { return availablePower; }

    public static boolean accepts(final boolean enchanting, final int slot, final ItemStack stack) {
        if (slot == Port.OUTPUT.ordinal()) return false;
        if (!enchanting) return true;
        return slot == Port.TOOL.ordinal() ? stack.copyWithCount(1).isEnchantable() : stack.is(Tags.Items.ENCHANTING_FUELS);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        nominalCapacity = OBLiquidExperience.capacityForPoints(storagePoints);
        tank.setCapacity(Math.max(nominalCapacity, tank.getFluidAmount()));
    }

    private boolean hasSide(final Port port, @Nullable final Direction face) {
        return !isRemoved() && (face == null || (settings(port) & 1 << face.ordinal()) != 0);
    }

    public void toggleSide(final Port port, final Direction face) {
        settings[port.ordinal()] ^= 1 << face.ordinal();
        final var previous = port == Port.XP ? xpFaces.remove(face) : itemFaces.remove(face);
        if (previous != null) previous.invalidate();
        setChanged();
    }

    public void toggleAutomatic(final Port port) {
        settings[port.ordinal()] ^= AUTOMATIC_FLAG;
        setChanged();
    }

    public void selectTier(final int value) {
        tier = Mth.clamp(value, 1, 3);
        setChanged();
    }

    public void setPowerLimit(final int value) {
        powerLimit = Mth.clamp(value, 1, MAXIMUM_POWER);
        setChanged();
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBXpWorkbenchBlockEntity machine) {
        if (!(level instanceof ServerLevel server)) return;
        machine.transfer(server, pos);
        if (machine.enchanting) {
            if (machine.powerCountdown-- <= 0) {
                machine.powerCountdown = 20;
                machine.availablePower = bookshelfPower(level, pos);
            }
            machine.enchant(server, pos);
        } else if (machine.cooldown-- <= -1) {
            machine.cooldown = -1;
            if (machine.items.getStackInSlot(Port.OUTPUT.ordinal()).isEmpty()) {
                machine.cooldown = ANVIL_COOLDOWN;
                machine.repair(server, pos);
            }
        }
    }

    private static int bookshelfPower(final Level level, final BlockPos pos) {
        float total = 0;
        for (final BlockPos offset : EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantmentTableBlock.isValidBookShelf(level, pos, offset)) {
                final BlockPos shelf = pos.offset(offset);
                total += level.getBlockState(shelf).getEnchantPowerBonus(level, shelf);
            }
        }
        return Mth.clamp((int) total, 0, Short.MAX_VALUE);
    }

    private void transfer(final Level level, final BlockPos pos) {
        if ((settings(Port.XP) & AUTOMATIC_FLAG) != 0 && tank.getSpace() > 0) {
            final int first = level.random.nextInt(FACES.length);
            int remaining = enchanting ? 80 : 100;
            for (int index = 0; index < FACES.length && remaining > 0 && tank.getSpace() > 0; index++) {
                final Direction face = FACES[(first + index) % FACES.length];
                if (hasSide(Port.XP, face)) remaining -= OBLiquidExperience.pull(level, pos.relative(face), face.getOpposite(), tank, remaining);
            }
        }
        moveItem(level, pos, Port.OUTPUT);
        moveItem(level, pos, Port.TOOL);
        moveItem(level, pos, Port.REAGENT);
    }

    private void moveItem(final Level level, final BlockPos pos, final Port port) {
        if ((settings(port) & AUTOMATIC_FLAG) == 0) return;
        final int slot = port.ordinal();
        final ItemStack current = items.getStackInSlot(slot);
        if (port == Port.OUTPUT ? current.isEmpty() : current.getCount() >= Math.min(items.getSlotLimit(slot), current.getMaxStackSize())) return;
        if (port == Port.TOOL && !current.isEmpty()) return;
        final Direction face = OBMachineItems.chooseFace(settings(port) & FACE_MASK, level.random);
        if (face == null) return;
        final IItemHandler adjacent = OBMachineItems.adjacent(level, pos, face);
        if (adjacent == null) return;
        if (port == Port.OUTPUT) OBMachineItems.push(level, pos, items, slot, adjacent, 1);
        else OBMachineItems.pullOne(level, pos, adjacent, items, slot);
    }

    private void repair(final ServerLevel level, final BlockPos pos) {
        final ItemStack tool = items.getStackInSlot(Port.TOOL.ordinal()).copy();
        if (tool.isEmpty() || OBLiquidExperience.ratio(tank.getFluid()) == 0) return;
        final ItemStack modifier = items.getStackInSlot(Port.REAGENT.ordinal()).copy();
        final var worker = FakePlayerFactory.get(level, ANVIL_ACTOR);
        worker.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        final var anvil = new AnvilCalculation(worker.getInventory());
        final ItemStack result = anvil.calculate(tool.copy(), modifier.copy());
        final int material = anvil.repairItemCountCost;
        if (result.isEmpty() || anvil.getCost() <= 0 || material < 0 || material > modifier.getCount()) return;
        if (!inputsUnchanged(tool, modifier) || !pay(OBLiquidExperience.pointsAtLevel(anvil.getCost()))) return;
        finish(result, material == 0 ? modifier.getCount() : material);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.3F, 1.0F);
    }

    private void enchant(final ServerLevel level, final BlockPos pos) {
        final int power = Math.min(availablePower, powerLimit);
        final int selectedTier = tier;
        if (power <= 0 || !items.getStackInSlot(Port.OUTPUT.ordinal()).isEmpty()) return;
        final ItemStack tool = items.getStackInSlot(Port.TOOL.ordinal()).copy();
        final ItemStack lapis = items.getStackInSlot(Port.REAGENT.ordinal()).copy();
        if (!tool.isEnchantable() || !lapis.is(Tags.Items.ENCHANTING_FUELS) || lapis.getCount() < selectedTier) return;
        if (OBLiquidExperience.ratio(tank.getFluid()) == 0) return;

        final RandomSource random = RandomSource.create(seed);
        final int index = selectedTier - 1;
        final int offer = EnchantmentHelper.getEnchantmentCost(random, index, power, tool);
        final int requirement = ForgeEventFactory.onEnchantmentLevelSet(level, pos, index, power, tool.copy(), offer <= selectedTier ? 0 : offer);
        if (requirement < selectedTier || !inputsUnchanged(tool, lapis)) return;
        final int requiredPoints = OBLiquidExperience.pointsAtLevel(requirement);
        final int ratio = OBLiquidExperience.ratio(tank.getFluid());
        if (ratio == 0 || tank.getFluidAmount() / ratio < requiredPoints) return;

        random.setSeed(seed + index);
        final var enchantments = EnchantmentHelper.selectEnchantment(random, tool, requirement, false);
        if (enchantments.isEmpty()) return;
        final boolean isBook = tool.is(Items.BOOK);
        final ItemStack result = isBook ? new ItemStack(Items.ENCHANTED_BOOK) : tool.copy();
        if (isBook) {
            if (tool.hasTag()) result.setTag(tool.getTag().copy());
            if (enchantments.size() > 1) enchantments.remove(random.nextInt(enchantments.size()));
        }
        for (final var enchantment : enchantments) {
            if (isBook) EnchantedBookItem.addEnchantment(result, enchantment);
            else result.enchant(enchantment.enchantment, enchantment.level);
        }
        if (!pay(requiredPoints - OBLiquidExperience.pointsAtLevel(requirement - selectedTier))) return;
        finish(result, selectedTier);
        seed = level.random.nextLong();
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.95F);
    }

    private boolean inputsUnchanged(final ItemStack tool, final ItemStack reagent) {
        return !isRemoved() && items.getStackInSlot(Port.OUTPUT.ordinal()).isEmpty()
                && ItemStack.matches(tool, items.getStackInSlot(Port.TOOL.ordinal()))
                && ItemStack.matches(reagent, items.getStackInSlot(Port.REAGENT.ordinal()));
    }

    private boolean pay(final int points) {
        final long bill = (long) points * OBLiquidExperience.ratio(tank.getFluid());
        if (bill <= 0 || bill > tank.getFluidAmount()) return false;
        tank.drain((int) bill, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private void finish(final ItemStack output, final int reagentCost) {
        final int reagent = Port.REAGENT.ordinal();
        final ItemStack stack = items.getStackInSlot(reagent);
        items.setStackInSlot(reagent, stack.copyWithCount(stack.getCount() - reagentCost));
        items.setStackInSlot(Port.TOOL.ordinal(), ItemStack.EMPTY);
        items.setStackInSlot(Port.OUTPUT.ordinal(), output);
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final OBXpWorkbenchBlockEntity machine) {
        if (!machine.enchanting) return;
        if (machine.book == null) machine.book = new EnchantmentTableBlockEntity(pos, Blocks.ENCHANTING_TABLE.defaultBlockState());
        EnchantmentTableBlockEntity.bookAnimationTick(level, pos, state, machine.book);
    }

    @Nullable
    public EnchantmentTableBlockEntity animatedBook() { return book; }

    public void dropItems(final Level level, final BlockPos pos) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            final ItemStack stack = items.getStackInSlot(slot);
            items.setStackInSlot(slot, ItemStack.EMPTY);
            Block.popResource(level, pos, stack);
        }
    }

    @Override
    public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public OBXpWorkbenchMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new OBXpWorkbenchMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", items.serializeNBT());
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.putIntArray("ports", settings);
        if (enchanting) {
            tag.putLong("seed", seed);
            tag.putInt("tier", tier);
            tag.putInt("powerLimit", powerLimit);
        } else tag.putInt("cooldown", cooldown);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        final CompoundTag inventory = tag.getCompound("inventory").copy();
        inventory.putInt("Size", SLOT_COUNT);
        items.deserializeNBT(inventory);
        tank.readFromNBT(tag.getCompound("tank"));
        tank.setCapacity(Math.max(nominalCapacity, tank.getFluidAmount()));
        final int[] ports = tag.getIntArray("ports");
        for (final Port port : PORTS) {
            final int index = port.ordinal();
            settings[index] = index < ports.length ? ports[index] & (FACE_MASK | AUTOMATIC_FLAG) : 0;
        }
        if (tag.contains("seed")) seed = tag.getLong("seed");
        tier = Mth.clamp(tag.getInt("tier"), 1, 3);
        powerLimit = Mth.clamp(tag.getInt("powerLimit"), 1, MAXIMUM_POWER);
        cooldown = Mth.clamp(tag.getInt("cooldown"), -1, ANVIL_COOLDOWN);
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction face) {
        if (!isRemoved() && capability == ForgeCapabilities.ITEM_HANDLER) {
            if (face == null) return unsidedItems.cast();
            if (hasSide(Port.TOOL, face) || hasSide(Port.REAGENT, face) || hasSide(Port.OUTPUT, face)) {
                return itemFaces.computeIfAbsent(face, side -> LazyOptional.of(() -> new ItemPort(side))).cast();
            }
        }
        if (capability == ForgeCapabilities.FLUID_HANDLER && hasSide(Port.XP, face)) {
            return face == null ? unsidedXp.cast() : xpFaces.computeIfAbsent(face, side -> LazyOptional.of(() -> new XpPort(side))).cast();
        }
        return super.getCapability(capability, face);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        unsidedItems.invalidate();
        unsidedXp.invalidate();
        itemFaces.values().forEach(LazyOptional::invalidate);
        xpFaces.values().forEach(LazyOptional::invalidate);
        itemFaces.clear();
        xpFaces.clear();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        unsidedItems = LazyOptional.of(() -> new ItemPort(null));
        unsidedXp = LazyOptional.of(() -> new XpPort(null));
    }

    private final class ItemPort implements IItemHandler {
        @Nullable private final Direction face;
        private final Port[] slots;

        private ItemPort(@Nullable final Direction face) {
            this.face = face;
            final var visible = new java.util.ArrayList<Port>(SLOT_COUNT);
            for (int slot = 0; slot < SLOT_COUNT; slot++) if (hasSide(PORTS[slot], face)) visible.add(PORTS[slot]);
            slots = visible.toArray(Port[]::new);
        }

        private int mapped(final int slot) {
            return slot >= 0 && slot < slots.length && hasSide(slots[slot], face) ? slots[slot].ordinal() : -1;
        }

        @Override public int getSlots() { return slots.length; }
        @Override public ItemStack getStackInSlot(final int slot) { return mapped(slot) < 0 ? ItemStack.EMPTY : items.getStackInSlot(mapped(slot)); }
        @Override public int getSlotLimit(final int slot) { return mapped(slot) < 0 ? 0 : items.getSlotLimit(mapped(slot)); }
        @Override public boolean isItemValid(final int slot, final ItemStack stack) { return mapped(slot) >= 0 && items.isItemValid(mapped(slot), stack); }
        @Override public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return isItemValid(slot, stack) ? items.insertItem(mapped(slot), stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return mapped(slot) == Port.OUTPUT.ordinal() ? items.extractItem(Port.OUTPUT.ordinal(), amount, simulate) : ItemStack.EMPTY;
        }
    }

    private final class XpPort implements IFluidHandler {
        @Nullable private final Direction face;

        private XpPort(@Nullable final Direction face) { this.face = face; }
        @Override public int getTanks() { return hasSide(Port.XP, face) ? 1 : 0; }
        @Override public FluidStack getFluidInTank(final int slot) { return slot == 0 && getTanks() > 0 ? contents() : FluidStack.EMPTY; }
        @Override public int getTankCapacity(final int slot) { return slot == 0 && getTanks() > 0 ? capacity() : 0; }
        @Override public boolean isFluidValid(final int slot, final FluidStack fluid) { return slot == 0 && getTanks() > 0 && tank.isFluidValid(fluid); }
        @Override public int fill(final FluidStack fluid, final FluidAction action) { return getTanks() > 0 ? tank.fill(fluid, action) : 0; }
        @Override public FluidStack drain(final FluidStack fluid, final FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(final int amount, final FluidAction action) { return FluidStack.EMPTY; }
    }

    private static final class AnvilCalculation extends AnvilMenu {
        private AnvilCalculation(final Inventory inventory) { super(0, inventory); }

        private ItemStack calculate(final ItemStack tool, final ItemStack modifier) {
            inputSlots.setItem(INPUT_SLOT, tool);
            inputSlots.setItem(ADDITIONAL_SLOT, modifier);
            createResult();
            return getSlot(RESULT_SLOT).getItem();
        }

        @Override public void slotsChanged(final Container slots) { }
    }
}
