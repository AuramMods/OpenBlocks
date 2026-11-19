package art.arcane.openblocks.enchantment;

import art.arcane.openblocks.config.OBServerConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

public final class OBArmorEnchantment extends Enchantment {

    private final int maxLevel;
    private final BooleanValue enabled;

    public OBArmorEnchantment(final Rarity rarity, final int maxLevel, final BooleanValue enabled) {
        super(rarity, EnchantmentCategory.ARMOR, new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
        this.maxLevel = maxLevel;
        this.enabled = enabled;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public int getMinCost(final int level) {
        return switch (level) {
            case 1 -> 15;
            case 2 -> 25;
            default -> 100;
        };
    }

    @Override
    public int getMaxCost(final int level) {
        return getMinCost(level) + 10;
    }

    @Override
    public boolean isDiscoverable() {
        return !OBServerConfig.SPEC.isLoaded() || enabled.get();
    }

    @Override
    public boolean isTradeable() {
        return isDiscoverable();
    }
}
