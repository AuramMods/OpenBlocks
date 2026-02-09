package art.arcane.openblocks.grave;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBGraveBlockEntity;
import art.arcane.openblocks.command.OBInventoryStore;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.world.OBGameRules;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBGraveHooks {

    private static final int[] SEARCH_Y_OFFSETS = {0, 1, -1, 2, -2, 3, -3};
    private static final int MAX_SEARCH_RADIUS = 4;

    private OBGraveHooks() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(final LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return;
        if (event.isCanceled()) return;

        final ServerLevel level = player.serverLevel();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        if (!level.getGameRules().getBoolean(OBGameRules.SPAWN_GRAVES)) return;

        final List<ItemStack> drops = new ArrayList<>();
        for (final ItemEntity itemEntity : event.getDrops()) {
            final ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) drops.add(stack.copy());
        }

        if (drops.isEmpty()) return;

        final String id;
        try {
            id = OBInventoryStore.storeDroppedItems(player, "grave", drops);
        } catch (final IOException e) {
            player.sendSystemMessage(Component.literal(
                    "OpenBlocks failed to store grave backup: " + e.getMessage()));
            return;
        }

        final BlockPos gravePos = findPlacement(level, player.blockPosition());
        if (gravePos == null) {
            player.sendSystemMessage(Component.literal(
                    "OpenBlocks stored your grave backup as '" + id + "' (no valid placement found)."));
            return;
        }

        if (!placeGrave(level, gravePos, player, id)) {
            player.sendSystemMessage(Component.literal(
                    "OpenBlocks stored your grave backup as '" + id + "' (grave placement failed)."));
            return;
        }

        event.getDrops().clear();
        player.sendSystemMessage(Component.literal(
                "OpenBlocks placed your grave at "
                        + gravePos.getX() + ", " + gravePos.getY() + ", " + gravePos.getZ()
                        + " with backup '" + id + "'."));
    }

    private static boolean placeGrave(
            final ServerLevel level,
            final BlockPos gravePos,
            final ServerPlayer player,
            final String inventoryId) {
        if (!level.setBlock(gravePos, OBBlocks.GRAVE.get().defaultBlockState(), 3)) return false;

        if (!(level.getBlockEntity(gravePos) instanceof OBGraveBlockEntity graveBlockEntity)) {
            level.removeBlock(gravePos, false);
            return false;
        }

        graveBlockEntity.initializeFromDeath(player, inventoryId);
        return true;
    }

    private static BlockPos findPlacement(final ServerLevel level, final BlockPos origin) {
        for (int radius = 0; radius <= MAX_SEARCH_RADIUS; radius++) {
            for (final int yOffset : SEARCH_Y_OFFSETS) {
                final int y = origin.getY() + yOffset;
                for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                    for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                        if (Math.max(Math.abs(xOffset), Math.abs(zOffset)) != radius) continue;

                        final BlockPos candidate = new BlockPos(origin.getX() + xOffset, y, origin.getZ() + zOffset);
                        if (canPlaceGrave(level, candidate)) return candidate;
                    }
                }
            }
        }

        return null;
    }

    private static boolean canPlaceGrave(final ServerLevel level, final BlockPos pos) {
        if (!level.isInWorldBounds(pos)) return false;

        final BlockState stateAtPos = level.getBlockState(pos);
        if (!stateAtPos.canBeReplaced()) return false;

        final BlockPos belowPos = pos.below();
        if (!level.isInWorldBounds(belowPos)) return false;

        final BlockState belowState = level.getBlockState(belowPos);
        return belowState.isFaceSturdy(level, belowPos, Direction.UP);
    }
}
