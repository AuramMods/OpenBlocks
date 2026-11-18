package art.arcane.openblocks.item;

import art.arcane.openblocks.entity.OBHangGlider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class OBHangGliderItem extends Item {

    public OBHangGliderItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) OBHangGlider.toggle(serverPlayer, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
