package art.arcane.openblocks.block;

import art.arcane.openblocks.config.OBServerConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

record OBElevatorRules(Map<Block, OBElevatorRules.Action> blockers, Map<BlockState, DyeColor> overrides) {

    enum Action { IGNORE, ABORT, INCREMENT }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static List<String> lastBlockers = List.of();
    private static List<String> lastOverrides = List.of();
    private static OBElevatorRules current = new OBElevatorRules(Map.of(), Map.of());

    static synchronized OBElevatorRules configured() {
        final var blockers = OBServerConfig.ELEVATOR_RULES.get();
        final var overrides = OBServerConfig.ELEVATOR_OVERRIDES.get();
        if (!lastBlockers.equals(blockers) || !lastOverrides.equals(overrides)) {
            current = new OBElevatorRules(parseBlockers(blockers), parseOverrides(overrides));
            lastBlockers = List.copyOf(blockers);
            lastOverrides = List.copyOf(overrides);
        }
        return current;
    }

    @Nullable
    Landing identify(final BlockState state) {
        final DyeColor override = overrides.get(state);
        if (override != null) return new Landing(override, null);
        if (state.getBlock() instanceof OBElevatorBlock elevator) {
            return new Landing(state.getValue(OBElevatorBlock.COLOR), elevator.getTargetYaw(state));
        }
        return null;
    }

    private static Map<Block, Action> parseBlockers(final List<? extends String> entries) {
        final Map<Block, Action> rules = new HashMap<>();
        for (final String entry : entries) {
            final int split = entry.lastIndexOf(':');
            if (split < 1) {
                LOGGER.warn("Invalid elevator rule '{}'", entry);
                continue;
            }
            try {
                final ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, split).trim());
                if (id == null) throw new IllegalArgumentException("Invalid block ID");
                final Block block = BuiltInRegistries.BLOCK.getOptional(id)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown block " + id));
                rules.put(block, Action.valueOf(entry.substring(split + 1).trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid elevator rule '{}': {}", entry, e.getMessage());
            }
        }
        return Map.copyOf(rules);
    }

    private static Map<BlockState, DyeColor> parseOverrides(final List<? extends String> entries) {
        final Map<BlockState, DyeColor> result = new HashMap<>();
        for (final String entry : entries) {
            final String[] parts = entry.split(";", 2);
            String block = parts[0].trim();
            final int legacyProperties = block.indexOf('#');
            if (legacyProperties >= 0) {
                block = block.substring(0, legacyProperties) + "[" + block.substring(legacyProperties + 1) + "]";
            }
            final DyeColor color = parts.length == 1 ? DyeColor.WHITE : DyeColor.byName(parts[1].trim(), null);
            if (color == null) {
                LOGGER.warn("Invalid elevator color in override '{}'", entry);
                continue;
            }
            try {
                final var parsed = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), block, false);
                for (final BlockState state : parsed.blockState().getBlock().getStateDefinition().getPossibleStates()) {
                    if (!state.isAir() && state.getValues().entrySet().containsAll(parsed.properties().entrySet())) {
                        result.put(state, color);
                    }
                }
            } catch (CommandSyntaxException e) {
                LOGGER.warn("Invalid elevator override '{}': {}", entry, e.getMessage());
            }
        }
        return Map.copyOf(result);
    }

    record Landing(DyeColor color, @Nullable Float yaw) {}
}
