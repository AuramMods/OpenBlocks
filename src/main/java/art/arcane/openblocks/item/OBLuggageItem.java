package art.arcane.openblocks.item;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBLuggage;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBLuggageItem extends Item {

    public OBLuggageItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);

        final OBLuggage luggage = OBEntities.LUGGAGE.get().create(server);
        if (luggage == null) return InteractionResultHolder.fail(stack);
        final var look = player.getLookAngle();
        luggage.moveTo(player.getX() + look.x * 2.0 + 0.5, player.getY(), player.getZ() + look.z * 2.0 + 0.5, 0, 0);
        luggage.setOwnerUUID(player.getUUID());
        luggage.unpack(stack);
        if (!server.noCollision(luggage) || !server.addFreshEntity(luggage)) return InteractionResultHolder.fail(stack);

        // The empty singleton bypasses vanilla's creative count restoration. Old saves may still have stacks.
        if (stack.getCount() == 1) return InteractionResultHolder.consume(ItemStack.EMPTY);
        if (player.getAbilities().instabuild) {
            player.setItemInHand(hand, stack.copyWithCount(stack.getCount() - 1));
        } else {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(final ItemStack stack) {
        return OBLuggage.isChargedItem(stack);
    }

    @SubscribeEvent
    public static void protectPackedLuggage(final EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity item
                && item.getItem().is(OBItems.LUGGAGE.get()) && item.getItem().hasTag()) {
            item.setInvulnerable(true);
            item.setUnlimitedLifetime();
        }
    }
}
