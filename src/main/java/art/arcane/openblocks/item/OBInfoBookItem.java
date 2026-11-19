package art.arcane.openblocks.item;

import art.arcane.openblocks.client.OBInfoBookScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class OBInfoBookItem extends Item {

    public OBInfoBookItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OBInfoBookScreen::open);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
