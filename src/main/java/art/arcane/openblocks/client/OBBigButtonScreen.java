package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.menu.OBBigButtonMenu;
import art.arcane.openblocks.registry.OBMenuTypes;
import java.util.Locale;
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
public final class OBBigButtonScreen extends AbstractContainerScreen<OBBigButtonMenu> {

    public OBBigButtonScreen(final OBBigButtonMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageHeight = 182;
        inventoryLabelY = 88;
    }

    @SubscribeEvent
    public static void register(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(OBMenuTypes.BIG_BUTTON.get(), OBBigButtonScreen::new));
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
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        final int ticks = menu.pulseLength();
        final String seconds = String.format(Locale.ROOT, "%.2f", ticks / 20.0);
        graphics.drawString(font, Component.translatable("screen.open_blocks.big_button.pulse", ticks, seconds), 8, 74, 0x404040, false);
    }
}
