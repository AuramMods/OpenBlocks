package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.command.OBInventoryStore;
import art.arcane.openblocks.registry.OBBlockEntities;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class OBGraveBlockEntity extends BlockEntity {

    private static final String TAG_INVENTORY_ID = "InventoryId";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_OWNER_UUID = "OwnerUUID";
    private static final String TAG_CREATED = "Created";

    private String inventoryId = "";
    private String ownerName = "";
    private String ownerUuid = "";
    private long created;

    public OBGraveBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.GRAVE.get(), pos, state);
    }

    public void initializeFromDeath(final ServerPlayer player, final String inventoryId) {
        this.inventoryId = inventoryId == null ? "" : inventoryId;
        this.ownerName = player.getGameProfile().getName();
        this.ownerUuid = player.getUUID().toString();
        this.created = System.currentTimeMillis();
        setChanged();
    }

    public boolean claimLoot(final ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (inventoryId == null || inventoryId.isBlank()) {
            player.sendSystemMessage(Component.literal("This grave is empty."));
            return false;
        }

        final String dumpId = inventoryId;
        final List<ItemStack> loot;
        try {
            loot = OBInventoryStore.readDroppedItems(serverLevel, dumpId);
        } catch (final IOException e) {
            player.sendSystemMessage(Component.literal(
                    "OpenBlocks failed to read grave backup '" + dumpId + "': " + e.getMessage()));
            return false;
        }

        if (loot.isEmpty()) {
            try {
                OBInventoryStore.deleteDump(serverLevel, dumpId);
            } catch (final IOException ignored) {
                // Empty dump cleanup is best-effort.
            }
            clearAndRemove(serverLevel);
            player.sendSystemMessage(Component.literal("This grave is empty."));
            return true;
        }

        final double x = worldPosition.getX() + 0.5D;
        final double y = worldPosition.getY() + 0.5D;
        final double z = worldPosition.getZ() + 0.5D;
        for (final ItemStack stack : loot) {
            Containers.dropItemStack(serverLevel, x, y, z, stack.copy());
        }

        try {
            OBInventoryStore.deleteDump(serverLevel, dumpId);
        } catch (final IOException e) {
            player.sendSystemMessage(Component.literal(
                    "OpenBlocks recovered grave loot but failed to delete backup '" + dumpId + "': " + e.getMessage()));
        }

        clearAndRemove(serverLevel);
        player.sendSystemMessage(Component.literal(
                "Recovered " + loot.size() + " stack(s) from grave '" + dumpId + "'."));
        return true;
    }

    private void clearAndRemove(final ServerLevel level) {
        this.inventoryId = "";
        this.ownerName = "";
        this.ownerUuid = "";
        this.created = 0L;
        setChanged();
        level.removeBlock(worldPosition, false);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(TAG_INVENTORY_ID, inventoryId);
        tag.putString(TAG_OWNER_NAME, ownerName);
        tag.putString(TAG_OWNER_UUID, ownerUuid);
        tag.putLong(TAG_CREATED, created);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        this.inventoryId = tag.getString(TAG_INVENTORY_ID);
        this.ownerName = tag.getString(TAG_OWNER_NAME);
        this.ownerUuid = tag.getString(TAG_OWNER_UUID);
        this.created = tag.getLong(TAG_CREATED);
    }
}
