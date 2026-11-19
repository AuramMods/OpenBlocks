package art.arcane.openblocks.item;

import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.world.OBHeightMapData;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class OBHeightMapItem extends Item {

    public OBHeightMapItem() {
        super(new Properties());
    }

    public static int mapId(final ItemStack stack) {
        final var tag = stack.getTag();
        return stack.is(OBItems.HEIGHT_MAP.get()) && tag != null && tag.contains("MapId", Tag.TAG_INT) ? tag.getInt("MapId") : -1;
    }

    public static int knownScale(final ItemStack stack) {
        final var tag = stack.getTag();
        return tag != null && tag.contains("Survey", Tag.TAG_LONG) ? OBEmptyMapItem.scale(stack) : -1;
    }

    public static ItemStack create(final ItemStack source, final int id, final OBHeightMapData map) {
        final ItemStack result = new ItemStack(OBItems.HEIGHT_MAP.get());
        if (source.hasTag()) result.setTag(source.getTag().copy());
        result.getOrCreateTag().putInt("MapId", id);
        updateMetadata(result, map);
        return result;
    }

    public static void updateMetadata(final ItemStack stack, final OBHeightMapData map) {
        final var tag = stack.getOrCreateTag();
        tag.putLong("Survey", map.survey());
        tag.putByte("Scale", (byte) map.scale());
        tag.putInt("CenterX", map.centerX());
        tag.putInt("CenterZ", map.centerZ());
        tag.putString("Dimension", map.dimension().toString());
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity, final int slot, final boolean selected) {
        if (!(level instanceof ServerLevel server)) return;
        final OBHeightMapData map = OBHeightMapData.get(server, mapId(stack));
        if (map != null && stack.getOrCreateTag().getLong("Survey") != map.survey()) updateMetadata(stack, map);
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip, final TooltipFlag flags) {
        final var tag = stack.getTag();
        if (tag == null || mapId(stack) < 0 || knownScale(stack) < 0) return;
        tooltip.add(Component.translatable("item.open_blocks.map.center", tag.getInt("CenterX"), tag.getInt("CenterZ")));
        tooltip.add(Component.translatable("item.open_blocks.map.scale", 1 << knownScale(stack)));
    }
}
