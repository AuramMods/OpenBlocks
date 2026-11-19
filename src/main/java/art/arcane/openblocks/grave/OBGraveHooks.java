package art.arcane.openblocks.grave;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.api.OBGraveDropsEvent;
import art.arcane.openblocks.block.entity.OBGraveBlockEntity;
import art.arcane.openblocks.command.OBInventoryStore;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.world.OBGameRules;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBGraveHooks {

    private static final int[] SEARCH_Y_OFFSETS = {0, 1, -1, 2, -2, 3, -3};
    private static final int MAX_SEARCH_RADIUS = 4;
    private static final int PLACEMENT_FLAGS = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private OBGraveHooks() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(final LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return;
        if (event.isCanceled()) return;

        final ServerLevel level = player.serverLevel();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        if (!level.getGameRules().getBoolean(OBGameRules.SPAWN_GRAVES)) return;

        final OBGraveDropsEvent dropsEvent = new OBGraveDropsEvent(player);
        for (final ItemEntity itemEntity : event.getDrops()) {
            if (!itemEntity.getItem().isEmpty()) dropsEvent.addItem(itemEntity);
        }
        if (dropsEvent.getDrops().isEmpty()) return;
        if (MinecraftForge.EVENT_BUS.post(dropsEvent)) return;

        final List<ItemEntity> graveDrops = new ArrayList<>();
        final List<ItemEntity> worldDrops = new ArrayList<>();
        for (final OBGraveDropsEvent.ItemAction action : dropsEvent.getDrops()) {
            final ItemEntity itemEntity = action.getItem();
            final ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            switch (action.getAction()) {
                case STORE -> graveDrops.add(itemEntity);
                case DROP -> worldDrops.add(itemEntity);
                case DELETE -> {
                    // intentionally omitted from both grave storage and world drops.
                }
            }
        }

        final boolean stored = !graveDrops.isEmpty() && tryStoreInGrave(level, player, graveDrops);
        event.getDrops().clear();
        event.getDrops().addAll(worldDrops);
        if (!stored) event.getDrops().addAll(graveDrops);
    }

    private static boolean tryStoreInGrave(
            final ServerLevel level, final ServerPlayer player, final List<ItemEntity> graveDrops) {
        final List<ItemStack> stacks = graveDrops.stream().map(drop -> drop.getItem().copy()).toList();
        final BlockPos gravePos = findPlacement(level, player, player.blockPosition());
        final boolean placed = gravePos != null && placeGrave(level, gravePos, player, stacks);
        if (placed) {
            player.sendSystemMessage(Component.translatableWithFallback("grave.open_blocks.placed",
                    "Your grave is at %s, %s, %s.", gravePos.getX(), gravePos.getY(), gravePos.getZ()));
        }
        if (OBServerConfig.GRAVE_BACKUPS.get()) backup(player, placed ? gravePos : null, stacks);
        return placed;
    }

    private static void backup(final ServerPlayer player, final BlockPos gravePos, final List<ItemStack> stacks) {
        try {
            final String id = OBInventoryStore.storeGraveBackup(player, gravePos, stacks);
            player.sendSystemMessage(Component.translatableWithFallback("grave.open_blocks.backup_saved",
                    "Grave backup saved as '%s'.", id));
        } catch (final IOException e) {
            player.sendSystemMessage(Component.translatableWithFallback("grave.open_blocks.backup_write_failed",
                    "Could not write the grave backup. %s", e.getMessage()));
        }
    }

    private static boolean placeGrave(
            final ServerLevel level,
            final BlockPos gravePos,
            final ServerPlayer player,
            final List<ItemStack> stacks) {
        final BlockState state = OBBlocks.GRAVE.get().defaultBlockState();
        final var snapshot = BlockSnapshot.create(level.dimension(), level, gravePos, PLACEMENT_FLAGS);
        if (!level.setBlock(gravePos, state, PLACEMENT_FLAGS)) return false;
        boolean placed = false;
        try {
            if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)
                    || !(level.getBlockEntity(gravePos) instanceof OBGraveBlockEntity grave)) return false;
            grave.initializeFromDeath(player, stacks, deathMessage(player));
            placed = true;
        } finally {
            if (!placed) snapshot.restore(true, false);
        }
        level.markAndNotifyBlock(gravePos, level.getChunkAt(gravePos), snapshot.getReplacedBlock(), state, Block.UPDATE_ALL, Block.UPDATE_LIMIT);
        return true;
    }

    private static Component deathMessage(final ServerPlayer player) {
        final String day = String.format(Locale.ROOT, "%.1f", player.level().getGameTime() / 24000.0);
        return Component.translatableWithFallback("grave.open_blocks.death_message", "%s (day: %s)",
                player.getCombatTracker().getDeathMessage(), Component.literal(day).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
    }

    private static BlockPos findPlacement(final ServerLevel level, final ServerPlayer player, final BlockPos origin) {
        for (int radius = 0; radius <= MAX_SEARCH_RADIUS; radius++) {
            for (final int yOffset : SEARCH_Y_OFFSETS) {
                final int y = origin.getY() + yOffset;
                for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                    for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                        if (Math.max(Math.abs(xOffset), Math.abs(zOffset)) != radius) continue;

                        final BlockPos candidate = new BlockPos(origin.getX() + xOffset, y, origin.getZ() + zOffset);
                        if (canPlaceGrave(level, player, candidate)) return candidate;
                    }
                }
            }
        }

        return null;
    }

    private static boolean canPlaceGrave(final ServerLevel level, final ServerPlayer player, final BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos)) return false;

        final BlockState stateAtPos = level.getBlockState(pos);
        if (!stateAtPos.canBeReplaced() || stateAtPos.hasBlockEntity()) return false;

        final BlockPos belowPos = pos.below();
        if (!level.isInWorldBounds(belowPos)) return false;

        final BlockState belowState = level.getBlockState(belowPos);
        return belowState.isFaceSturdy(level, belowPos, Direction.UP);
    }
}
