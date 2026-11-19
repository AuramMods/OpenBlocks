package art.arcane.openblocks.item;

import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class OBPaintCanItem extends BlockItem {

    public static final int FULL = 30;
    private static final String COLOR = "color";
    private static final String AMOUNT = "amount";

    public OBPaintCanItem(final Block block) {
        super(block, new Properties().stacksTo(1));
    }

    public static int color(final ItemStack stack) {
        final var tag = stack.getTag();
        return tag != null && tag.contains(COLOR, Tag.TAG_ANY_NUMERIC) ? tag.getInt(COLOR) & 0xFFFFFF : OBPaintColors.WHITE;
    }

    public static int amount(final ItemStack stack) {
        final var tag = stack.getTag();
        return tag == null ? 0 : Mth.clamp(tag.getInt(AMOUNT), 0, FULL);
    }

    public static ItemStack create(final int color, final int amount) {
        final var stack = new ItemStack(OBItems.PAINT_CAN.get());
        final var tag = stack.getOrCreateTag();
        tag.putInt(COLOR, color & 0xFFFFFF);
        tag.putInt(AMOUNT, Mth.clamp(amount, 0, FULL));
        return stack;
    }

    @Override
    public boolean isBarVisible(final ItemStack stack) {
        return stack.hasTag() && amount(stack) < FULL;
    }

    @Override
    public int getBarWidth(final ItemStack stack) {
        return Math.round(13F * amount(stack) / FULL);
    }

    @Override
    public int getBarColor(final ItemStack stack) {
        return color(stack);
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> lines, final TooltipFlag flags) {
        lines.add(Component.literal(String.format(Locale.ROOT, "#%06X", color(stack))));
    }
}
