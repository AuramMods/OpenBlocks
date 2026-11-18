package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.menu.OBDevNullMenu;
import art.arcane.openblocks.registry.OBMenuTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBDevNullScreen extends AbstractContainerScreen<OBDevNullMenu> {

    public OBDevNullScreen(final OBDevNullMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageHeight = 137;
        inventoryLabelY = 44;
    }

    @SubscribeEvent
    public static void register(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(OBMenuTypes.DEV_NULL.get(), OBDevNullScreen::new));
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY) {
        OBMachineScreenLayout.drawInventoryPanel(graphics, leftPos, topPos, imageWidth, imageHeight, menu);
        for (final var slot : menu.slots) {
            if (!slot.isActive()) {
                graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xff555555);
            }
        }
    }
}
