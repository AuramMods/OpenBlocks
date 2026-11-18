package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBSprinklerBlock;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.menu.OBUtilityInventoryMenu;
import art.arcane.openblocks.registry.OBBlockEntities;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class OBSprinklerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOTS = 9;
    private static final String ACTOR_NAME = "[OpenBlocks Sprinkler]";
    private static final GameProfile ACTOR = new GameProfile(UUID.nameUUIDFromBytes(ACTOR_NAME.getBytes(StandardCharsets.UTF_8)), ACTOR_NAME);

    private final ItemStackHandler fertilizer = new ItemStackHandler(SLOTS) {
        @Override public boolean isItemValid(final int slot, final ItemStack stack) { return stack.is(Items.BONE_MEAL); }
        @Override protected void onContentsChanged(final int slot) { setChanged(); }
    };
    private final WaterTank water = new WaterTank();
    private LazyOptional<IItemHandler> fertilizerAccess = LazyOptional.of(() -> fertilizer);
    private LazyOptional<IFluidHandler> waterAccess = LazyOptional.of(() -> water);
    private int waterTicks;
    private int fertilizerTicks;
    private boolean boosted;

    public OBSprinklerBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.SPRINKLER.get(), pos, state);
    }

    public ItemStackHandler fertilizer() { return fertilizer; }
    public FluidStack contents() { return water.getFluid().copy(); }
    public int capacity() { return water.getCapacity(); }
    public boolean isActive() { return getBlockState().getValue(OBSprinklerBlock.ACTIVE); }

    private static int configuredCapacity() {
        return OBServerConfig.SPEC.isLoaded() ? OBServerConfig.SPRINKLER_CAPACITY.get() : OBServerConfig.SPRINKLER_CAPACITY.getDefault();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        water.setCapacity(Math.max(configuredCapacity(), water.getFluidAmount()));
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBSprinklerBlockEntity sprinkler) {
        if (level instanceof ServerLevel server) sprinkler.tick(server, pos);
    }

    private void tick(final ServerLevel level, final BlockPos pos) {
        if (water.isEmpty()) {
            final BlockPos below = pos.below();
            if (level.hasChunkAt(below)) {
                final var supply = FluidUtil.getFluidHandler(level, below, Direction.UP).orElse(null);
                if (supply != null) FluidUtil.tryFluidTransfer(water, supply, new FluidStack(Fluids.WATER, water.getSpace()), true);
            }
        }
        if (isRemoved()) return;
        if (waterTicks == 0 && water.spend()) waterTicks = OBServerConfig.SPRINKLER_WATER_RATE.get();
        final boolean active = waterTicks > 0;
        if (isActive() != active) {
            level.setBlock(pos, getBlockState().setValue(OBSprinklerBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
            setChanged();
        }
        if (!active) return;

        if (fertilizerTicks == 0) {
            boosted = consumeBoneMeal();
            fertilizerTicks = OBServerConfig.SPRINKLER_BONEMEAL_RATE.get();
        }
        final int chance = (boosted ? OBServerConfig.SPRINKLER_BONEMEAL_CHANCE : OBServerConfig.SPRINKLER_FERTILIZE_CHANCE).get();
        if (level.random.nextInt(chance) == 0) fertilize(level, pos);
        waterTicks--;
        fertilizerTicks--;
    }

    private boolean consumeBoneMeal() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (fertilizer.getStackInSlot(slot).is(Items.BONE_MEAL)) {
                fertilizer.extractItem(slot, 1, false);
                return true;
            }
        }
        return false;
    }

    private static void fertilize(final ServerLevel level, final BlockPos pos) {
        final int range = OBServerConfig.SPRINKLER_RANGE.get();
        final int x = level.random.nextInt(2 * range + 1) - range;
        final int z = level.random.nextInt(2 * range + 1) - range;
        final BlockPos base = pos.offset(x, -1, z);
        if (!level.hasChunkAt(base)) return;
        final var actor = FakePlayerFactory.get(level, ACTOR);
        actor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        final var dose = new ItemStack(Items.BONE_MEAL);
        for (int y = 0; y < 3; y++) {
            final BlockPos target = base.above(y);
            if (level.mayInteract(actor, target) && BoneMealItem.applyBonemeal(dose, level, target, actor)) break;
        }
    }

    public void dropFertilizer(final Level level, final BlockPos pos) {
        for (int slot = 0; slot < SLOTS; slot++) Block.popResource(level, pos, fertilizer.extractItem(slot, fertilizer.getSlotLimit(slot), false));
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public OBUtilityInventoryMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new OBUtilityInventoryMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", fertilizer.serializeNBT());
        tag.put("water", water.writeToNBT(new CompoundTag()));
        tag.putInt("waterTicks", waterTicks);
        tag.putInt("fertilizerTicks", fertilizerTicks);
        tag.putBoolean("boosted", boosted);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        final var stored = tag.getCompound("inventory").copy();
        stored.putInt("Size", SLOTS);
        fertilizer.deserializeNBT(stored);
        water.readFromNBT(tag.getCompound("water"));
        water.setCapacity(Math.max(configuredCapacity(), water.getFluidAmount()));
        waterTicks = Math.max(0, tag.getInt("waterTicks"));
        fertilizerTicks = Math.max(0, tag.getInt("fertilizerTicks"));
        boosted = tag.getBoolean("boosted");
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction face) {
        if (!isRemoved()) {
            if (cap == ForgeCapabilities.FLUID_HANDLER) return waterAccess.cast();
            if (cap == ForgeCapabilities.ITEM_HANDLER) return fertilizerAccess.cast();
        }
        return super.getCapability(cap, face);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fertilizerAccess.invalidate();
        waterAccess.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fertilizerAccess = LazyOptional.of(() -> fertilizer);
        waterAccess = LazyOptional.of(() -> water);
    }

    public static float sprayAngle(final Level level, final float partialTick) {
        return 25.0F * Mth.sin((level.getGameTime() % 314159 + partialTick) * 0.02F);
    }

    private final class WaterTank extends FluidTank {
        private WaterTank() { super(configuredCapacity(), stack -> stack.getFluid().isSame(Fluids.WATER) && !stack.hasTag()); }
        private boolean spend() { return !super.drain(1, FluidAction.EXECUTE).isEmpty(); }
        @Override protected void onContentsChanged() { setCapacity(Math.max(configuredCapacity(), getFluidAmount())); setChanged(); }
        @Override public FluidStack drain(final FluidStack stack, final FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(final int amount, final FluidAction action) { return FluidStack.EMPTY; }
    }
}
