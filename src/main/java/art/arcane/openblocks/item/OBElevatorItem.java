package art.arcane.openblocks.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public final class OBElevatorItem extends BlockItem {

    public OBElevatorItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    public static DyeColor getColor(final ItemStack stack) {
        final var properties = stack.getTagElement(BLOCK_STATE_TAG);
        return properties == null ? DyeColor.WHITE : DyeColor.byName(properties.getString("color"), DyeColor.WHITE);
    }

    public static ItemStack create(final ItemLike item, final DyeColor color) {
        final ItemStack stack = new ItemStack(item);
        if (color != DyeColor.WHITE) stack.getOrCreateTagElement(BLOCK_STATE_TAG).putString("color", color.getName());
        return stack;
    }

    @Override
    public Component getName(final ItemStack stack) {
        return Component.translatable("item.open_blocks.colored_elevator",
                Component.translatable(getDescriptionId(stack)),
                Component.translatable("color.minecraft." + getColor(stack).getName()));
    }
}
