package art.arcane.openblocks.item;

import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBHeightMapData;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class OBEmptyMapItem extends Item {

    public OBEmptyMapItem() {
        super(new Properties());
    }

    public static int scale(final ItemStack stack) {
        final var tag = stack.getTag();
        return tag == null ? 0 : Mth.clamp(tag.getByte("Scale"), 0, OBHeightMapData.MAX_SCALE);
    }

    public static ItemStack create(final int scale) {
        final ItemStack stack = new ItemStack(OBItems.EMPTY_MAP.get());
        setScale(stack, scale);
        return stack;
    }

    public static void setScale(final ItemStack stack, final int scale) {
        if (scale == 0) stack.removeTagKey("Scale");
        else stack.getOrCreateTag().putByte("Scale", (byte) scale);
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip, final TooltipFlag flags) {
        tooltip.add(Component.translatable("item.open_blocks.map.scale", 1 << scale(stack)));
    }
}
