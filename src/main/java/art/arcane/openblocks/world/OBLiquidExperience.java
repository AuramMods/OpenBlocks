package art.arcane.openblocks.world;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.math.NumberUtils;

public final class OBLiquidExperience {

    public static final int XP_PER_BOTTLE = 8;

    private OBLiquidExperience() {}

    public static FluidStack juice(final int points) {
        return new FluidStack(OBFluids.XP_JUICE.get(), millibuckets(points, OBServerConfig.XP_FLUID_RATIO.get()));
    }

    public static void collectOrb(final IFluidHandler receiver, final ExperienceOrb orb, final int ratio) {
        final long unit = (long) orb.getValue() * ratio;
        if (orb.isRemoved() || unit <= 0 || unit > Integer.MAX_VALUE) return;
        final int fit = unitsThatFit(receiver, (int) unit, orb.count);
        final int stored = storeUnits(receiver, (int) unit, fit);
        if (stored <= 0) return;
        orb.count -= stored;
        if (orb.count == 0) orb.discard();
    }

    public static int unitsThatFit(final IFluidHandler receiver, final int millibucketsPerUnit, final int offeredUnits) {
        if (offeredUnits <= 0) return 0;
        final int amount = Math.min(offeredUnits, Integer.MAX_VALUE / millibucketsPerUnit) * millibucketsPerUnit;
        return receiver.fill(new FluidStack(OBFluids.XP_JUICE.get(), amount), FluidAction.SIMULATE) / millibucketsPerUnit;
    }

    public static int storeUnits(final IFluidHandler receiver, final int millibucketsPerUnit, final int units) {
        if (units <= 0) return 0;
        final var fluid = new FluidStack(OBFluids.XP_JUICE.get(), units * millibucketsPerUnit);
        final int stored = receiver.fill(fluid, FluidAction.EXECUTE);
        final int remainder = stored % millibucketsPerUnit;
        if (remainder > 0) receiver.drain(new FluidStack(fluid, remainder), FluidAction.EXECUTE);
        return stored / millibucketsPerUnit;
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
        return capacityForPoints(XP_PER_BOTTLE);
    }

    public static int capacityForPoints(final int points) {
        if (!OBServerConfig.SPEC.isLoaded()) return millibuckets(points, OBServerConfig.XP_FLUID_RATIO.getDefault());
        int maxRatio = OBServerConfig.XP_FLUID_RATIO.get();
        for (final var fluid : ForgeRegistries.FLUIDS) {
            maxRatio = Math.max(maxRatio, ratio(new FluidStack(fluid, 1)));
        }
        return millibuckets(points, maxRatio);
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
        return (int) Math.min(Integer.MAX_VALUE, (long) pointsAtLevel(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
    }

    public static int pointsAtLevel(final long level) {
        if (level >= 32768) return Integer.MAX_VALUE;
        if (level <= 0) return 0;
        final long atLevel;
        if (level >= 32) {
            atLevel = (9 * level * level - 325 * level + 4440) / 2;
        } else if (level >= 17) {
            atLevel = (5 * level * level - 81 * level + 720) / 2;
        } else {
            atLevel = level * level + 6 * level;
        }
        return (int) Math.min(Integer.MAX_VALUE, atLevel);
    }
}
