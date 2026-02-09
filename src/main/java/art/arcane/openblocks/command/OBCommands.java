package art.arcane.openblocks.command;

import art.arcane.openblocks.capability.OBCapabilities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
        dispatcher.register(Commands.literal("ob_inventory")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("store")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> inventoryPlaceholder(ctx, "store"))))
                .then(Commands.literal("restore")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("inventory_id", StringArgumentType.word())
                                        .executes(ctx -> inventoryPlaceholder(ctx, "restore")))))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("inventory_id", StringArgumentType.word())
                                .executes(ctx -> inventoryPlaceholder(ctx, "spawn"))
                                .then(Commands.argument("sub_inventory", StringArgumentType.word())
                                        .executes(ctx -> inventoryPlaceholder(ctx, "spawn"))
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .executes(ctx -> inventoryPlaceholder(ctx, "spawn")))))));
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

    private static int inventoryPlaceholder(final CommandContext<CommandSourceStack> ctx, final String subCommand) {
        ctx.getSource().sendSuccess(() -> Component.literal(
                "/ob_inventory " + subCommand + " is registered, but inventory dump/restore behavior is not ported yet."), false);
        return 1;
    }

    private static int getLuck(final ServerPlayer player) {
        return OBCapabilities.getLuck(player);
    }

    private static int addLuck(final ServerPlayer player, final int amount) {
        return OBCapabilities.addLuck(player, amount);
    }
}
