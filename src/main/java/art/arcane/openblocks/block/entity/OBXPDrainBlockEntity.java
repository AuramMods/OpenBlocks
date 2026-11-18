package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.world.OBLiquidExperience;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class OBXPDrainBlockEntity extends BlockEntity {

    private static final int PLAYER_XP_PER_TICK = 4;
    private final AABB playerArea;
    private final AABB orbArea;

    public OBXPDrainBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.XP_DRAIN.get(), pos, state);
        playerArea = new AABB(pos);
        orbArea = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 0.3, pos.getZ() + 1.0);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBXPDrainBlockEntity entity) {
        final var receiver = FluidUtil.getFluidHandler(level, pos.below(), Direction.UP).orElse(null);
        if (receiver == null) return;
        final int ratio = OBServerConfig.XP_FLUID_RATIO.get();
        boolean transferred = false;
        for (final ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, entity.orbArea, orb -> !orb.isRemoved())) {
            OBLiquidExperience.collectOrb(receiver, orb, ratio);
        }
        for (final Player player : level.getEntitiesOfClass(Player.class, entity.playerArea, player -> player.isAlive() && !player.isSpectator())) {
            transferred |= transferPlayer(receiver, player, ratio);
        }
        if (transferred && level.getGameTime() % 4 == 0) {
            final float pitch = 0.5F * ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.8F);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.1F, pitch);
        }
    }

    private static boolean transferPlayer(final IFluidHandler receiver, final Player player, final int ratio) {
        final int offered = Math.min(PLAYER_XP_PER_TICK, OBLiquidExperience.playerPoints(player));
        final int fit = OBLiquidExperience.unitsThatFit(receiver, ratio, offered);
        if (fit <= 0) return false;
        final var event = new PlayerXpEvent.XpChange(player, -fit);
        if (MinecraftForge.EVENT_BUS.post(event) || event.getAmount() >= 0) return false;

        final int agreed = (int) Math.min(fit, -(long) event.getAmount());
        final int available = Math.min(agreed, OBLiquidExperience.playerPoints(player));
        final int transferred = OBLiquidExperience.storeUnits(receiver, ratio, OBLiquidExperience.unitsThatFit(receiver, ratio, available));
        if (transferred <= 0) return false;
        spendPoints(player, transferred);
        return true;
    }

    private static void spendPoints(final Player player, final int cost) {
        // A tick can cross at most one level, since the smallest XP bar holds seven points.
        int remaining = Math.round(player.experienceProgress * player.getXpNeededForNextLevel()) - cost;
        if (remaining < 0 && player.experienceLevel > 0) {
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.setExperienceLevels(player.experienceLevel - 1);
            else player.experienceLevel--;
            remaining += player.getXpNeededForNextLevel();
        }
        player.totalExperience = Math.max(0, player.totalExperience - cost);
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.setExperiencePoints(Math.max(0, remaining));
        else player.experienceProgress = (float) Math.max(0, remaining) / player.getXpNeededForNextLevel();
    }
}
