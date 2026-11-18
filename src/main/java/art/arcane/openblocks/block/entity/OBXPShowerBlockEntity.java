package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBXPShowerBlock;
import art.arcane.openblocks.entity.OBFallingExperienceOrb;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBFluids;
import art.arcane.openblocks.world.OBLiquidExperience;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public final class OBXPShowerBlockEntity extends BlockEntity {

    private static final int PULL_PER_CYCLE = 100;
    private static final int TICK_INTERVAL = 3;
    private static final String BUFFER_TAG = "buffer";
    private final FluidTank buffer = new FluidTank(FluidType.BUCKET_VOLUME, fluid -> {
        final int ratio = OBLiquidExperience.ratio(fluid);
        return ratio > 0 && ratio <= FluidType.BUCKET_VOLUME;
    }) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public OBXPShowerBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.XP_SHOWER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBXPShowerBlockEntity entity) {
        if (!state.getValue(OBXPShowerBlock.POWERED) || level.getGameTime() % TICK_INTERVAL != 0
                || !(level instanceof ServerLevel server)) return;
        final Direction facing = state.getValue(OBXPShowerBlock.FACING);
        OBLiquidExperience.pull(level, pos.relative(facing.getOpposite()), facing, entity.buffer, PULL_PER_CYCLE);
        final int ratio = OBLiquidExperience.ratio(entity.buffer.getFluid());
        if (ratio == 0 || entity.buffer.getFluidAmount() < ratio) return;

        final int points = ExperienceOrb.getExperienceValue(entity.buffer.getFluidAmount() / ratio);
        final Vec3 nozzle = Vec3.atBottomCenterOf(pos);
        final var orb = new OBFallingExperienceOrb(server, nozzle.add(0.0, 0.1, 0.0), points);
        if (!server.addFreshEntity(orb)) return;
        entity.buffer.drain(points * ratio, FluidAction.EXECUTE);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, OBFluids.XP_JUICE_BLOCK.get().defaultBlockState()),
                nozzle.x, nozzle.y + 0.4, nozzle.z, 2, 0.04, 0.01, 0.04, 0.02);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        if (!buffer.isEmpty()) tag.put(BUFFER_TAG, buffer.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        buffer.readFromNBT(tag.getCompound(BUFFER_TAG));
    }
}
