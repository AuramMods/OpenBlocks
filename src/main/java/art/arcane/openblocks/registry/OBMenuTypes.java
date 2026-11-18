package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.menu.OBRedstoneMachineMenu;
import art.arcane.openblocks.menu.OBVacuumHopperMenu;
import art.arcane.openblocks.menu.OBXPBottlerMenu;
import art.arcane.openblocks.menu.OBXpWorkbenchMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, OpenBlocks.MODID);

    public static final RegistryObject<MenuType<OBXpWorkbenchMenu>> AUTO_ANVIL = MENU_TYPES.register("auto_anvil",
            () -> new MenuType<>(OBXpWorkbenchMenu::anvil, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<OBXpWorkbenchMenu>> AUTO_ENCHANTMENT_TABLE = MENU_TYPES.register("auto_enchantment_table",
            () -> new MenuType<>(OBXpWorkbenchMenu::enchantmentTable, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ChestMenu>> BIG_BUTTON = register("big_button");
    public static final RegistryObject<MenuType<OBRedstoneMachineMenu>> BLOCK_PLACER = MENU_TYPES.register("block_placer",
            () -> new MenuType<>(OBRedstoneMachineMenu::blockPlacer, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ChestMenu>> DEV_NULL = register("dev_null");
    public static final RegistryObject<MenuType<ChestMenu>> DONATION_STATION = register("donation_station");
    public static final RegistryObject<MenuType<ChestMenu>> DRAWING_TABLE = register("drawing_table");
    public static final RegistryObject<MenuType<OBRedstoneMachineMenu>> ITEM_DROPPER = MENU_TYPES.register("item_dropper",
            () -> new MenuType<>(OBRedstoneMachineMenu::itemDropper, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ChestMenu>> LUGGAGE = register("luggage");
    public static final RegistryObject<MenuType<ChestMenu>> PAINT_MIXER = register("paint_mixer");
    public static final RegistryObject<MenuType<ChestMenu>> PROJECTOR = register("projector");
    public static final RegistryObject<MenuType<ChestMenu>> SPRINKLER = register("sprinkler");
    public static final RegistryObject<MenuType<OBVacuumHopperMenu>> VACUUM_HOPPER = MENU_TYPES.register("vacuum_hopper",
            () -> new MenuType<>(OBVacuumHopperMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<OBXPBottlerMenu>> XP_BOTTLER = MENU_TYPES.register("xp_bottler",
            () -> new MenuType<>(OBXPBottlerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private OBMenuTypes() {}

    private static RegistryObject<MenuType<ChestMenu>> register(final String id) {
        return MENU_TYPES.register(id, () -> IForgeMenuType.create((windowId, inventory, data) -> ChestMenu.threeRows(windowId, inventory)));
    }
}
