package art.arcane.openblocks.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class OBServerConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue ELEVATOR_DISTANCE;
    public static final ForgeConfigSpec.IntValue ELEVATOR_MAX_BLOCKERS;
    public static final ForgeConfigSpec.BooleanValue ELEVATOR_IGNORE_BLOCKERS;
    public static final ForgeConfigSpec.BooleanValue ELEVATOR_IGNORE_PARTIAL_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue ELEVATOR_CENTER;
    public static final ForgeConfigSpec.BooleanValue ELEVATOR_ALLOW_NON_COLLIDING_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue ELEVATOR_XP_PER_BLOCK;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELEVATOR_RULES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELEVATOR_OVERRIDES;
    public static final ForgeConfigSpec.BooleanValue ROPE_LADDER_INFINITE;
    public static final ForgeConfigSpec.IntValue SCAFFOLDING_DESPAWN_RATE;

    static {
        final var builder = new ForgeConfigSpec.Builder();
        builder.push("elevator");
        ELEVATOR_DISTANCE = builder.defineInRange("searchDistance", 20, 1, Integer.MAX_VALUE);
        ELEVATOR_MAX_BLOCKERS = builder.defineInRange("maxPassThrough", 4, 0, Integer.MAX_VALUE);
        ELEVATOR_IGNORE_BLOCKERS = builder.define("ignoreAllBlocks", false);
        ELEVATOR_IGNORE_PARTIAL_BLOCKS = builder.define("ignoreHalfBlocks", false);
        ELEVATOR_CENTER = builder.define("centerOnBlock", false);
        ELEVATOR_ALLOW_NON_COLLIDING_BLOCKS = builder.comment("Allow non-air blocks at the landing if they do not intersect the player.")
                .define("irregularBlocksArePassable", true);
        ELEVATOR_XP_PER_BLOCK = builder.comment("Experience points per vertical block between the elevators.")
                .defineInRange("elevatorXpDrainRatio", 0.0D, 0.0D, Integer.MAX_VALUE);
        ELEVATOR_RULES = builder.comment("Each rule is a block ID followed by :ignore, :abort or :increment. Example: minecraft:glass:ignore.")
                .defineListAllowEmpty("specialBlockRules", List.of(), value -> value instanceof String);
        ELEVATOR_OVERRIDES = builder.comment("Use another block as an elevator. Example: minecraft:blue_wool;blue or minecraft:oak_slab[type=top];white.")
                .defineListAllowEmpty("overrides", List.of(), value -> value instanceof String);
        builder.pop();
        builder.push("ladder");
        ROPE_LADDER_INFINITE = builder.comment("A single rope ladder extends to the next obstruction. Rungs placed in this mode do not drop.")
                .define("infiniteMode", false);
        builder.pop();
        builder.push("scaffolding");
        SCAFFOLDING_DESPAWN_RATE = builder.comment("A scaffold breaks on one in this many random ticks. 0 breaks on every random tick.")
                .defineInRange("despawnRate", 4, 0, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    private OBServerConfig() {}
}
