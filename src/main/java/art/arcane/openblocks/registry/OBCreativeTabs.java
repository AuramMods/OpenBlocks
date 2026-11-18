package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBElevatorItem;
import art.arcane.openblocks.item.OBSkyItem;
import art.arcane.openblocks.item.OBTankItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenBlocks.MODID);

    public static final RegistryObject<CreativeModeTab> OPEN_BLOCKS_TAB = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + OpenBlocks.MODID))
                    .icon(() -> new ItemStack(OBItems.FLAG.get()))
                    .displayItems((params, output) -> OBItems.all().forEach(entry -> {
                        final var item = entry.get();
                        if (entry == OBItems.XP_BUCKET && OBServerConfig.SPEC.isLoaded() && !OBServerConfig.XP_BUCKET_IN_CREATIVE.get()) return;
                        if (item instanceof OBElevatorItem) {
                            for (final DyeColor color : DyeColor.values()) {
                                output.accept(OBElevatorItem.create(item, color));
                            }
                        } else {
                            output.accept(item);
                            if (item instanceof OBSkyItem) output.accept(OBSkyItem.create(true));
                            if (item instanceof OBTankItem && (!OBServerConfig.SPEC.isLoaded() || OBServerConfig.TANK_CREATIVE_FLUIDS.get())) {
                                for (final var fluid : ForgeRegistries.FLUIDS) {
                                    if (fluid == Fluids.EMPTY || (fluid instanceof FlowingFluid flowing && flowing.getSource() != fluid)) continue;
                                    final var stack = new ItemStack(item);
                                    OBTankItem.setContents(stack, new FluidStack(fluid, OBTankBlockEntity.capacity()));
                                    output.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
                                }
                            }
                        }
                    }))
                    .build());

    private OBCreativeTabs() {}
}
