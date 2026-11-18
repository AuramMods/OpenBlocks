package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.menu.OBUtilityInventoryMenu;
import art.arcane.openblocks.registry.OBMenuTypes;
import art.arcane.openblocks.world.OBDonationMetadata;
import java.net.URI;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBUtilityInventoryScreen extends AbstractContainerScreen<OBUtilityInventoryMenu> {

    private static final String DONATION = "screen.open_blocks.donation_station.";
    private static final int LABEL_WIDTH = 114;
    @Nullable private Item lastSample;
    @Nullable private OBDonationMetadata.Entry mod;
    private Button visit;

    public OBUtilityInventoryScreen(final OBUtilityInventoryMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageHeight = menu.isSprinkler() ? 167 : 172;
        inventoryLabelY = menu.isSprinkler() ? 73 : 78;
    }

    @SubscribeEvent
    public static void registerScreens(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(OBMenuTypes.SPRINKLER.get(), OBUtilityInventoryScreen::new);
            MenuScreens.register(OBMenuTypes.DONATION_STATION.get(), OBUtilityInventoryScreen::new);
        });
    }

    @Override
    protected void init() {
        super.init();
        if (!menu.isSprinkler()) {
            lastSample = null;
            visit = addRenderableWidget(Button.builder(Component.translatable(DONATION + "donate"), button -> browseLink())
                    .bounds(leftPos + 30, topPos + 56, 116, 18).build());
            refreshDonation();
        }
    }

    @Nullable
    private URI link() {
        return mod == null ? null : mod.donation() != null ? mod.donation() : mod.homepage();
    }

    private void browseLink() {
        final URI url = link();
        if (url == null || minecraft == null || !minecraft.options.chatLinks().get()) return;
        if (minecraft.options.chatLinksPrompt().get()) ConfirmLinkScreen.confirmLinkNow(url.toASCIIString(), this, false);
        else Util.getPlatform().openUri(url);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!menu.isSprinkler()) refreshDonation();
    }

    private void refreshDonation() {
        final var stack = menu.sample();
        if (stack.getItem() != lastSample) {
            lastSample = stack.getItem();
            mod = OBDonationMetadata.identify(stack);
            visit.setMessage(Component.translatable(DONATION + (mod != null && mod.donation() == null && mod.homepage() != null ? "project" : "donate")));
        }
        final URI url = link();
        visit.active = url != null && minecraft != null && minecraft.options.chatLinks().get();
        visit.setTooltip(url == null ? null : Tooltip.create(Component.literal(url.toString())));
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.isSprinkler() && isHovering(OBMachineScreenLayout.GAUGE_X, OBMachineScreenLayout.GAUGE_Y,
                OBMachineScreenLayout.GAUGE_WIDTH, OBMachineScreenLayout.GAUGE_HEIGHT, mouseX, mouseY)) {
            OBMachineScreenLayout.gaugeTooltip(graphics, font, menu.fluid(), menu.capacity(), mouseX, mouseY);
        } else if (!menu.isSprinkler() && mod != null) {
            overflowTooltip(graphics, mod.name(), 30, mouseX, mouseY);
            overflowTooltip(graphics, String.join(", ", mod.authors()), 42, mouseX, mouseY);
        }
    }

    private void overflowTooltip(final GuiGraphics graphics, final String value, final int y, final int mouseX, final int mouseY) {
        if (font.width(value) > LABEL_WIDTH && isHovering(54, y, LABEL_WIDTH, 10, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(value), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY) {
        OBMachineScreenLayout.drawInventoryPanel(graphics, leftPos, topPos, imageWidth, imageHeight, menu);
        if (menu.isSprinkler()) OBMachineScreenLayout.drawGauge(graphics, leftPos, topPos, menu.fluid(), menu.capacity());
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.isSprinkler()) {
            final String state = menu.isActive() ? "spraying" : "dry";
            graphics.drawString(font, Component.translatable("screen.open_blocks.sprinkler." + state), 111, 73, 0x404040, false);
        } else if (menu.sample().isEmpty()) {
            graphics.drawString(font, Component.translatable(DONATION + "insert"), 54, 32, 0x606060, false);
        } else if (mod == null) {
            graphics.drawString(font, Component.translatable(DONATION + "unknown"), 54, 30, 0x404040, false);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(mod.name(), LABEL_WIDTH), 54, 30, 0x404040, false);
            graphics.drawString(font, font.plainSubstrByWidth(String.join(", ", mod.authors()), LABEL_WIDTH), 54, 42, 0x606060, false);
        }
    }
}
