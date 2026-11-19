package art.arcane.openblocks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class OBEpicEraserItem extends Item {

    public OBEpicEraserItem() {
        super(new Properties().durability(15));
    }

    @Override public boolean isFoil(final ItemStack stack) { return true; }

    @Override
    public boolean hasCraftingRemainingItem(final ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    @Override
    public ItemStack getCraftingRemainingItem(final ItemStack stack) {
        if (!hasCraftingRemainingItem(stack)) return ItemStack.EMPTY;
        final ItemStack remainder = stack.copyWithCount(1);
        remainder.setDamageValue(stack.getDamageValue() + 1);
        return remainder;
    }
}
