package art.arcane.openblocks.world;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidTank;
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

    public static int bottleCapacity() {
        if (!OBServerConfig.SPEC.isLoaded()) return XP_PER_BOTTLE * OBServerConfig.XP_FLUID_RATIO.getDefault();
        int maxRatio = OBServerConfig.XP_FLUID_RATIO.get();
        for (final var fluid : ForgeRegistries.FLUIDS) {
            maxRatio = Math.max(maxRatio, ratio(new FluidStack(fluid, 1)));
        }
        return maxRatio * XP_PER_BOTTLE;
    }

    public static int pull(final Level level, final BlockPos pos, final Direction side, final FluidTank receiver, final int limit) {
        if (receiver.getSpace() <= 0 || !level.hasChunkAt(pos)) return 0;
        final var source = FluidUtil.getFluidHandler(level, pos, side).orElse(null);
        if (source == null) return 0;
        final int amount = Math.min(limit, receiver.getSpace());
        if (!receiver.isEmpty()) {
            return FluidUtil.tryFluidTransfer(receiver, source, new FluidStack(receiver.getFluid(), amount), true).getAmount();
        }
        for (int index = 0; index < source.getTanks(); index++) {
            final FluidStack candidate = source.getFluidInTank(index);
            if (candidate.isEmpty() || !receiver.isFluidValid(candidate)) continue;
            final FluidStack moved = FluidUtil.tryFluidTransfer(receiver, source, new FluidStack(candidate, amount), true);
            if (!moved.isEmpty()) return moved.getAmount();
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
