package art.arcane.openblocks.enchantment;

import art.arcane.openblocks.config.OBServerConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class OBFlimFlamEnchantment extends Enchantment {

    private static final EnchantmentCategory EQUIPMENT = EnchantmentCategory.create("open_blocks_flim_flam",
            item -> item instanceof ArmorItem || item instanceof SwordItem);

    public OBFlimFlamEnchantment() {
        super(Rarity.RARE, EQUIPMENT, new EquipmentSlot[] { EquipmentSlot.MAINHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET });
    }

    @Override public int getMaxLevel() { return 4; }
    @Override public int getMinCost(final int level) { return 31 + level * 10; }
    @Override public int getMaxCost(final int level) { return getMinCost(level) + 10; }
    @Override public boolean isDiscoverable() { return !OBServerConfig.SPEC.isLoaded() || OBServerConfig.FLIM_FLAM_ENABLED.get(); }
    @Override public boolean isTradeable() { return isDiscoverable(); }
}
