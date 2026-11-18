package art.arcane.openblocks.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OBClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue RENDER_SKY_BLOCKS;
    public static final ForgeConfigSpec.IntValue DEV_NULL_COUNT_COLOR;

    static {
        final var builder = new ForgeConfigSpec.Builder();
        builder.push("skyblock");
        RENDER_SKY_BLOCKS = builder.comment("Show the current sky on active Sky Blocks. Disable if another renderer is incompatible.")
                .define("renderingEnabled", true);
        builder.pop();
        builder.push("devnull");
        DEV_NULL_COUNT_COLOR = builder.comment("RGB color of the /dev/null contents count in the inventory.")
                .defineInRange("countColor", 0xFFFF00, 0, 0xFFFFFF);
        builder.pop();
        SPEC = builder.build();
    }

    private OBClientConfig() {}
}
