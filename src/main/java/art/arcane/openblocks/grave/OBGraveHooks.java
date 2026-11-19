package art.arcane.openblocks.grave;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.api.OBGraveDropsEvent;
import art.arcane.openblocks.api.OBGraveSpawnEvent;
import art.arcane.openblocks.block.OBGraveBlock;
import art.arcane.openblocks.block.entity.OBGraveBlockEntity;
import art.arcane.openblocks.command.OBInventoryStore;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBGameRules;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBGraveHooks {

    private static final Comparator<BlockPos> SEARCH_ORDER = Comparator.comparingInt(OBGraveHooks::manhattanDistance)
            .thenComparing(Comparator.comparingInt(OBGraveHooks::axisDistance).reversed())
            .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);
    private static final int PLACEMENT_FLAGS = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static SearchOffsets cachedSearch = new SearchOffsets(-1, List.of());

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
        final Set<ItemEntity> handled = new HashSet<>();
        for (final OBGraveDropsEvent.ItemAction action : dropsEvent.getDrops()) {
            final ItemEntity itemEntity = action.getItem();
            if (itemEntity.getItem().isEmpty() || !handled.add(itemEntity)) continue;

            switch (action.getAction()) {
                case STORE -> graveDrops.add(itemEntity);
                case DROP -> worldDrops.add(itemEntity);
                case DELETE -> {
                    // intentionally omitted from both grave storage and world drops.
                }
            }
        }

        final boolean stored = !graveDrops.isEmpty() && tryStoreInGrave(level, player, graveDrops, worldDrops);
        event.getDrops().clear();
        for (final ItemEntity drop : worldDrops) if (!drop.getItem().isEmpty()) event.getDrops().add(drop);
        if (!stored) event.getDrops().addAll(graveDrops);
    }

    private static boolean tryStoreInGrave(
            final ServerLevel level, final ServerPlayer player, final List<ItemEntity> graveDrops, final List<ItemEntity> worldDrops) {
        final ItemEntity consumed;
        if (OBServerConfig.GRAVE_REQUIRES_ITEM.get() && !player.isCreative()) {
            consumed = java.util.stream.Stream.concat(graveDrops.stream(), worldDrops.stream())
                    .filter(drop -> drop.getItem().is(OBItems.GRAVE.get())).findFirst().orElse(null);
            if (consumed == null) return false;
        } else {
            consumed = null;
        }

        final var event = new OBGraveSpawnEvent(player, findPlacement(level, player), copyDrops(graveDrops, consumed),
                player.getGameProfile().getName(), deathMessage(player));
        final boolean canceled = MinecraftForge.EVENT_BUS.post(event);
        final BlockPos gravePos = event.getLocation();
        final List<ItemStack> stacks = event.getLoot().stream().filter(Objects::nonNull).filter(stack -> !stack.isEmpty()).toList();
        final boolean placed = !canceled && gravePos != null && canPlaceGrave(level, player, gravePos, OBServerConfig.GRAVE_DESTRUCTIVE.get())
                && placeGrave(level, gravePos, player, stacks, event.getEpitaph(), event.getDeathMessage());
        if (placed) {
            if (consumed != null) consumed.getItem().shrink(1);
            player.sendSystemMessage(Component.translatableWithFallback("grave.open_blocks.placed",
                    "Your grave is at %s, %s, %s.", gravePos.getX(), gravePos.getY(), gravePos.getZ()));
        }
        if (OBServerConfig.GRAVE_BACKUPS.get()) backup(player, placed ? gravePos : null, placed ? stacks : copyDrops(graveDrops, null));
        return placed;
    }

    private static List<ItemStack> copyDrops(final List<ItemEntity> drops, @Nullable final ItemEntity consumed) {
        return drops.stream().map(drop -> drop == consumed ? drop.getItem().copyWithCount(drop.getItem().getCount() - 1) : drop.getItem().copy())
                .filter(stack -> !stack.isEmpty()).toList();
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
            final List<ItemStack> stacks,
            final String epitaph,
            final Component deathMessage) {
        final BlockState previous = level.getBlockState(gravePos);
        if (!previous.canBeReplaced() && MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, gravePos, previous, player))) return false;
        if (level.getBlockState(gravePos) != previous) return false;

        final BlockPos below = gravePos.below();
        final boolean makeBase = OBServerConfig.GRAVE_ADD_BASE.get() && mayChange(level, player, below) && level.isEmptyBlock(below);
        final BlockState dirt = Blocks.DIRT.defaultBlockState();
        final BlockState state = OBBlocks.GRAVE.get().defaultBlockState().setValue(OBGraveBlock.BASE,
                makeBase || OBGraveBlock.hasBase(level.getBlockState(below)));
        final var graveSnapshot = BlockSnapshot.create(level.dimension(), level, gravePos, PLACEMENT_FLAGS);
        final var baseSnapshot = makeBase ? BlockSnapshot.create(level.dimension(), level, below, PLACEMENT_FLAGS) : null;
        boolean placed = false;
        try {
            if (makeBase && !level.setBlock(below, dirt, PLACEMENT_FLAGS)) return false;
            if (!level.setBlock(gravePos, state, PLACEMENT_FLAGS)) return false;
            final boolean denied = baseSnapshot == null ? ForgeEventFactory.onBlockPlace(player, graveSnapshot, Direction.UP)
                    : ForgeEventFactory.onMultiBlockPlace(player, List.of(graveSnapshot, baseSnapshot), Direction.UP);
            if (denied || level.getBlockState(gravePos) != state || (makeBase && level.getBlockState(below) != dirt)
                    || !(level.getBlockEntity(gravePos) instanceof OBGraveBlockEntity grave)) return false;
            grave.initializeFromDeath(player, stacks, epitaph, deathMessage);
            placed = true;
        } finally {
            if (!placed) {
                graveSnapshot.restore(true, false);
                if (baseSnapshot != null) baseSnapshot.restore(true, false);
            }
        }
        if (baseSnapshot != null) notifyPlacement(level, baseSnapshot, dirt);
        notifyPlacement(level, graveSnapshot, state);
        return true;
    }

    private static void notifyPlacement(final ServerLevel level, final BlockSnapshot snapshot, final BlockState state) {
        final BlockPos pos = snapshot.getPos();
        level.markAndNotifyBlock(pos, level.getChunkAt(pos), snapshot.getReplacedBlock(), state, Block.UPDATE_ALL, Block.UPDATE_LIMIT);
    }

    private static Component deathMessage(final ServerPlayer player) {
        final String day = String.format(Locale.ROOT, "%.1f", player.level().getGameTime() / 24000.0);
        return Component.translatableWithFallback("grave.open_blocks.death_message", "%s (day: %s)",
                player.getCombatTracker().getDeathMessage(), Component.literal(day).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
    }

    @Nullable
    private static BlockPos findPlacement(final ServerLevel level, final ServerPlayer player) {
        final int min = minGraveY(level);
        final int max = maxGraveY(level);
        if (min > max) return null;
        final BlockPos origin = player.blockPosition().atY(Mth.clamp(player.blockPosition().getY(), min, max));
        final int radius = OBServerConfig.GRAVE_SPAWN_RANGE.get() / 2;
        if (cachedSearch.radius() != radius) {
            cachedSearch = new SearchOffsets(radius, BlockPos.betweenClosedStream(-radius, -radius, -radius, radius, radius, radius)
                    .map(BlockPos::immutable).sorted(SEARCH_ORDER).toList());
        }
        final List<BlockPos> offsets = cachedSearch.offsets();
        final BlockPos polite = firstPlacement(level, player, origin, offsets, false);
        return polite == null && OBServerConfig.GRAVE_DESTRUCTIVE.get() ? firstPlacement(level, player, origin, offsets, true) : polite;
    }

    @Nullable
    private static BlockPos firstPlacement(final ServerLevel level, final ServerPlayer player, final BlockPos origin,
            final List<BlockPos> offsets, final boolean destructive) {
        for (final BlockPos offset : offsets) {
            final BlockPos candidate = origin.offset(offset);
            if (canPlaceGrave(level, player, candidate, destructive)) return candidate;
        }
        return null;
    }

    private static boolean canPlaceGrave(final ServerLevel level, final ServerPlayer player, final BlockPos pos, final boolean destructive) {
        if (pos.getY() < minGraveY(level) || pos.getY() > maxGraveY(level) || !mayChange(level, player, pos)) return false;
        final BlockState existing = level.getBlockState(pos);
        if (existing.hasBlockEntity() || existing.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || existing.hasProperty(BlockStateProperties.BED_PART) || existing.getOptionalValue(BlockStateProperties.EXTENDED).orElse(false)
                || existing.getBlock() instanceof PistonHeadBlock) return false;
        return existing.canBeReplaced() || destructive && existing.getDestroySpeed(level, pos) >= 0;
    }

    private static int minGraveY(final ServerLevel level) {
        final int minimum = OBServerConfig.GRAVE_MIN_Y.get();
        return minimum == Integer.MIN_VALUE ? level.getMinBuildHeight() + 6 : Math.max(level.getMinBuildHeight(), minimum);
    }

    private static int maxGraveY(final ServerLevel level) {
        final int maximum = OBServerConfig.GRAVE_MAX_Y.get();
        return maximum == Integer.MAX_VALUE ? level.getMaxBuildHeight() - 7 : Math.min(level.getMaxBuildHeight() - 1, maximum);
    }

    private static boolean mayChange(final ServerLevel level, final ServerPlayer player, final BlockPos pos) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(player, pos);
    }

    private static int manhattanDistance(final BlockPos pos) { return Math.abs(pos.getX()) + Math.abs(pos.getY()) + Math.abs(pos.getZ()); }
    private static int axisDistance(final BlockPos pos) { return Math.max(Math.max(Math.abs(pos.getX()), Math.abs(pos.getY())), Math.abs(pos.getZ())); }

    private record SearchOffsets(int radius, List<BlockPos> offsets) {}
}
