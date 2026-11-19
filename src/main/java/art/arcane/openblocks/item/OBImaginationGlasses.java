package art.arcane.openblocks.item;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class OBImaginationGlasses extends ArmorItem {

    public enum Kind {
        PENCIL("pencil"), CRAYON("crayon"), TECHNICOLOR("technicolor"), SERIOUS("admin");

        private final String texture;

        Kind(final String name) {
            texture = OpenBlocks.id("textures/model/glasses_" + name + ".png").toString();
        }
    }

    private final Kind kind;

    public OBImaginationGlasses(final Kind kind) {
        super(ArmorMaterials.GOLD, Type.HELMET, new Properties());
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    public static ItemStack createCrayonGlasses(final int color) {
        final var result = new ItemStack(OBItems.CRAYON_GLASSES.get());
        result.getOrCreateTag().putInt("Color", color & 0xFFFFFF);
        return result;
    }

    public static int color(final ItemStack stack) {
        final var tag = stack.getTag();
        return tag == null ? 0 : tag.getInt("Color") & 0xFFFFFF;
    }

    public static Component colorDescription(final int rgb) {
        return Component.translatable("item.open_blocks.imaginary.color", String.format(Locale.ROOT, "%06X", rgb));
    }

    @Nullable
    @Override
    public String getArmorTexture(final ItemStack stack, final Entity entity, final EquipmentSlot slot, @Nullable final String type) {
        return kind.texture;
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> lines, final TooltipFlag flag) {
        if (kind == Kind.CRAYON) lines.add(colorDescription(color(stack)));
    }
}
