package art.arcane.openblocks.item;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.world.OBSpongeDrain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class OBSpongeOnAStickItem extends Item {

    public OBSpongeOnAStickItem() {
        super(new Item.Properties().durability(OBServerConfig.SPONGE_STICK_DURABILITY.getDefault()));
    }

    @Override
    public int getMaxDamage(final ItemStack stack) {
        return OBServerConfig.SPEC.isLoaded() ? OBServerConfig.SPONGE_STICK_DURABILITY.get() : super.getMaxDamage(stack);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return soak(context.getLevel(), context.getClickedPos(), player, context.getHand(), context.getItemInHand());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        return new InteractionResultHolder<>(soak(level, player.blockPosition(), player, hand, stack), stack);
    }

    private static InteractionResult soak(final Level level, final BlockPos origin,
            final Player player, final InteractionHand hand, final ItemStack stack) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        final var result = OBSpongeDrain.drain(server, origin, OBServerConfig.SPONGE_STICK_RANGE.get(),
                OBServerConfig.SPONGE_STICK_BLOCK_UPDATE.get(), player);
        if (!result.absorbed()) return InteractionResult.PASS;

        if (result.lava()) {
            player.broadcastBreakEvent(hand);
            stack.shrink(1);
            player.setSecondsOnFire(6);
        } else {
            stack.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(hand));
        }
        return InteractionResult.CONSUME;
    }
}
