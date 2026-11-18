package art.arcane.openblocks.item;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.client.OBCraneRendering;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class OBCraneBackpackItem extends ArmorItem {

    public OBCraneBackpackItem() {
        super(ArmorMaterials.IRON, Type.CHESTPLATE, new Properties());
    }

    @Override
    public String getArmorTexture(final ItemStack stack, final Entity entity, final EquipmentSlot slot, final String type) {
        return OpenBlocks.id("textures/model/crane.png").toString();
    }

    @Override
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(OBCraneRendering.backpackExtension());
    }
}
