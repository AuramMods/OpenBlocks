package art.arcane.openblocks.item;

import art.arcane.openblocks.client.OBTrophyRendering;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.trophy.OBTrophyType;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class OBTrophyItem extends BlockItem {

    public OBTrophyItem(final Block block) {
        super(block, new Properties());
    }

    public static ItemStack create(final OBTrophyType type, final int cooldown) {
        final var stack = new ItemStack(OBItems.TROPHY.get());
        final var tag = stack.getOrCreateTag();
        tag.putString(OBTrophyType.ENTITY_ID, type.id().toString());
        if (cooldown > 0) tag.putInt(OBTrophyType.COOLDOWN, cooldown);
        return stack;
    }

    @Override
    public Component getName(final ItemStack stack) {
        final var type = OBTrophyType.read(stack.getTag());
        return type == null ? super.getName(stack)
                : Component.translatable("block.open_blocks.trophy.entity", type.entityType().getDescription());
    }

    @Override
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new OBTrophyRendering.ItemRenderer();
                return renderer;
            }
        });
    }
}
