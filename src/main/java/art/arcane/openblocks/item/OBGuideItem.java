package art.arcane.openblocks.item;

import art.arcane.openblocks.block.entity.OBGuideBlockEntity;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class OBGuideItem extends BlockItem {

    public OBGuideItem(final Block block) { super(block, new Properties()); }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> lines, final TooltipFlag flags) {
        final var tag = stack.getTag();
        if (tag == null || !tag.contains("Mode") && !tag.contains("PosX", Tag.TAG_ANY_NUMERIC) && !tag.contains("Width", Tag.TAG_ANY_NUMERIC)) return;
        final var bounds = OBGuideBlockEntity.readBounds(tag);
        lines.add(Component.translatable("guide.open_blocks.bounds", -bounds.negX(), -bounds.negY(), -bounds.negZ(),
                bounds.posX(), bounds.posY(), bounds.posZ()));
        lines.add(Component.translatable("guide.open_blocks.selected", OBGuideBlockEntity.readShape(tag).label()));
        if (tag.contains("Color", Tag.TAG_ANY_NUMERIC)) {
            lines.add(Component.translatable("guide.open_blocks.color", String.format(Locale.ROOT, "#%06X", tag.getInt("Color") & 0xFFFFFF)));
        }
    }
}
