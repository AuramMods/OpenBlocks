package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, OpenBlocks.MODID);

    public static final RegistryObject<MenuType<ChestMenu>> AUTO_ANVIL = register("auto_anvil");
    public static final RegistryObject<MenuType<ChestMenu>> AUTO_ENCHANTMENT_TABLE = register("auto_enchantment_table");
    public static final RegistryObject<MenuType<ChestMenu>> BIG_BUTTON = register("big_button");
    public static final RegistryObject<MenuType<ChestMenu>> BLOCK_PLACER = register("block_placer");
    public static final RegistryObject<MenuType<ChestMenu>> DEV_NULL = register("dev_null");
    public static final RegistryObject<MenuType<ChestMenu>> DONATION_STATION = register("donation_station");
    public static final RegistryObject<MenuType<ChestMenu>> DRAWING_TABLE = register("drawing_table");
    public static final RegistryObject<MenuType<ChestMenu>> ITEM_DROPPER = register("item_dropper");
    public static final RegistryObject<MenuType<ChestMenu>> LUGGAGE = register("luggage");
    public static final RegistryObject<MenuType<ChestMenu>> PAINT_MIXER = register("paint_mixer");
    public static final RegistryObject<MenuType<ChestMenu>> PROJECTOR = register("projector");
    public static final RegistryObject<MenuType<ChestMenu>> SPRINKLER = register("sprinkler");
    public static final RegistryObject<MenuType<ChestMenu>> VACUUM_HOPPER = register("vacuum_hopper");
    public static final RegistryObject<MenuType<ChestMenu>> XP_BOTTLER = register("xp_bottler");

    private OBMenuTypes() {}

    private static RegistryObject<MenuType<ChestMenu>> register(final String id) {
        return MENU_TYPES.register(id, () -> IForgeMenuType.create((windowId, inventory, data) -> ChestMenu.threeRows(windowId, inventory)));
    }
}
