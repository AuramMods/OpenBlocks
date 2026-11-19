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
    public static final ForgeConfigSpec.BooleanValue GRAVE_BACKUPS;
    public static final ForgeConfigSpec.DoubleValue GRAVE_SPECIAL_ACTION;
    public static final ForgeConfigSpec.BooleanValue GRAVE_DESTRUCTIVE;
    public static final ForgeConfigSpec.IntValue GRAVE_SPAWN_RANGE;
    public static final ForgeConfigSpec.BooleanValue GRAVE_REQUIRES_ITEM;
    public static final ForgeConfigSpec.BooleanValue GRAVE_ADD_BASE;
    public static final ForgeConfigSpec.IntValue GRAVE_MIN_Y;
    public static final ForgeConfigSpec.IntValue GRAVE_MAX_Y;
    public static final ForgeConfigSpec.BooleanValue GRAVE_AMBIENT_MOBS;
    public static final ForgeConfigSpec.DoubleValue GRAVE_AMBIENT_RATE;
    public static final ForgeConfigSpec.BooleanValue DUMP_DEATH_INVENTORY;
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
    public static final ForgeConfigSpec.BooleanValue XP_BUCKET_DIRECT_FILL;
    public static final ForgeConfigSpec.BooleanValue XP_BUCKET_IN_CREATIVE;
    public static final ForgeConfigSpec.IntValue XP_FLUID_RATIO;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> XP_ADDITIONAL_FLUIDS;
    public static final ForgeConfigSpec.IntValue BLOCK_BREAKER_ACTION_LIMIT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_BREAKER_TOOLS;
    public static final ForgeConfigSpec.IntValue BLOCK_PLACER_ACTION_LIMIT;
    public static final ForgeConfigSpec.DoubleValue ITEM_DROPPER_MAX_SPEED;
    public static final ForgeConfigSpec.IntValue SPRINKLER_FERTILIZE_CHANCE;
    public static final ForgeConfigSpec.IntValue SPRINKLER_BONEMEAL_CHANCE;
    public static final ForgeConfigSpec.IntValue SPRINKLER_RANGE;
    public static final ForgeConfigSpec.IntValue SPRINKLER_CAPACITY;
    public static final ForgeConfigSpec.IntValue SPRINKLER_BONEMEAL_RATE;
    public static final ForgeConfigSpec.IntValue SPRINKLER_WATER_RATE;
    public static final ForgeConfigSpec.DoubleValue FAN_FORCE;
    public static final ForgeConfigSpec.DoubleValue FAN_RANGE;
    public static final ForgeConfigSpec.BooleanValue FAN_REDSTONE;
    public static final ForgeConfigSpec.BooleanValue DEV_NULL_SNEAK_GUI;
    public static final ForgeConfigSpec.BooleanValue HANG_GLIDER_THERMALS;
    public static final ForgeConfigSpec.BooleanValue CRANE_COLLISIONS;
    public static final ForgeConfigSpec.BooleanValue CRANE_SHIFT_DIRECTION;
    public static final ForgeConfigSpec.BooleanValue TURTLE_MAGNET_ENABLED;
    public static final ForgeConfigSpec.DoubleValue TURTLE_MAGNET_RANGE;
    public static final ForgeConfigSpec.DoubleValue TURTLE_MAGNET_RECALL_RANGE;
    public static final ForgeConfigSpec.BooleanValue MAGNET_PICK_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue MAGNET_PICK_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MAGNET_BLOCK_ALLOWLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MAGNET_ENTITY_ALLOWLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MAGNET_BLOCK_ENTITY_ALLOWLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CARTOGRAPHER_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue PROJECTOR_LIGHT_ENABLED;
    public static final ForgeConfigSpec.IntValue PROJECTOR_LIGHT_LEVEL;
    public static final ForgeConfigSpec.IntValue GUIDE_REDSTONE;
    public static final ForgeConfigSpec.IntValue GUIDE_MAX_HALF_AXIS;
    public static final ForgeConfigSpec.BooleanValue CANVAS_REPLACE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CANVAS_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue SERIOUS_MODE;

    public static final ForgeConfigSpec.DoubleValue IMAGINARY_USES;
    public static final ForgeConfigSpec.BooleanValue TECHNICOLOR_LOOT;

    public static final ForgeConfigSpec.BooleanValue GOLDEN_EGG_PICK_BLOCKS;

    public static final ForgeConfigSpec.BooleanValue GOLDEN_EYE_DEBUG;

    public static final ForgeConfigSpec.DoubleValue TROPHY_DROP_CHANCE;
    public static final ForgeConfigSpec.IntValue CURSOR_DISTANCE_SQUARED;
    public static final ForgeConfigSpec.BooleanValue UNSTABLE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue UNSTABLE_GRIEF;
    public static final ForgeConfigSpec.BooleanValue LAST_STAND_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> LAST_STAND_FORMULA;
    public static final ForgeConfigSpec.BooleanValue FLIM_FLAM_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FLIM_FLAM_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue FLIM_FLAM_REVERSE_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue FLIM_FLAM_SAFE_ONLY;

    static {
        final var builder = new ForgeConfigSpec.Builder();
        builder.push("cursor");
        CURSOR_DISTANCE_SQUARED = builder.comment("Maximum squared distance of a Cursor activation. Default 64 means less than eight blocks.")
                .defineInRange("cursorMaxDistance", 64, 0, Integer.MAX_VALUE);
        builder.pop();
        builder.push("features");
        UNSTABLE_ENABLED = builder.define("explosiveEnchantment", true);
        UNSTABLE_GRIEF = builder.comment("Allow Unstable III blasts to break blocks.").define("explosiveEnchantGrief", true);
        LAST_STAND_ENABLED = builder.define("lastStandEnchantment", true);
        FLIM_FLAM_ENABLED = builder.define("flimFlamEnchantment", true);
        LAST_STAND_FORMULA = builder.comment("XP cost when a hit would leave less than half a heart. Variables are hp, dmg, ench and xp.")
                .define("lastStandFormula", "max(1, 50*(1-(hp-dmg))/ench)");
        builder.pop();
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
        builder.push("graves");
        GRAVE_BACKUPS = builder.comment("Keep a separate administrator recovery file for grave loot.").define("storeContents", true);
        GRAVE_SPECIAL_ACTION = builder.comment("Chance that a successful dig plays the grave sound and starts a short thunderstorm.")
                .defineInRange("specialActionFrequency", 0.03D, 0.0D, 1.0D);
        GRAVE_DESTRUCTIVE = builder.comment("Replace suitable solid blocks only if the initial search finds no space.").define("destructiveGraves", false);
        GRAVE_SPAWN_RANGE = builder.comment("The search extends by half this value along each axis.").defineInRange("spawnRange", 10, 0, 64);
        GRAVE_REQUIRES_ITEM = builder.define("requiresGraveInInv", false);
        GRAVE_ADD_BASE = builder.define("canSpawnBase", true);
        GRAVE_MIN_Y = builder.comment("Use the smallest integer for six blocks above the dimension floor.")
                .defineInRange("minimalPosY", Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        GRAVE_MAX_Y = builder.comment("Use the largest integer for six blocks below the dimension ceiling.")
                .defineInRange("maximalPosY", Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        GRAVE_AMBIENT_MOBS = builder.define("spawnSkeletons", true);
        GRAVE_AMBIENT_RATE = builder.comment("Per-tick chance to try spawning a skeleton or bat at a grave.")
                .defineInRange("skeletonSpawnRate", 0.002D, 0.0D, 1.0D);
        builder.pop();
        builder.push("additional");
        DUMP_DEATH_INVENTORY = builder.comment("Save a separate inventory snapshot for administrator recovery when a player dies.")
                .define("dumpDeadPlayersInventories", true);
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
        builder.push("xpBucket");
        XP_BUCKET_DIRECT_FILL = builder.comment("Allow buckets to pick up Liquid XP source blocks. Tanks have their own bucket setting.")
                .define("directFill", true);
        XP_BUCKET_IN_CREATIVE = builder.comment("Include the XP Bucket in the Open Blocks creative tab and creative search.")
                .worldRestart().define("showInCreativeGui", true);
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
        builder.push("sprinkler");
        SPRINKLER_FERTILIZE_CHANCE = builder.comment("One growth attempt in this many active ticks without bone meal.")
                .defineInRange("fertilizeChance", 500, 1, Integer.MAX_VALUE);
        SPRINKLER_BONEMEAL_CHANCE = builder.comment("One growth attempt in this many active ticks while boosted by bone meal.")
                .defineInRange("bonemealFertilizeChance", 200, 1, Integer.MAX_VALUE);
        SPRINKLER_RANGE = builder.comment("Horizontal growth range on each side of the sprinkler.")
                .defineInRange("effectiveRange", 4, 0, (Integer.MAX_VALUE - 1) / 2);
        SPRINKLER_CAPACITY = builder.defineInRange("internalTankCapacity", 50, 1, Integer.MAX_VALUE);
        SPRINKLER_BONEMEAL_RATE = builder.comment("Active ticks per bone meal consumed.")
                .defineInRange("bonemealConsumeRate", 600, 1, Integer.MAX_VALUE);
        SPRINKLER_WATER_RATE = builder.comment("Active ticks per millibucket of water consumed.")
                .defineInRange("waterConsumeRate", 20, 1, Integer.MAX_VALUE);
        builder.pop();
        builder.push("fan");
        FAN_FORCE = builder.comment("Maximum horizontal velocity added per tick at full power. Force falls off with distance.")
                .defineInRange("fanForce", 0.05D, 0, 4.0D);
        FAN_RANGE = builder.comment("Distance from the airflow cone's origin, in blocks.").defineInRange("fanRange", 10.0D, 0, 128.0D);
        FAN_REDSTONE = builder.comment("Scale airflow and blade rotation by incoming redstone strength. Otherwise fans always run at full power.")
                .define("isRedstoneActivated", true);
        builder.pop();
        builder.push("devnull");
        DEV_NULL_SNEAK_GUI = builder.comment("Require sneaking when opening the /dev/null by clicking the air.")
                .define("sneakClickToGui", true);
        builder.pop();
        builder.push("hangglider");
        HANG_GLIDER_THERMALS = builder.comment("Enable daytime thermal lift and weather effects for the Hang Glider.")
                .define("enableThermal", true);
        builder.pop();
        builder.push("crane");
        CRANE_COLLISIONS = builder.define("doCraneCollisionCheck", false);
        CRANE_SHIFT_DIRECTION = builder.comment("Hold sneak to lower the magnet. When false, each use reverses direction.").define("boringMode", true);
        TURTLE_MAGNET_ENABLED = builder.comment("Allow CC: Tweaked turtles to deploy a Miracle Magnet.").define("addTurtles", true);
        TURTLE_MAGNET_RANGE = builder.comment("Maximum target offset on each turtle axis.").defineInRange("turtleMagnetRange", 32.0D, 0.0D, 1024.0D);
        TURTLE_MAGNET_RECALL_RANGE = builder.comment("Maximum distance at which a turtle can deactivate its magnet.")
                .defineInRange("turtleMagnetDeactivateRange", 3.0D, 0.0D, 1024.0D);
        MAGNET_BLOCK_ALLOWLIST = builder.defineListAllowEmpty("magnetBlockWhitelist", List.of(), value -> value instanceof String);
        MAGNET_ENTITY_ALLOWLIST = builder.defineListAllowEmpty("magnetEntityWhitelist", List.of(), value -> value instanceof String);
        MAGNET_BLOCK_ENTITY_ALLOWLIST = builder.defineListAllowEmpty("magnetTileEntityWhitelist", List.of(), value -> value instanceof String);
        builder.pop();
        builder.push("magnet");
        MAGNET_PICK_ENTITIES = builder.define("pickEntities", true);
        MAGNET_PICK_BLOCKS = builder.define("pickBlocks", true);
        builder.pop();
        builder.push("cartographer");
        CARTOGRAPHER_BLACKLIST = builder.comment("Block registry IDs the Cartographer should see through.")
                .defineListAllowEmpty("blockBlacklist", List.of(), value -> value instanceof String);
        builder.pop();
        builder.push("guide");
        GUIDE_REDSTONE = builder.comment("Guide redstone mode. 1 requires power, -1 requires no power, and 0 ignores redstone.")
                .defineInRange("redstoneSensitivity", 1, -1, 1);
        GUIDE_MAX_HALF_AXIS = builder.comment("Maximum number of blocks a Guide can extend along each half-axis.")
                .defineInRange("maxHalfAxis", 64, 8, 128);
        builder.pop();

        builder.push("projector");
        PROJECTOR_LIGHT_ENABLED = builder.define("lightUpWhenWorking", true);
        PROJECTOR_LIGHT_LEVEL = builder.defineInRange("lightLevel", 10, 0, 15);
        builder.pop();

        builder.push("debug");
        GOLDEN_EYE_DEBUG = builder.comment("Show the structure ID and coordinates selected by the Golden Eye.").define("goldenEyeDebug", false);
        builder.pop();
        builder.push("trophy");
        TROPHY_DROP_CHANCE = builder.comment("Base trophy chance. Multiplied by the sum of Looting level and a random bias below 0.25.")
                .defineInRange("trophyDropChance", 0.001, 0.0, 1.0);
        builder.pop();
        builder.push("egg");
        GOLDEN_EGG_PICK_BLOCKS = builder.comment("The hatching Golden Egg can lift nearby blocks.").define("pickBlocks", true);
        builder.pop();
        builder.push("imaginary");
        IMAGINARY_USES = builder.comment("Placement charges in new creative inventory and pick-block pencils and crayons.")
                .defineInRange("numberOfUses", 10.0, 0.5, 1000000.0);
        builder.pop();
        builder.push("loot");
        TECHNICOLOR_LOOT = builder.comment("Add Technicolor Glasses to mineshaft and dungeon loot.")
                .define("technicolorGlasses", true);
        builder.pop();
        builder.push("canvas");
        CANVAS_REPLACE = builder.comment("Allow brushes to paint suitable blocks by wrapping their original block state.")
                .define("paintbrushReplacesBlocks", true);
        CANVAS_BLACKLIST = builder.comment("Block IDs that brush and stencil tools cannot wrap in a canvas.")
                .defineListAllowEmpty("replaceBlacklist", List.of(), value -> value instanceof String);
        builder.pop();
        builder.push("tomfoolery");
        SERIOUS_MODE = builder.comment("Disable Tasty Clay digestion and the Be Silly action.")
                .define("weAreSeriousPeople", true);
        FLIM_FLAM_BLACKLIST = builder.comment("Effect IDs to exclude from the Flim-Flam enchantment.")
                .defineListAllowEmpty("flimFlamBlacklist", List.of(), value -> value instanceof String);
        FLIM_FLAM_REVERSE_BLACKLIST = builder.comment("Use the effect list as an allowlist.").define("reverseBlacklist", false);
        FLIM_FLAM_SAFE_ONLY = builder.comment("Limit automatic Flim-Flam effects to the original safe group.").define("safeOnly", false);
        builder.pop();
        SPEC = builder.build();
    }

    private OBServerConfig() {}
}
