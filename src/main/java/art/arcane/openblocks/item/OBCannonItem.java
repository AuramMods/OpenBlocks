package art.arcane.openblocks.item;

import art.arcane.openblocks.client.OBCannonRendering;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class OBCannonItem extends BlockItem {

    public OBCannonItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new OBCannonRendering.ItemRenderer();
                return renderer;
            }
        });
    }
}
