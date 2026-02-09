package art.arcane.openblocks.command;

import art.arcane.openblocks.capability.OBCapabilities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = art.arcane.openblocks.OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBCommands {

    private static final List<String> LEGACY_FLIM_FLAM_EFFECTS = List.of(
            "inventory-shuffle",
            "useless-tool",
            "bane",
            "epic-lore",
            "living-rename",
            "squid",
            "sheep-dye",
            "invisible-mobs",
            "sound",
            "snowballs",
            "teleport",
            "mount",
            "encase",
            "creepers",
            "disarm",
            "effect",
            "skyblock");

    private OBCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        registerLuck(dispatcher);
        registerFlimFlam(dispatcher);
        registerInventory(dispatcher);
    }

    private static void registerLuck(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("luck")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> queryLuck(ctx, EntityArgument.getPlayer(ctx, "player")))
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(ctx -> modifyLuck(
                                        ctx,
                                        EntityArgument.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "amount"))))));
    }

    private static void registerFlimFlam(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("flimflam")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> runFlimFlam(ctx, EntityArgument.getPlayer(ctx, "player"), null))
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests(OBCommands::suggestFlimFlamEffects)
                                .executes(ctx -> runFlimFlam(
                                        ctx,
                                        EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "effect"))))));
    }

    private static void registerInventory(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ob_inventory")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.literal("store")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> storeInventory(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("restore")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("inventory_id", StringArgumentType.word())
                                                .suggests(OBCommands::suggestInventoryIds)
                                                .executes(ctx -> restoreInventory(
                                                        ctx,
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "inventory_id"))))))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("inventory_id", StringArgumentType.word())
                                        .suggests(OBCommands::suggestInventoryIds)
                                        .executes(ctx -> spawnInventory(
                                                ctx,
                                                StringArgumentType.getString(ctx, "inventory_id"),
                                                "main",
                                                null))
                                        .then(Commands.argument("sub_inventory", StringArgumentType.word())
                                                .suggests(OBCommands::suggestInventoryTargets)
                                                .executes(ctx -> spawnInventory(
                                                        ctx,
                                                        StringArgumentType.getString(ctx, "inventory_id"),
                                                        StringArgumentType.getString(ctx, "sub_inventory"),
                                                        null))
                                                .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> spawnInventory(
                                                                ctx,
                                                                StringArgumentType.getString(ctx, "inventory_id"),
                                                                StringArgumentType.getString(ctx, "sub_inventory"),
                                                                IntegerArgumentType.getInteger(ctx, "slot"))))))));
    }

    private static int queryLuck(final CommandContext<CommandSourceStack> ctx, final ServerPlayer player) {
        final int luck = getLuck(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Luck for " + player.getGameProfile().getName() + ": " + luck), false);
        return luck;
    }

    private static int modifyLuck(final CommandContext<CommandSourceStack> ctx, final ServerPlayer player, final int amount) {
        final int result = addLuck(player, amount);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Adjusted luck for " + player.getGameProfile().getName() + " by " + amount + ". New value: " + result), true);
        return result;
    }

    private static int runFlimFlam(final CommandContext<CommandSourceStack> ctx, final ServerPlayer player, final String effectName) {
        final String chosenEffect;
        if (effectName == null) {
            chosenEffect = LEGACY_FLIM_FLAM_EFFECTS.get(player.getRandom().nextInt(LEGACY_FLIM_FLAM_EFFECTS.size()));
        } else {
            chosenEffect = effectName.toLowerCase(Locale.ROOT);
            if (!LEGACY_FLIM_FLAM_EFFECTS.contains(chosenEffect)) {
                ctx.getSource().sendFailure(Component.literal("Unknown flimflam effect: " + effectName));
                return 0;
            }
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Triggered flimflam '" + chosenEffect + "' on " + player.getGameProfile().getName()
                        + " (placeholder: behavior port pending)."), true);

        if (!player.equals(ctx.getSource().getEntity())) {
            player.sendSystemMessage(Component.literal(
                    "You were flimflammed with '" + chosenEffect + "' (placeholder: behavior port pending)."));
        }

        return 1;
    }

    private static CompletableFuture<Suggestions> suggestFlimFlamEffects(
            final CommandContext<CommandSourceStack> ctx,
            final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(LEGACY_FLIM_FLAM_EFFECTS, builder);
    }

    private static CompletableFuture<Suggestions> suggestInventoryIds(
            final CommandContext<CommandSourceStack> ctx,
            final SuggestionsBuilder builder) {
        final List<String> ids = OBInventoryStore.getMatchedDumps(ctx.getSource().getLevel(), builder.getRemaining());
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static CompletableFuture<Suggestions> suggestInventoryTargets(
            final CommandContext<CommandSourceStack> ctx,
            final SuggestionsBuilder builder) {
        final String inventoryId;
        try {
            inventoryId = StringArgumentType.getString(ctx, "inventory_id");
        } catch (final IllegalArgumentException ignored) {
            return Suggestions.empty();
        }

        final List<String> targets = OBInventoryStore.getAvailableTargets(ctx.getSource().getLevel(), inventoryId);
        return SharedSuggestionProvider.suggest(targets, builder);
    }

    private static int storeInventory(final CommandContext<CommandSourceStack> ctx, final ServerPlayer player) {
        try {
            final String id = OBInventoryStore.storePlayerInventory(player, "command");
            final String dumpPath = OBInventoryStore.resolveDumpPath(player.serverLevel(), id).toAbsolutePath().toString();
            ctx.getSource().sendSuccess(
                    () -> Component.literal("Stored inventory for " + player.getGameProfile().getName()
                            + " as '" + id + "' at " + dumpPath),
                    true);
            return 1;
        } catch (final IOException e) {
            ctx.getSource().sendFailure(Component.literal(
                    "Failed to store inventory for " + player.getGameProfile().getName() + ": " + e.getMessage()));
            return 0;
        }
    }

    private static int restoreInventory(
            final CommandContext<CommandSourceStack> ctx,
            final ServerPlayer player,
            final String inventoryId) {
        try {
            final boolean restored = OBInventoryStore.restoreInventory(player, inventoryId);
            if (!restored) {
                ctx.getSource().sendFailure(Component.literal("Could not restore inventory '" + inventoryId + "'."));
                return 0;
            }

            ctx.getSource().sendSuccess(
                    () -> Component.literal("Restored inventory '" + inventoryId + "' for " + player.getGameProfile().getName()),
                    true);
            return 1;
        } catch (final IOException e) {
            ctx.getSource().sendFailure(Component.literal(
                    "Failed to restore inventory '" + inventoryId + "': " + e.getMessage()));
            return 0;
        }
    }

    private static int spawnInventory(
            final CommandContext<CommandSourceStack> ctx,
            final String inventoryId,
            final String target,
            final Integer slot) {
        final List<ItemStack> stacks;
        try {
            stacks = OBInventoryStore.getSpawnStacks(ctx.getSource().getLevel(), inventoryId, target, slot);
        } catch (final IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        } catch (final IOException e) {
            ctx.getSource().sendFailure(Component.literal(
                    "Failed to read inventory '" + inventoryId + "': " + e.getMessage()));
            return 0;
        }

        if (stacks.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal(
                    "Inventory '" + inventoryId + "' has no items for target '" + target + "'."));
            return 0;
        }

        final var source = ctx.getSource();
        final var level = source.getLevel();
        final var pos = source.getPosition();

        for (final ItemStack stack : stacks) {
            Containers.dropItemStack(level, pos.x, pos.y, pos.z, stack.copy());
        }

        final String slotInfo = slot == null ? "" : (" slot " + slot);
        source.sendSuccess(
                () -> Component.literal("Spawned " + stacks.size() + " item stack(s) from '" + inventoryId
                        + "' target '" + target + "'" + slotInfo + "."),
                true);
        return stacks.size();
    }

    private static int getLuck(final ServerPlayer player) {
        return OBCapabilities.getLuck(player);
    }

    private static int addLuck(final ServerPlayer player, final int amount) {
        return OBCapabilities.addLuck(player, amount);
    }
}
