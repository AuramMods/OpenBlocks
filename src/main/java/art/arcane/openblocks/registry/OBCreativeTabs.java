package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.config.OBClientConfig;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBElevatorItem;
import art.arcane.openblocks.item.OBEmptyMapItem;
import art.arcane.openblocks.item.OBGlyphItem;
import art.arcane.openblocks.item.OBImaginaryItem;
import art.arcane.openblocks.item.OBImaginationGlasses;
import art.arcane.openblocks.item.OBPaintBrushItem;
import art.arcane.openblocks.item.OBPaintCanItem;
import art.arcane.openblocks.item.OBPaintColors;
import art.arcane.openblocks.item.OBSkyItem;
import art.arcane.openblocks.item.OBStencilItem;
import art.arcane.openblocks.item.OBStencilPattern;
import art.arcane.openblocks.item.OBTankItem;
import art.arcane.openblocks.world.OBHeightMapData;
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
                        if (entry == OBItems.HEIGHT_MAP || entry == OBItems.CANVAS_GLASS) return;
                        if (item instanceof OBElevatorItem) {
                            for (final DyeColor color : DyeColor.values()) {
                                output.accept(OBElevatorItem.create(item, color));
                            }
                        } else if (item instanceof OBEmptyMapItem) {
                            for (int scale = 0; scale <= OBHeightMapData.MAX_SCALE; scale++) output.accept(OBEmptyMapItem.create(scale));
                        } else if (item instanceof OBPaintCanItem) {
                            for (final DyeColor dye : DyeColor.values()) output.accept(OBPaintCanItem.create(OBPaintColors.palette(dye), OBPaintCanItem.FULL));
                        } else if (item instanceof OBPaintBrushItem) {
                            output.accept(item);
                            for (final DyeColor dye : DyeColor.values()) output.accept(OBPaintBrushItem.create(OBPaintColors.palette(dye)));
                        } else if (item instanceof OBStencilItem) {
                            for (final var pattern : OBStencilPattern.values()) output.accept(OBStencilItem.create(pattern, 1));
                        } else if (item instanceof OBImaginaryItem) {
                            final float uses = OBImaginaryItem.defaultUses();
                            output.accept(OBImaginaryItem.create(null, OBImaginaryItem.Mode.BLOCK, uses));
                            for (final DyeColor color : DyeColor.values()) {
                                output.accept(OBImaginaryItem.create(OBPaintColors.palette(color), OBImaginaryItem.Mode.BLOCK, uses));
                            }
                        } else if (entry == OBItems.CRAYON_GLASSES) {
                            for (final DyeColor color : DyeColor.values()) {
                                output.accept(OBImaginationGlasses.createCrayonGlasses(OBPaintColors.palette(color)));
                            }
                        } else if (item instanceof OBGlyphItem) {
                            final boolean search = OBClientConfig.SPEC.isLoaded() && OBClientConfig.GLYPHS_IN_SEARCH.get();
                            for (int index = 0; index < OBGlyphItem.COUNT; index++) {
                                if (!OBGlyphItem.available(index)) continue;
                                final boolean displayed = OBGlyphItem.CREATIVE_CHARACTERS.indexOf(OBGlyphItem.character(index)) >= 0;
                                if (displayed || search) {
                                    output.accept(OBGlyphItem.create(index, 1), displayed
                                            ? (search ? CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS : CreativeModeTab.TabVisibility.PARENT_TAB_ONLY)
                                            : CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
                                }
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
