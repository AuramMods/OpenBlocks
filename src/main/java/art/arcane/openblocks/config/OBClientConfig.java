package art.arcane.openblocks.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OBClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue RENDER_SKY_BLOCKS;
    public static final ForgeConfigSpec.IntValue DEV_NULL_COUNT_COLOR;
    public static final ForgeConfigSpec.BooleanValue SERIOUS_MODE;
    public static final ForgeConfigSpec.BooleanValue BOO_WHILE_TYPING;
    public static final ForgeConfigSpec.BooleanValue PROJECTOR_CONE;
    public static final ForgeConfigSpec.IntValue PROJECTOR_BRIGHTNESS;
    public static final ForgeConfigSpec.BooleanValue GLYPHS_IN_SEARCH;
    public static final ForgeConfigSpec.DoubleValue GUIDE_RENDER_DISTANCE_SQ;
    public static final ForgeConfigSpec.DoubleValue SONIC_GLASSES_OPACITY;
    public static final ForgeConfigSpec.BooleanValue SONIC_GLASSES_USE_TEXTURE;

    public static final ForgeConfigSpec.DoubleValue IMAGINARY_FADE;

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
        builder.push("tomfoolery");
        SERIOUS_MODE = builder.comment("Disable the Be Silly key on this client.").define("weAreSeriousPeople", true);
        BOO_WHILE_TYPING = builder.comment("Allow the Be Silly key to activate while a screen is open.")
                .define("doItWhileTyping", false);
        builder.pop();
        builder.push("projector");
        PROJECTOR_CONE = builder.define("renderHolographicCone", true);
        PROJECTOR_BRIGHTNESS = builder.comment("Brightness of the holographic cone. Use -1 for world lighting.")
                .defineInRange("brightness", -1, -1, 255);
        builder.pop();
        builder.push("guide");
        GUIDE_RENDER_DISTANCE_SQ = builder.comment("Squared distance at which Guide markers can render.")
                .defineInRange("renderDistanceSq", 65536.0, 1.0, 262144.0);
        builder.pop();
        builder.push("glasses");
        SONIC_GLASSES_OPACITY = builder.comment("World dimming while wearing Sonic Glasses. Set to 0 for a clear view.")
                .defineInRange("opacity", 0.7, 0.0, 1.0);
        SONIC_GLASSES_USE_TEXTURE = builder.comment("Use the original obsidian texture when dimming the world.")
                .define("useTexture", true);
        builder.pop();
        builder.push("imaginary");
        IMAGINARY_FADE = builder.comment("Visibility change per 1/60 second when imaginary blocks appear or fade.")
                .defineInRange("fadingSpeed", 0.0075, 0.0001, 1.0);
        builder.pop();
        builder.push("glyphs");
        GLYPHS_IN_SEARCH = builder.comment("Include every printable glyph in the creative search tab.").define("showInCreativeSearch", false);
        builder.pop();
        SPEC = builder.build();
    }

    private OBClientConfig() {}
}
