package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public class OBHealBlockEntity extends BlockEntity {

    private static final long TICK_INTERVAL = 20L;
    private static final double HORIZONTAL_RANGE = 1.0D;
    private static final double VERTICAL_RANGE = 2.0D;

    public OBHealBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.HEAL.get(), pos, state);
    }

    public static void serverTick(
            final Level level,
            final BlockPos pos,
            final BlockState state,
            final OBHealBlockEntity blockEntity) {
        if ((level.getGameTime() % TICK_INTERVAL) != 0L) return;

        final AABB effectBounds = new AABB(pos).inflate(HORIZONTAL_RANGE, VERTICAL_RANGE, HORIZONTAL_RANGE);
        for (final Player player : level.getEntitiesOfClass(Player.class, effectBounds, player -> !player.isCreative())) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2, 10, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 0, false, false, false));
        }
    }
}
