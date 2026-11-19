package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.enchantment.OBArmorEnchantment;
import art.arcane.openblocks.enchantment.OBFlimFlamEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, OpenBlocks.MODID);

    public static final RegistryObject<Enchantment> EXPLOSIVE = ENCHANTMENTS.register("explosive",
            () -> new OBArmorEnchantment(Enchantment.Rarity.RARE, 3, OBServerConfig.UNSTABLE_ENABLED));
    public static final RegistryObject<Enchantment> LAST_STAND = ENCHANTMENTS.register("last_stand",
            () -> new OBArmorEnchantment(Enchantment.Rarity.UNCOMMON, 2, OBServerConfig.LAST_STAND_ENABLED));
    public static final RegistryObject<Enchantment> FLIM_FLAM = ENCHANTMENTS.register("flim_flam", OBFlimFlamEnchantment::new);

    private OBEnchantments() {}

}
