package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBLuggage;
import art.arcane.openblocks.menu.OBBigButtonMenu;
import art.arcane.openblocks.menu.OBDevNullMenu;
import art.arcane.openblocks.menu.OBDrawingTableMenu;
import art.arcane.openblocks.menu.OBPaintMixerMenu;
import art.arcane.openblocks.menu.OBProjectorMenu;
import art.arcane.openblocks.menu.OBRedstoneMachineMenu;
import art.arcane.openblocks.menu.OBUtilityInventoryMenu;
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
    public static final RegistryObject<MenuType<OBBigButtonMenu>> BIG_BUTTON = MENU_TYPES.register("big_button",
            () -> new MenuType<>(OBBigButtonMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<OBRedstoneMachineMenu>> BLOCK_PLACER = MENU_TYPES.register("block_placer",
            () -> new MenuType<>(OBRedstoneMachineMenu::blockPlacer, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<OBDevNullMenu>> DEV_NULL = MENU_TYPES.register("dev_null",
            () -> IForgeMenuType.create(OBDevNullMenu::new));
    public static final RegistryObject<MenuType<OBUtilityInventoryMenu>> DONATION_STATION = MENU_TYPES.register("donation_station",
            () -> new MenuType<>(OBUtilityInventoryMenu::donationStation, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<OBDrawingTableMenu>> DRAWING_TABLE = MENU_TYPES.register("drawing_table",
            () -> IForgeMenuType.create(OBDrawingTableMenu::new));
    public static final RegistryObject<MenuType<OBRedstoneMachineMenu>> ITEM_DROPPER = MENU_TYPES.register("item_dropper",
            () -> new MenuType<>(OBRedstoneMachineMenu::itemDropper, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ChestMenu>> LUGGAGE = MENU_TYPES.register("luggage",
            () -> IForgeMenuType.create(OBLuggage::createClientMenu));
    public static final RegistryObject<MenuType<OBPaintMixerMenu>> PAINT_MIXER = MENU_TYPES.register("paint_mixer",
            () -> IForgeMenuType.create(OBPaintMixerMenu::new));
    public static final RegistryObject<MenuType<OBProjectorMenu>> PROJECTOR = MENU_TYPES.register("projector",
            () -> IForgeMenuType.create(OBProjectorMenu::new));
    public static final RegistryObject<MenuType<OBUtilityInventoryMenu>> SPRINKLER = MENU_TYPES.register("sprinkler",
            () -> new MenuType<>(OBUtilityInventoryMenu::sprinkler, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<OBVacuumHopperMenu>> VACUUM_HOPPER = MENU_TYPES.register("vacuum_hopper",
            () -> new MenuType<>(OBVacuumHopperMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<OBXPBottlerMenu>> XP_BOTTLER = MENU_TYPES.register("xp_bottler",
            () -> new MenuType<>(OBXPBottlerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private OBMenuTypes() {}

}
