package art.arcane.openblocks.world;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public final class OBMachineItems {

    private static final Direction[] SIDES = Direction.values();

    private OBMachineItems() {}

    @Nullable
    public static Direction chooseFace(int mask, final RandomSource random) {
        if (mask == 0) return null;
        for (int skip = random.nextInt(Integer.bitCount(mask)); skip > 0; skip--) mask &= mask - 1;
        return SIDES[Integer.numberOfTrailingZeros(mask)];
    }

    @Nullable
    public static IItemHandler adjacent(final Level level, final BlockPos pos, final Direction face) {
        final BlockPos adjacent = pos.relative(face);
        if (!level.hasChunkAt(adjacent)) return null;
        final var entity = level.getBlockEntity(adjacent);
        return entity == null ? null : entity.getCapability(ForgeCapabilities.ITEM_HANDLER, face.getOpposite()).orElse(null);
    }

    public static boolean push(final Level level, final BlockPos pos, final ItemStackHandler source, final int slot,
            final IItemHandler destination, final int limit) {
        final ItemStack offered = source.extractItem(slot, limit, true);
        if (offered.isEmpty()) return false;
        final int accepted = offered.getCount() - ItemHandlerHelper.insertItemStacked(destination, offered, true).getCount();
        if (accepted <= 0) return false;
        final ItemStack removed = source.extractItem(slot, accepted, false);
        final int count = removed.getCount();
        final ItemStack remainder = ItemHandlerHelper.insertItemStacked(destination, removed, false);
        if (!remainder.isEmpty()) {
            final ItemStack current = source.getStackInSlot(slot);
            final int room = Math.max(0, Math.min(source.getSlotLimit(slot), remainder.getMaxStackSize()) - current.getCount());
            final int restored = current.isEmpty() || ItemStack.isSameItemSameTags(current, remainder) ? Math.min(room, remainder.getCount()) : 0;
            if (restored > 0) source.setStackInSlot(slot, remainder.copyWithCount(current.getCount() + restored));
            Block.popResource(level, pos, remainder.copyWithCount(remainder.getCount() - restored));
        }
        return count > remainder.getCount();
    }

    public static void pullOne(final Level level, final BlockPos pos, final IItemHandler source, final ItemStackHandler target,
            final int targetSlot) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            final ItemStack offered = source.extractItem(slot, 1, true);
            if (offered.isEmpty() || !target.insertItem(targetSlot, offered, true).isEmpty()) continue;
            final ItemStack retrieved = source.extractItem(slot, 1, false);
            if (retrieved.isEmpty()) continue;
            final ItemStack leftover = target.insertItem(targetSlot, retrieved, false);
            if (!leftover.isEmpty()) {
                final ItemStack returned = source.insertItem(slot, leftover, false);
                Block.popResource(level, pos, returned);
            }
            return;
        }
    }
}
