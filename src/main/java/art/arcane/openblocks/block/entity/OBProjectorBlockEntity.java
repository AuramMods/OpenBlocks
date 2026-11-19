package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBProjectorBlock;
import art.arcane.openblocks.item.OBEmptyMapItem;
import art.arcane.openblocks.item.OBHeightMapItem;
import art.arcane.openblocks.menu.OBProjectorMenu;
import art.arcane.openblocks.network.OBNetwork;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBHeightMapData;
import com.google.common.collect.MapMaker;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class OBProjectorBlockEntity extends BlockEntity implements MenuProvider {

    // Projectors on the same client level read one snapshot and build one mesh for each map.
    // A snapshot can be reclaimed once no loaded projector holds it.
    private static final Map<Level, Map<Integer, ClientProjection>> CLIENT_PROJECTIONS = new WeakHashMap<>();

    private static final class ClientProjection {
        @Nullable private OBHeightMapData data;
        private long nextRequest;
    }

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override public int getSlotLimit(final int slot) { return 1; }
        @Override public boolean isItemValid(final int slot, final ItemStack stack) { return acceptsMap(stack); }

        @Override
        protected void onContentsChanged(final int slot) {
            setChanged();
            if (!isRemoved() && level instanceof ServerLevel server) refresh(server);
        }
    };

    private LazyOptional<IItemHandler> access = LazyOptional.of(() -> inventory);
    private final Map<ServerPlayer, Long> nextSnapshot = new WeakHashMap<>();
    private int rotation;
    private int displayedMap = -1;
    private long revision = -1;
    private int emittedLight = -1;
    @Nullable private ClientProjection clientProjection;

    public OBProjectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.PROJECTOR.get(), pos, state);
    }

    public static boolean acceptsMap(final ItemStack stack) {
        return stack.is(OBItems.EMPTY_MAP.get()) || stack.is(OBItems.HEIGHT_MAP.get());
    }

    public ItemStackHandler inventory() { return inventory; }
    public int rotation() { return rotation; }
    public int mapId() { return OBHeightMapItem.mapId(inventory.getStackInSlot(0)); }

    @Nullable
    public OBHeightMapData map() {
        if (level instanceof ServerLevel server) return OBHeightMapData.get(server, mapId());
        return clientProjection == null ? null : clientProjection.data;
    }

    public void rotate(final int step) {
        final int next = (rotation + step) & 3;
        if (next == rotation) return;
        rotation = next;
        setChanged();
        notifyClients();
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBProjectorBlockEntity projector) {
        if ((level.getGameTime() + pos.asLong()) % 10 == 0) projector.refresh((ServerLevel) level);
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final OBProjectorBlockEntity projector) {
        if (projector.displayedMap < 0) return;
        final ClientProjection projection = projector.clientProjection(level);
        if ((projection.data != null && projection.data.revision() >= projector.revision)
                || level.getGameTime() < projection.nextRequest) return;
        projection.nextRequest = level.getGameTime() + 20;
        OBNetwork.requestProjection(pos, projector.displayedMap);
    }

    private ClientProjection clientProjection(@Nullable final Level level) {
        if (clientProjection == null) {
            clientProjection = level == null ? new ClientProjection()
                    : CLIENT_PROJECTIONS.computeIfAbsent(level, ignored -> new MapMaker().weakValues().makeMap())
                            .computeIfAbsent(displayedMap, ignored -> new ClientProjection());
        }
        return clientProjection;
    }

    private void refresh(final ServerLevel server) {
        final ItemStack item = inventory.getStackInSlot(0);
        final int id = mapId();
        final OBHeightMapData data = OBHeightMapData.get(server, id);
        if (data == null && acceptsMap(item)) {
            OBHeightMapData.reportMissingReference(server, worldPosition, id);
            final int created = OBHeightMapData.createCanvas(server, worldPosition, OBEmptyMapItem.scale(item));
            inventory.setStackInSlot(0, OBHeightMapItem.create(item, created, OBHeightMapData.get(server, created)));
            return;
        }

        final BlockState state = getBlockState();
        final boolean active = data != null;
        if (active && item.getOrCreateTag().getLong("Survey") != data.survey()) {
            OBHeightMapItem.updateMetadata(item, data);
            setChanged();
        }
        final long nextRevision = active ? data.revision() : -1;
        final boolean changed = displayedMap != id || revision != nextRevision;
        displayedMap = id;
        revision = nextRevision;
        if (state.getValue(OBProjectorBlock.ACTIVE) != active) {
            server.setBlock(worldPosition, state.setValue(OBProjectorBlock.ACTIVE, active), Block.UPDATE_ALL);
        } else if (changed) {
            notifyClients();
        }
        final int light = getBlockState().getLightEmission(server, worldPosition);
        if (light != emittedLight) {
            emittedLight = light;
            server.getChunkSource().getLightEngine().checkBlock(worldPosition);
        }
    }

    public void sendProjection(final ServerPlayer viewer, final int requestedMap) {
        if (!(level instanceof ServerLevel server) || viewer.level() != server || requestedMap != mapId()
                || !server.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false).contains(viewer)) return;
        final OBHeightMapData data = map();
        if (data == null || server.getGameTime() < nextSnapshot.getOrDefault(viewer, 0L)) return;
        nextSnapshot.put(viewer, server.getGameTime() + 5);
        OBNetwork.sendProjection(viewer, worldPosition, requestedMap, data.save(new CompoundTag()));
    }

    public void acceptProjection(final int id, final CompoundTag tag) {
        final long receivedRevision = tag.getLong("Revision");
        if (id != displayedMap || receivedRevision < revision) return;
        final ClientProjection projection = clientProjection(level);
        if (projection.data != null && receivedRevision <= projection.data.revision()) return;
        projection.data = OBHeightMapData.load(tag);
        projection.nextRequest = 0;
    }

    private void notifyClients() {
        if (level instanceof ServerLevel) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void dropMap() {
        final ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty()) Block.popResource(level, worldPosition, stack);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, 66, 0);
    }

    @Override
    public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory playerInventory, final Player player) {
        return new OBProjectorMenu(id, playerInventory, this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction side) {
        return !isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER ? access.cast() : super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        access.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        access = LazyOptional.of(() -> inventory);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putByte("Rotation", (byte) rotation);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
        rotation = tag.getByte("Rotation") & 3;
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = new CompoundTag();
        final OBHeightMapData data = map();
        tag.putInt("Map", data == null ? -1 : mapId());
        tag.putLong("Revision", data == null ? -1 : data.revision());
        tag.putByte("Rotation", (byte) rotation);
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        final int id = tag.getInt("Map");
        final long nextRevision = tag.getLong("Revision");
        if (id != displayedMap) {
            clientProjection = null;
        } else if (nextRevision < revision && clientProjection != null) {
            clientProjection.data = null;
            clientProjection.nextRequest = 0;
        }
        displayedMap = id;
        revision = nextRevision;
        rotation = tag.getByte("Rotation") & 3;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void onDataPacket(final Connection connection, final ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
}
