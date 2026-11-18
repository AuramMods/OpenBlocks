package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBVacuumHopperBlock;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.entity.OBItemProjectile;
import art.arcane.openblocks.menu.OBVacuumHopperMenu;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBFluids;
import art.arcane.openblocks.world.OBLiquidExperience;
import art.arcane.openblocks.world.OBMachineItems;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public final class OBVacuumHopperBlockEntity extends BlockEntity implements MenuProvider {

    public enum Output { ITEMS, XP }

    public static final int SLOTS = 10;
    private static final int FIVE_LEVELS_XP = 55;
    private static final double PICKUP_DISTANCE_SQR = 1.1 * 1.1;
    private static final int OUTPUT_PERIOD = 10;
    private static final int FLUID_PER_TRANSFER = 50;
    private static final Direction[] SIDES = Direction.values();
    private static final int ALL_SIDES = (1 << SIDES.length) - 1;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(final int slot) {
            setChanged();
        }
    };
    private final FluidTank tank = new FluidTank(capacityForConfig(), this::isLiquidXp) {
        @Override
        protected void onContentsChanged() {
            setCapacity(Math.max(capacityForConfig(), getFluidAmount()));
            setChanged();
        }
    };
    private final Vec3 center;
    private final AABB suctionArea;
    private int itemOutputs;
    private int xpOutputs;

    private final Map<Direction, LazyOptional<IItemHandler>> itemFaces = new EnumMap<>(Direction.class);
    private final Map<Direction, LazyOptional<IFluidHandler>> fluidFaces = new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> unsidedItems = LazyOptional.of(() -> new ItemOutput(null));
    private LazyOptional<IFluidHandler> unsidedFluid = LazyOptional.of(() -> new FluidOutput(null));

    public OBVacuumHopperBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.VACUUM_HOPPER.get(), pos, state);
        center = Vec3.atCenterOf(pos);
        suctionArea = new AABB(pos).inflate(3.0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        tank.setCapacity(Math.max(capacityForConfig(), tank.getFluidAmount()));
    }

    private static int capacityForConfig() {
        final int ratio = OBServerConfig.SPEC.isLoaded() ? OBServerConfig.XP_FLUID_RATIO.get() : OBServerConfig.XP_FLUID_RATIO.getDefault();
        return OBLiquidExperience.millibuckets(FIVE_LEVELS_XP, ratio);
    }

    private boolean isLiquidXp(final FluidStack fluid) {
        return !fluid.hasTag() && fluid.getFluid() == OBFluids.XP_JUICE.get();
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidStack contents() {
        return tank.getFluid().copy();
    }

    public int capacity() {
        return tank.getCapacity();
    }

    public int outputMask(final Output output) {
        return output == Output.ITEMS ? itemOutputs : xpOutputs;
    }

    private boolean allows(final Output output, @Nullable final Direction face) {
        return !isRemoved() && (face == null || (outputMask(output) & (1 << face.ordinal())) != 0);
    }

    public void toggleOutput(final Output output, final Direction side) {
        final int mask = outputMask(output) ^ (1 << side.ordinal());
        setOutputs(output == Output.ITEMS ? mask : itemOutputs, output == Output.XP ? mask : xpOutputs);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void setOutputs(final int itemMask, final int xpMask) {
        final int changedItems = itemOutputs ^ itemMask;
        final int changedXp = xpOutputs ^ xpMask;
        itemOutputs = itemMask;
        xpOutputs = xpMask;
        for (final Direction side : SIDES) {
            final int bit = 1 << side.ordinal();
            if ((changedItems & bit) != 0) {
                final var previous = itemFaces.remove(side);
                if (previous != null) previous.invalidate();
            }
            if ((changedXp & bit) != 0) {
                final var previous = fluidFaces.remove(side);
                if (previous != null) previous.invalidate();
            }
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBVacuumHopperBlockEntity hopper) {
        if (!state.getValue(OBVacuumHopperBlock.ENABLED)) return;
        final int xpRatio = OBServerConfig.XP_FLUID_RATIO.get();
        for (final ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, hopper.suctionArea, Entity::isAlive)) {
            if (item instanceof OBItemProjectile projectile && !projectile.canBeVacuumed()) continue;
            if (item.distanceToSqr(hopper.center) < PICKUP_DISTANCE_SQR) hopper.collectItem(item);
            else if (ItemHandlerHelper.insertItemStacked(hopper.items, item.getItem(), true).getCount() < item.getItem().getCount()) hopper.attract(item);
        }
        if (hopper.tank.getSpace() >= xpRatio) {
            for (final ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, hopper.suctionArea, Entity::isAlive)) {
                if (orb.distanceToSqr(hopper.center) < PICKUP_DISTANCE_SQR) OBLiquidExperience.collectOrb(hopper.tank, orb, xpRatio);
                else if ((long) orb.getValue() * xpRatio <= hopper.tank.getSpace()) hopper.attract(orb);
            }
        }
        if (level.getGameTime() % OUTPUT_PERIOD == 0) {
            hopper.pushFluid(level, pos);
            hopper.pushItems(level, pos);
        }
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final OBVacuumHopperBlockEntity hopper) {
        if (!state.getValue(OBVacuumHopperBlock.ENABLED)) return;
        level.addParticle(ParticleTypes.PORTAL, hopper.center.x, hopper.center.y, hopper.center.z,
                level.random.nextDouble() - 0.5, level.random.nextDouble() - 1.0, level.random.nextDouble() - 0.5);
    }

    public void collectContact(final Entity entity) {
        if (entity instanceof ItemEntity item) collectItem(item);
        else if (entity instanceof ExperienceOrb orb) OBLiquidExperience.collectOrb(tank, orb, OBServerConfig.XP_FLUID_RATIO.get());
    }

    private void collectItem(final ItemEntity item) {
        if (!item.isAlive()) return;
        final ItemStack stack = item.getItem();
        final ItemStack remainder = ItemHandlerHelper.insertItemStacked(items, stack.copy(), false);
        if (remainder.isEmpty()) item.discard();
        else if (remainder.getCount() < stack.getCount()) item.setItem(remainder);
    }

    private void attract(final Entity entity) {
        final Vec3 offset = center.subtract(entity.position());
        final double distance = offset.length();
        final double force = 1.0 - distance / 15.0;
        final Vec3 direction = offset.scale(force * force / distance);
        entity.setDeltaMovement(entity.getDeltaMovement().add(direction.multiply(0.05, 0.2, 0.05)));
        entity.hasImpulse = true;
    }

    private void pushFluid(final Level level, final BlockPos pos) {
        if (tank.isEmpty() || xpOutputs == 0) return;
        int remaining = FLUID_PER_TRANSFER;
        final int first = level.random.nextInt(SIDES.length);
        for (int index = 0; index < SIDES.length && remaining > 0 && !tank.isEmpty(); index++) {
            final Direction face = SIDES[(index + first) % SIDES.length];
            final BlockPos adjacent = pos.relative(face);
            if (!allows(Output.XP, face) || !level.hasChunkAt(adjacent)) continue;
            final var receiver = FluidUtil.getFluidHandler(level, adjacent, face.getOpposite()).orElse(null);
            if (receiver != null) remaining -= FluidUtil.tryFluidTransfer(receiver, tank, remaining, true).getAmount();
        }
    }

    private void pushItems(final Level level, final BlockPos pos) {
        final Direction face = OBMachineItems.chooseFace(itemOutputs, level.random);
        if (face == null) return;
        final IItemHandler destination = OBMachineItems.adjacent(level, pos, face);
        if (destination == null) return;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (OBMachineItems.push(level, pos, items, slot, destination, items.getSlotLimit(slot))) break;
        }
    }

    public void dropContents(final Level level, final BlockPos pos) {
        for (int slot = 0; slot < SLOTS; slot++) Block.popResource(level, pos, items.extractItem(slot, items.getSlotLimit(slot), false));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new OBVacuumHopperMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", items.serializeNBT());
        if (!tank.isEmpty()) tag.put("tank", tank.writeToNBT(new CompoundTag()));
        writeOutputs(tag);
    }

    private void writeOutputs(final CompoundTag tag) {
        tag.putInt("itemOutputs", itemOutputs);
        tag.putInt("xpOutputs", xpOutputs);
    }

    private void readOutputs(final CompoundTag tag) {
        setOutputs(tag.getInt("itemOutputs") & ALL_SIDES, tag.getInt("xpOutputs") & ALL_SIDES);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        final CompoundTag inventory = tag.getCompound("inventory").copy();
        inventory.putInt("Size", SLOTS);
        items.deserializeNBT(inventory);
        tank.readFromNBT(tag.getCompound("tank"));
        tank.setCapacity(Math.max(capacityForConfig(), tank.getFluidAmount()));
        readOutputs(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        final var tag = new CompoundTag();
        writeOutputs(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        readOutputs(tag);
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void onDataPacket(final Connection connection, final ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction face) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && allows(Output.ITEMS, face)) {
            return face == null ? unsidedItems.cast() : itemFaces.computeIfAbsent(face, side -> LazyOptional.of(() -> new ItemOutput(side))).cast();
        }
        if (capability == ForgeCapabilities.FLUID_HANDLER && allows(Output.XP, face)) {
            return face == null ? unsidedFluid.cast() : fluidFaces.computeIfAbsent(face, side -> LazyOptional.of(() -> new FluidOutput(side))).cast();
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
        unsidedItems = LazyOptional.of(() -> new ItemOutput(null));
        unsidedFluid = LazyOptional.of(() -> new FluidOutput(null));
    }

    private final class ItemOutput implements IItemHandler {
        @Nullable private final Direction face;

        private ItemOutput(@Nullable final Direction face) {
            this.face = face;
        }

        @Override
        public int getSlots() {
            return allows(Output.ITEMS, face) ? SLOTS : 0;
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return allows(Output.ITEMS, face) ? items.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return allows(Output.ITEMS, face) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(final int slot) {
            return allows(Output.ITEMS, face) ? items.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return false;
        }
    }

    private final class FluidOutput implements IFluidHandler {
        @Nullable private final Direction face;

        private FluidOutput(@Nullable final Direction face) {
            this.face = face;
        }

        @Override
        public int getTanks() {
            return allows(Output.XP, face) ? 1 : 0;
        }

        @Override
        public FluidStack getFluidInTank(final int index) {
            return index == 0 && allows(Output.XP, face) ? contents() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(final int index) {
            return index == 0 && allows(Output.XP, face) ? capacity() : 0;
        }

        @Override
        public boolean isFluidValid(final int index, final FluidStack fluid) {
            return index == 0 && face == null && !isRemoved() && tank.isFluidValid(fluid);
        }

        @Override
        public int fill(final FluidStack fluid, final FluidAction action) {
            return isFluidValid(0, fluid) ? tank.fill(fluid, action) : 0;
        }

        @Override
        public FluidStack drain(final FluidStack fluid, final FluidAction action) {
            return allows(Output.XP, face) ? tank.drain(fluid, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(final int amount, final FluidAction action) {
            return amount > 0 && allows(Output.XP, face) ? tank.drain(amount, action) : FluidStack.EMPTY;
        }
    }
}
