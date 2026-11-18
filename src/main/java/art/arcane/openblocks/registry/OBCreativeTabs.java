package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.item.OBElevatorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class OBCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenBlocks.MODID);

    public static final RegistryObject<CreativeModeTab> OPEN_BLOCKS_TAB = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + OpenBlocks.MODID))
                    .icon(() -> new ItemStack(OBItems.LADDER.get()))
                    .displayItems((params, output) -> OBItems.all().forEach(entry -> {
                        final var item = entry.get();
                        if (item instanceof OBElevatorItem) {
                            for (final DyeColor color : DyeColor.values()) {
                                output.accept(OBElevatorItem.create(item, color));
                            }
                        } else {
                            output.accept(item);
                        }
                    }))
                    .build());

    private OBCreativeTabs() {}
}
