package art.arcane.openblocks.item;

import art.arcane.openblocks.block.OBFlagBlock;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class OBFlagItem extends BlockItem {

    public OBFlagItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    public static DyeColor getColor(final ItemStack stack) {
        final var state = stack.getTagElement(BLOCK_STATE_TAG);
        return state == null ? OBFlagBlock.DEFAULT_COLOR : DyeColor.byName(state.getString("color"), OBFlagBlock.DEFAULT_COLOR);
    }

    public static ItemStack create(final DyeColor color) {
        final ItemStack item = new ItemStack(OBItems.FLAG.get());
        setColor(item, color);
        return item;
    }

    private static void setColor(final ItemStack stack, final DyeColor color) {
        if (color != OBFlagBlock.DEFAULT_COLOR) {
            stack.getOrCreateTagElement(BLOCK_STATE_TAG).putString("color", color.getName());
            return;
        }
        final var state = stack.getTagElement(BLOCK_STATE_TAG);
        if (state == null) return;
        state.remove("color");
        if (state.isEmpty()) stack.removeTagKey(BLOCK_STATE_TAG);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        return context.isSecondaryUseActive() ? InteractionResult.PASS : super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        setColor(stack, DyeColor.byId((getColor(stack).getId() + 1) % DyeColor.values().length));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
