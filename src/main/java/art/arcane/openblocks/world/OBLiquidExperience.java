package art.arcane.openblocks.world;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBFluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.math.NumberUtils;

public final class OBLiquidExperience {

    public static final int XP_PER_BOTTLE = 8;

    private OBLiquidExperience() {}

    public static FluidStack juice(final int points) {
        return new FluidStack(OBFluids.XP_JUICE.get(), millibuckets(points, OBServerConfig.XP_FLUID_RATIO.get()));
    }

    public static int millibuckets(final int points, final int ratio) {
        return (int) Math.min(Integer.MAX_VALUE, (long) points * ratio);
    }

    public static int ratio(final FluidStack fluid) {
        if (fluid.isEmpty() || fluid.hasTag()) return 0;
        if (fluid.getFluid().isSame(OBFluids.XP_JUICE.get())) return OBServerConfig.XP_FLUID_RATIO.get();
        final ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        if (id == null) return 0;

        for (final String entry : OBServerConfig.XP_ADDITIONAL_FLUIDS.get()) {
            final int split = entry.contains("=") ? entry.lastIndexOf('=') : entry.lastIndexOf(':');
            if (split <= 0) continue;
            final String fluidName = entry.substring(0, split).trim();
            if (!fluidName.equals(fluidName.contains(":") ? id.toString() : id.getPath())) continue;
            final int configured = NumberUtils.toInt(entry.substring(split + 1).trim());
            if (configured > 0 && configured <= Integer.MAX_VALUE / XP_PER_BOTTLE) return configured;
        }
        return 0;
    }

    public static int playerPoints(final Player player) {
        final long level = player.experienceLevel;
        if (level >= 32768) return Integer.MAX_VALUE;
        final long atLevel;
        if (level >= 32) {
            atLevel = (9 * level * level - 325 * level + 4440) / 2;
        } else if (level >= 17) {
            atLevel = (5 * level * level - 81 * level + 720) / 2;
        } else {
            atLevel = level * level + 6 * level;
        }
        return (int) Math.min(Integer.MAX_VALUE, atLevel + Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
    }
}
