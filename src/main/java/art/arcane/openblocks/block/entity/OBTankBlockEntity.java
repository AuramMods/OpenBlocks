package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBTankBlock;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public final class OBTankBlockEntity extends BlockEntity {

    public static final String FLUID_TAG = "tank";
    private static final int SYNC_INTERVAL = 8;
    private final FluidTank tank = new FluidTank(capacity()) {
        @Override
        protected void onContentsChanged() {
            fluidChanged();
        }
    };
    private final IFluidHandler column = new ColumnHandler();
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> column);
    private boolean needsBalance = true;
    private boolean needsSync = true;

    public OBTankBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.TANK.get(), pos, state);
    }

    public static int capacity() {
        final int buckets = OBServerConfig.SPEC.isLoaded() ? OBServerConfig.TANK_BUCKETS.get() : OBServerConfig.TANK_BUCKETS.getDefault();
        return buckets * FluidType.BUCKET_VOLUME;
    }

    public FluidStack contents() {
        return tank.getFluid().copy();
    }

    public int comparatorOutput() {
        final int amount = tank.getFluidAmount();
        return amount == 0 ? 0 : Math.max(1, (int) (15L * amount / Math.max(capacity(), amount)));
    }

    public void restoreContents(final FluidStack fluid) {
        tank.setFluid(fluid.copy());
        fluidChanged();
    }

    private void fluidChanged() {
        setChanged();
        needsSync = true;
        needsBalance = true;
        if (level == null || level.isClientSide) return;
        for (final Direction direction : Direction.values()) {
            final var neighbor = neighbor(direction);
            if (neighbor != null) neighbor.needsBalance = true;
        }
    }

    public void scheduleBalance() {
        needsBalance = true;
    }

    @Nullable
    public OBTankBlockEntity neighbor(final Direction direction) {
        final BlockPos pos = worldPosition.relative(direction);
        return level != null && level.isInWorldBounds(pos) && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof OBTankBlockEntity neighbor && !neighbor.isRemoved() ? neighbor : null;
    }

    private boolean accepts(final FluidStack fluid) {
        return tank.isEmpty() || tank.getFluid().isFluidEqual(fluid);
    }

    private int fillLocal(final FluidStack fluid, final IFluidHandler.FluidAction action) {
        // Config changes cannot truncate a previously filled tank.
        tank.setCapacity(Math.max(capacity(), tank.getFluidAmount()));
        return tank.fill(fluid, action);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBTankBlockEntity entity) {
        if (entity.needsBalance && OBServerConfig.TANK_BALANCE.get()) {
            entity.needsBalance = false;
            entity.balance();
        }
        if (entity.needsSync && Math.floorMod(level.getGameTime() + pos.asLong(), SYNC_INTERVAL) == 0) {
            entity.needsSync = false;
            final FluidStack fluid = entity.tank.getFluid();
            final int light = OBServerConfig.TANK_LIGHT.get() && !fluid.isEmpty()
                    ? fluid.getFluid().getFluidType().getLightLevel(fluid) : 0;
            final BlockState next = state.setValue(OBTankBlock.LIGHT, light);
            if (state != next) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            level.sendBlockUpdated(pos, next, next, Block.UPDATE_CLIENTS);
        }
    }

    private void balance() {
        if (tank.isEmpty()) return;
        final var below = neighbor(Direction.DOWN);
        if (below != null) {
            final int moved = below.fillLocal(tank.getFluid(), IFluidHandler.FluidAction.EXECUTE);
            if (moved > 0) tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
        }
        if (tank.isEmpty()) return;
        final int threshold = Math.max(1, OBServerConfig.TANK_BALANCE_THRESHOLD.get());
        for (final Direction direction : Direction.Plane.HORIZONTAL) {
            final var side = neighbor(direction);
            if (side == null || !side.accepts(tank.getFluid())) continue;
            final int toShare = (tank.getFluidAmount() - side.tank.getFluidAmount()) / 2;
            if (toShare < threshold) continue;
            final FluidStack transfer = new FluidStack(tank.getFluid(), toShare);
            final int moved = side.fillLocal(transfer, IFluidHandler.FluidAction.EXECUTE);
            if (moved > 0) tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        writeFluid(tag);
    }

    private CompoundTag writeFluid(final CompoundTag tag) {
        if (!tank.isEmpty()) tag.put(FLUID_TAG, tank.getFluid().writeToNBT(new CompoundTag()));
        return tag;
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        tank.setFluid(FluidStack.loadFluidStackFromNBT(tag.getCompound(FLUID_TAG)));
        needsBalance = true;
        needsSync = true;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return writeFluid(new CompoundTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction side) {
        if (!isRemoved() && capability == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidCapability = LazyOptional.of(() -> column);
    }

    private final class ColumnHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(final int index) {
            return index == 0 ? contents() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(final int index) {
            return index == 0 ? Math.max(capacity(), tank.getFluidAmount()) : 0;
        }

        @Override
        public boolean isFluidValid(final int index, final FluidStack fluid) {
            return index == 0;
        }

        @Override
        public int fill(final FluidStack fluid, final FluidAction action) {
            if (fluid.isEmpty()) return 0;
            int remaining = fluid.getAmount();
            for (OBTankBlockEntity part = OBTankBlockEntity.this; part != null && remaining > 0 && part.accepts(fluid);
                    part = part.neighbor(Direction.UP)) {
                remaining -= part.fillLocal(new FluidStack(fluid, remaining), action);
            }
            return fluid.getAmount() - remaining;
        }

        @Override
        public FluidStack drain(final FluidStack fluid, final FluidAction action) {
            if (fluid.isEmpty() || !tank.getFluid().isFluidEqual(fluid)) return FluidStack.EMPTY;
            return drain(fluid.getAmount(), action);
        }

        @Override
        public FluidStack drain(final int amount, final FluidAction action) {
            if (amount <= 0 || tank.isEmpty()) return FluidStack.EMPTY;
            final FluidStack fluid = contents();
            OBTankBlockEntity top = OBTankBlockEntity.this;
            OBTankBlockEntity next = top.neighbor(Direction.UP);
            while (next != null && next.tank.getFluid().isFluidEqual(fluid)) {
                top = next;
                next = top.neighbor(Direction.UP);
            }
            int remaining = amount;
            for (OBTankBlockEntity part = top; part != null && remaining > 0; part = part.neighbor(Direction.DOWN)) {
                remaining -= part.tank.drain(remaining, action).getAmount();
                if (part == OBTankBlockEntity.this) break;
            }
            return new FluidStack(fluid, amount - remaining);
        }
    }
}
