package art.arcane.openblocks;

import art.arcane.openblocks.advancement.OBCriterions;
import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.client.OBComputerCraftMagnetClient;
import art.arcane.openblocks.config.OBClientConfig;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.integration.OBComputerCraftCannon;
import art.arcane.openblocks.integration.OBComputerCraftDonations;
import art.arcane.openblocks.integration.OBComputerCraftGuide;
import art.arcane.openblocks.integration.OBComputerCraftMagnet;
import art.arcane.openblocks.integration.OBComputerCraftProjector;
import art.arcane.openblocks.network.OBNetwork;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBCreativeTabs;
import art.arcane.openblocks.registry.OBEnchantments;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBFluidTypes;
import art.arcane.openblocks.registry.OBFluids;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBMenuTypes;
import art.arcane.openblocks.registry.OBRecipeSerializers;
import art.arcane.openblocks.registry.OBSounds;
import art.arcane.openblocks.registry.OBStats;
import art.arcane.openblocks.world.OBGameRules;
import art.arcane.openblocks.world.OBTechnicolorLoot;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(OpenBlocks.MODID)
public class OpenBlocks {

    public static final String MODID = "open_blocks";
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    static {
        LOOT_MODIFIERS.register("technicolor_glasses", () -> OBTechnicolorLoot.CODEC);
    }

    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public OpenBlocks(final FMLJavaModLoadingContext context) {
        final IEventBus modEventBus = context.getModEventBus();

        OBFluidTypes.FLUID_TYPES.register(modEventBus);
        OBFluids.FLUIDS.register(modEventBus);
        OBFluids.FLUID_BLOCKS.register(modEventBus);

        OBBlocks.BLOCKS.register(modEventBus);
        OBItems.ITEMS.register(modEventBus);
        OBCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        OBBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        OBEntities.ENTITY_TYPES.register(modEventBus);
        OBSounds.SOUND_EVENTS.register(modEventBus);
        OBStats.CUSTOM_STATS.register(modEventBus);
        OBEnchantments.ENCHANTMENTS.register(modEventBus);
        OBMenuTypes.MENU_TYPES.register(modEventBus);
        OBRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
        modEventBus.addListener(OBCapabilities::onRegisterCapabilities);
        if (ModList.get().isLoaded("computercraft")) {
            modEventBus.addListener(this::setupComputerCraft);
            OBComputerCraftMagnet.register(modEventBus);
            if (FMLEnvironment.dist == Dist.CLIENT) OBComputerCraftMagnetClient.register(modEventBus);
        }

        context.registerConfig(ModConfig.Type.SERVER, OBServerConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, OBClientConfig.SPEC);
        OBNetwork.register();
        OBCriterions.bootstrap();
        OBGameRules.bootstrap();
    }

    private void setupComputerCraft(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            OBComputerCraftDonations.register();
            OBComputerCraftGuide.register();
            OBComputerCraftCannon.register();
            OBComputerCraftProjector.register();
        });
    }
}
