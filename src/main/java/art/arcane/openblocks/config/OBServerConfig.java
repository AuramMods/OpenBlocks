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
    public static final ForgeConfigSpec.IntValue SPONGE_RANGE;
    public static final ForgeConfigSpec.BooleanValue SPONGE_BLOCK_UPDATE;
    public static final ForgeConfigSpec.IntValue SPONGE_STICK_RANGE;
    public static final ForgeConfigSpec.IntValue SPONGE_STICK_DURABILITY;
    public static final ForgeConfigSpec.BooleanValue SPONGE_STICK_BLOCK_UPDATE;
    public static final ForgeConfigSpec.IntValue TANK_BUCKETS;
    public static final ForgeConfigSpec.BooleanValue TANK_BALANCE;
    public static final ForgeConfigSpec.IntValue TANK_BALANCE_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue TANK_BUCKET_DRAIN;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TANK_ADDITIONAL_BUCKETS;
    public static final ForgeConfigSpec.BooleanValue TANK_LIGHT;
    public static final ForgeConfigSpec.BooleanValue TANK_CREATIVE_FLUIDS;
    public static final ForgeConfigSpec.IntValue XP_FLUID_RATIO;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> XP_ADDITIONAL_FLUIDS;
    public static final ForgeConfigSpec.IntValue BLOCK_BREAKER_ACTION_LIMIT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_BREAKER_TOOLS;
    public static final ForgeConfigSpec.IntValue BLOCK_PLACER_ACTION_LIMIT;
    public static final ForgeConfigSpec.DoubleValue ITEM_DROPPER_MAX_SPEED;

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
        builder.push("sponge");
        SPONGE_RANGE = builder.comment("Liquid removal radius of the sponge block.")
                .defineInRange("spongeRange", 3, 0, 32);
        SPONGE_BLOCK_UPDATE = builder.comment("Notify neighbors when the sponge block removes liquids.")
                .define("blockUpdate", false);
        SPONGE_STICK_RANGE = builder.comment("Liquid removal radius of the Sponge on a Stick.")
                .defineInRange("spongeStickRange", 3, 0, 32);
        SPONGE_STICK_DURABILITY = builder.defineInRange("spongeStickUseCount", 256, 1, Integer.MAX_VALUE);
        SPONGE_STICK_BLOCK_UPDATE = builder.comment("Notify neighbors when the Sponge on a Stick removes liquids.")
                .define("stickBlockUpdate", false);
        builder.pop();
        builder.push("tanks");
        TANK_BUCKETS = builder.defineInRange("bucketsPerTank", 16, 1, 32767);
        TANK_BALANCE = builder.define("tankTicks", true);
        TANK_BALANCE_THRESHOLD = builder.defineInRange("fluidDifferenceUpdateThreshold", 0, 0, Integer.MAX_VALUE);
        TANK_BUCKET_DRAIN = builder.define("allowBucketDrain", true);
        TANK_ADDITIONAL_BUCKETS = builder.comment("Additional fluid container item IDs that can be filled from a placed tank.")
                .defineListAllowEmpty("bucketItems", List.of(), value -> value instanceof String);
        TANK_LIGHT = builder.define("emitLight", true);
        TANK_CREATIVE_FLUIDS = builder.define("displayAllFluids", true);
        builder.pop();
        builder.push("experience");
        XP_FLUID_RATIO = builder.comment("Millibuckets of XP Juice per experience point.")
                .defineInRange("xpToLiquidRatio", 20, 1, Integer.MAX_VALUE / 8);
        XP_ADDITIONAL_FLUIDS = builder.comment("Extra XP liquids in registry-id=millibuckets-per-point form. Unqualified legacy names match any namespace.")
                .defineListAllowEmpty("additionalXpFluids", List.of("experience=20"), value -> value instanceof String);
        builder.pop();
        builder.push("breaker");
        BLOCK_BREAKER_ACTION_LIMIT = builder.comment("Maximum number of break attempts per machine in one tick.")
                .defineInRange("actionLimit", 16, 0, Integer.MAX_VALUE);
        BLOCK_BREAKER_TOOLS = builder.comment("Item IDs to try in order when choosing a tool for the target block.")
                .defineListAllowEmpty("fakePlayerBlockBreakTools", List.of("minecraft:diamond_pickaxe", "minecraft:diamond_shovel",
                        "minecraft:diamond_axe", "minecraft:diamond_sword", "minecraft:shears"), value -> value instanceof String);
        builder.pop();
        builder.push("placer");
        BLOCK_PLACER_ACTION_LIMIT = builder.comment("Maximum number of placement attempts per machine in one tick.")
                .defineInRange("actionLimit", 16, 0, Integer.MAX_VALUE);
        builder.pop();
        builder.push("itemdropper");
        ITEM_DROPPER_MAX_SPEED = builder.comment("Maximum launch speed selectable in the Item Dropper menu, in blocks per tick.")
                .defineInRange("maxItemDropSpeed", 4.0, 0.0, Integer.MAX_VALUE / 10.0);
        builder.pop();
        SPEC = builder.build();
    }

    private OBServerConfig() {}
}
