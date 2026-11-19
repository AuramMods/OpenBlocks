package art.arcane.openblocks.item;

import art.arcane.openblocks.registry.OBEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class OBCartographerItem extends Item {

    public OBCartographerItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);

        final var assistant = OBEntities.CARTOGRAPHER.get().create(server);
        if (assistant == null) return InteractionResultHolder.fail(stack);
        assistant.deploy(player, stack);
        if (!server.noCollision(assistant) || !server.addFreshEntity(assistant)) return InteractionResultHolder.fail(stack);

        if (stack.getCount() == 1) return InteractionResultHolder.consume(ItemStack.EMPTY);
        final ItemStack remaining = stack.copyWithCount(stack.getCount() - 1);
        player.setItemInHand(hand, remaining);
        return InteractionResultHolder.consume(remaining);
    }
}
