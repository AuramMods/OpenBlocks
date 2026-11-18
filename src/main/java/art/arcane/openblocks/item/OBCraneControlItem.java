package art.arcane.openblocks.item;

import art.arcane.openblocks.entity.OBMagnet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class OBCraneControlItem extends Item {

    public OBCraneControlItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!OBMagnet.wearsBackpack(player)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            final OBMagnet magnet = OBMagnet.ensureFor(serverPlayer);
            if (magnet != null) magnet.startWinch(serverPlayer.isShiftKeyDown());
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(final ItemStack stack) {
        return 72_000;
    }

    @Override
    public boolean onEntitySwing(final ItemStack stack, final LivingEntity entity) {
        if (entity instanceof ServerPlayer player && OBMagnet.wearsBackpack(player)) {
            final OBMagnet magnet = OBMagnet.ensureFor(player);
            if (magnet != null) magnet.toggleFromControl(player);
        }
        return true;
    }

    @Override
    public boolean onLeftClickEntity(final ItemStack stack, final Player player, final Entity target) {
        return true;
    }

    @Override
    public boolean canAttackBlock(final BlockState state, final Level level, final BlockPos pos, final Player player) {
        return false;
    }
}
