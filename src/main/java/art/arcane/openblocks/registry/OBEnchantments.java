package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, OpenBlocks.MODID);

    public static final RegistryObject<Enchantment> EXPLOSIVE = register("explosive", 3, EnchantmentCategory.DIGGER, EquipmentSlot.MAINHAND);
    public static final RegistryObject<Enchantment> LAST_STAND = register("last_stand", 2, EnchantmentCategory.ARMOR,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public static final RegistryObject<Enchantment> FLIM_FLAM = register("flim_flam", 4, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND);

    private OBEnchantments() {}

    private static RegistryObject<Enchantment> register(final String id, final int maxLevel, final EnchantmentCategory category, final EquipmentSlot... slots) {
        return ENCHANTMENTS.register(id, () -> new OBPlaceholderEnchantment(maxLevel, category, slots));
    }

    private static final class OBPlaceholderEnchantment extends Enchantment {
        private final int maxLevel;

        private OBPlaceholderEnchantment(final int maxLevel, final EnchantmentCategory category, final EquipmentSlot... slots) {
            super(Rarity.UNCOMMON, category, slots);
            this.maxLevel = maxLevel;
        }

        @Override
        public int getMaxLevel() {
            return maxLevel;
        }
    }
}
