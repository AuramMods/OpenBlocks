package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBRedstoneMachineBlock;
import art.arcane.openblocks.block.OBRedstoneMachineBlock.Kind;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.menu.OBRedstoneMachineMenu;
import art.arcane.openblocks.world.OBMachineItems;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public final class OBRedstoneMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INVENTORY_SIZE = 9;
    private static final String ACTOR_NAME = "[OpenBlocks]";
    private static final GameProfile ACTOR = new GameProfile(UUID.nameUUIDFromBytes(ACTOR_NAME.getBytes(StandardCharsets.UTF_8)), ACTOR_NAME);
    private static final String INVENTORY_TAG = "inventory";
    private static final String SPEED_TAG = "speedTenths";
    private static final String SCALE_TAG = "scaleWithRedstone";

    private final Kind kind;
    private final ItemStackHandler items;
    private LazyOptional<IItemHandler> itemAccess;
    private long lastWorkTick = Long.MIN_VALUE;
    private int attempts;
    private boolean working;
    private int speedTenths;
    private boolean scaleWithRedstone;

    public OBRedstoneMachineBlockEntity(final Kind kind, final BlockPos pos, final BlockState state) {
        super(kind.type(), pos, state);
        this.kind = kind;
        items = new ItemStackHandler(kind == Kind.BREAKER ? 1 : INVENTORY_SIZE) {
            @Override
            public boolean isItemValid(final int slot, final ItemStack stack) {
                return kind != Kind.BREAKER;
            }

            @Override
            protected void onContentsChanged(final int slot) {
                inventoryChanged(slot);
            }
        };
        itemAccess = LazyOptional.of(() -> items);
    }

    public ItemStackHandler items() {
        return items;
    }

    public void inventoryChanged(final int slot) {
        setChanged();
        if (kind == Kind.PLACER && !working && !isRemoved() && level instanceof ServerLevel server
                && getBlockState().getValue(OBRedstoneMachineBlock.POWERED) && !items.getStackInSlot(slot).isEmpty()) {
            requestWork(server, getBlockState().getValue(OBRedstoneMachineBlock.FACING));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) scheduleWork(server);
    }

    private void scheduleWork(final ServerLevel server) {
        if (!isRemoved()) server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    public void requestWork(final ServerLevel server, final Direction facing) {
        final BlockPos target = worldPosition.relative(facing);
        if (server.hasChunkAt(target) && canWork(server, target, server.getBlockState(target), kind == Kind.BREAKER || firstOccupiedSlot() >= 0)) {
            server.blockEvent(worldPosition, getBlockState().getBlock(), OBRedstoneMachineBlock.WORK_EVENT, 0);
        }
    }

    private boolean canWork(final ServerLevel server, final BlockPos target, final BlockState state, final boolean stocked) {
        if (kind == Kind.BREAKER) return !state.isAir() && state.getDestroySpeed(server, target) >= 0.0F;
        return stocked && state.canBeReplaced();
    }

    public void manipulate(final ServerLevel server, final Direction facing) {
        final BlockPos target = worldPosition.relative(facing);
        if (!server.hasChunkAt(target)) return;
        final BlockState targetState = server.getBlockState(target);
        final int slot = kind == Kind.PLACER ? firstOccupiedSlot() : -1;
        if (!canWork(server, target, targetState, slot >= 0)) return;

        final int limit = (kind == Kind.BREAKER ? OBServerConfig.BLOCK_BREAKER_ACTION_LIMIT : OBServerConfig.BLOCK_PLACER_ACTION_LIMIT).get();
        if (limit == 0) return;
        if (lastWorkTick != server.getGameTime()) {
            lastWorkTick = server.getGameTime();
            attempts = 0;
        }
        if (attempts >= limit) {
            scheduleWork(server);
            return;
        }
        attempts++;

        final FakePlayer player = actor(server, target, facing);
        if (!server.mayInteract(player, target)) return;
        working = true;
        try {
            final boolean madeProgress = kind == Kind.BREAKER ? harvest(server, player, target, targetState, facing)
                    : place(server, player, target, targetState, facing, slot);
            if (madeProgress) scheduleWork(server);
        } finally {
            working = false;
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    private boolean harvest(final ServerLevel server, final FakePlayer player, final BlockPos target,
            final BlockState targetState, final Direction facing) {
        selectTool(server, player, target, targetState);
        final AABB bounds = new AABB(target);
        final var existing = new HashSet<>(server.getEntitiesOfClass(ItemEntity.class, bounds, item -> item.blockPosition().equals(target)));
        if (!player.gameMode.destroyBlock(target)) return false;

        final var harvested = server.getEntitiesOfClass(ItemEntity.class, bounds,
                item -> item.blockPosition().equals(target) && !existing.contains(item));

        final IItemHandler output = OBMachineItems.adjacent(server, worldPosition, facing.getOpposite());
        if (output != null) {
            for (final ItemEntity drop : harvested) {
                final ItemStack left = ItemHandlerHelper.insertItemStacked(output, drop.getItem(), false);
                if (left.isEmpty()) drop.discard();
                else drop.setItem(left);
            }
        }
        return !harvested.isEmpty() || server.getBlockState(target) != targetState;
    }

    private static void selectTool(final ServerLevel server, final FakePlayer player, final BlockPos target, final BlockState state) {
        for (final String name : OBServerConfig.BLOCK_BREAKER_TOOLS.get()) {
            final ResourceLocation id = ResourceLocation.tryParse(name);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(id)));
            if (state.canHarvestBlock(server, target, player)) return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
    }

    private boolean place(final ServerLevel server, final FakePlayer player, final BlockPos target,
            final BlockState targetState, final Direction facing, final int slot) {
        final ItemStack held = items.extractItem(slot, Integer.MAX_VALUE, false);
        final ItemStack original = held.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        try {
            final var hit = new BlockHitResult(Vec3.atCenterOf(target), facing.getOpposite(), target, false);
            player.gameMode.useItemOn(player, server, held, InteractionHand.MAIN_HAND, hit);
            return server.getBlockState(target) != targetState || !ItemStack.matches(original, player.getMainHandItem());
        } finally {
            restore(server, slot, player.getMainHandItem());
        }
    }

    public void eject(final ServerLevel server, final Direction facing, final int signal) {
        final int slot = firstOccupiedSlot();
        if (slot < 0 || !server.hasChunkAt(worldPosition.relative(facing))) return;
        final ItemStack reserved = items.extractItem(slot, 1, false);
        final double speed = speedTenths() / 10.0 * (scaleWithRedstone ? signal / 15.0 : 1.0);
        final Vec3 velocity = Vec3.atLowerCornerOf(facing.getNormal()).scale(speed);
        final Vec3 point = Vec3.atCenterOf(worldPosition).relative(facing, 1.0).add(0.0, -EntityType.ITEM.getHeight(), 0.0);
        final var item = new ItemEntity(server, point.x, point.y, point.z, reserved.copy());
        item.setPickUpDelay(40);
        item.setDeltaMovement(velocity);

        final var event = new ItemTossEvent(item, actor(server, worldPosition, facing.getOpposite()));
        boolean spawned = false;
        try {
            spawned = !MinecraftForge.EVENT_BUS.post(event) && server.addFreshEntity(event.getEntity());
        } finally {
            if (!spawned) restore(server, slot, reserved);
        }
    }

    private static FakePlayer actor(final ServerLevel server, final BlockPos target, final Direction facing) {
        final FakePlayer player = FakePlayerFactory.get(server, ACTOR);
        final Vec3 position = Vec3.atCenterOf(target).relative(facing, 2.0);
        final float pitch = facing == Direction.UP ? 90.0F : facing == Direction.DOWN ? -90.0F : 0.0F;
        final float yaw = facing.getOpposite().toYRot();
        player.moveTo(position.x, position.y, position.z, yaw, pitch);
        player.setYHeadRot(yaw);
        return player;
    }

    private int firstOccupiedSlot() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private void restore(final ServerLevel server, final int slot, final ItemStack stack) {
        if (stack.isEmpty()) return;
        if (isRemoved()) {
            Block.popResource(server, worldPosition, stack);
            return;
        }
        final ItemStack remaining = items.insertItem(slot, stack, false);
        Block.popResource(server, worldPosition, ItemHandlerHelper.insertItemStacked(items, remaining, false));
    }

    public void dropInventory() {
        if (level == null) return;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Block.popResource(level, worldPosition, items.extractItem(slot, Integer.MAX_VALUE, false));
        }
    }

    public static int maxSpeedTenths() {
        return (int) Math.floor(OBServerConfig.ITEM_DROPPER_MAX_SPEED.get() * 10.0);
    }

    public int speedTenths() {
        return Mth.clamp(speedTenths, 0, maxSpeedTenths());
    }

    public void setSpeedTenths(final int speed) {
        speedTenths = Mth.clamp(speed, 0, maxSpeedTenths());
        setChanged();
    }

    public boolean scalesWithRedstone() {
        return scaleWithRedstone;
    }

    public void toggleRedstoneScaling() {
        scaleWithRedstone = !scaleWithRedstone;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return kind == Kind.BREAKER ? null : new OBRedstoneMachineMenu(id, inventory, this, kind == Kind.DROPPER);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(INVENTORY_TAG, items.serializeNBT());
        if (kind == Kind.DROPPER) {
            tag.putInt(SPEED_TAG, speedTenths);
            tag.putBoolean(SCALE_TAG, scaleWithRedstone);
        }
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        if (tag.contains(INVENTORY_TAG, CompoundTag.TAG_COMPOUND)) {
            final CompoundTag stored = tag.getCompound(INVENTORY_TAG).copy();
            stored.putInt("Size", items.getSlots());
            items.deserializeNBT(stored);
        }
        speedTenths = Math.max(0, tag.getInt(SPEED_TAG));
        scaleWithRedstone = tag.getBoolean(SCALE_TAG);
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction side) {
        return !isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER ? itemAccess.cast() : super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemAccess.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemAccess = LazyOptional.of(() -> items);
    }
}
