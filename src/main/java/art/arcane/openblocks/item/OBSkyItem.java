package art.arcane.openblocks.item;

import art.arcane.openblocks.block.OBSkyBlock;
import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class OBSkyItem extends BlockItem {

    public OBSkyItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    public static boolean isInverted(final ItemStack stack) {
        final var state = stack.getTagElement(BLOCK_STATE_TAG);
        return state != null && OBSkyBlock.INVERTED.getName(true).equals(state.getString(OBSkyBlock.INVERTED.getName()));
    }

    public static ItemStack create(final boolean inverted) {
        final ItemStack stack = new ItemStack(OBItems.SKY.get());
        if (inverted) {
            stack.getOrCreateTagElement(BLOCK_STATE_TAG).putString(OBSkyBlock.INVERTED.getName(), OBSkyBlock.INVERTED.getName(true));
        }
        return stack;
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip,
            final TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (isInverted(stack)) tooltip.add(Component.translatable("tooltip.open_blocks.sky.inverted"));
    }
}
