package art.arcane.openblocks.client;

import static art.arcane.openblocks.client.OBMachineScreenLayout.CONTROL_PANEL;
import static art.arcane.openblocks.client.OBMachineScreenLayout.GAUGE_HEIGHT;
import static art.arcane.openblocks.client.OBMachineScreenLayout.GAUGE_WIDTH;
import static art.arcane.openblocks.client.OBMachineScreenLayout.GAUGE_X;
import static art.arcane.openblocks.client.OBMachineScreenLayout.GAUGE_Y;
import static art.arcane.openblocks.client.OBMachineScreenLayout.SIDES;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBXPBottlerBlockEntity;
import art.arcane.openblocks.block.entity.OBXPBottlerBlockEntity.Port;
import art.arcane.openblocks.menu.OBXPBottlerMenu;
import art.arcane.openblocks.registry.OBMenuTypes;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBXPBottlerScreen extends AbstractContainerScreen<OBXPBottlerMenu> {

    private static final ResourceLocation FURNACE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final String KEY = "screen.open_blocks.xp_bottler.";
    private final Map<Port, Button> tabs = new EnumMap<>(Port.class);
    private final Map<Direction, Button> sideButtons = new EnumMap<>(Direction.class);
    private Port selected = Port.INPUT;
    private Button automation;

    public OBXPBottlerScreen(final OBXPBottlerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = OBMachineScreenLayout.WIDTH;
        imageHeight = OBMachineScreenLayout.HEIGHT;
        inventoryLabelY = 58;
    }

    @SubscribeEvent
    public static void setup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(OBMenuTypes.XP_BOTTLER.get(), OBXPBottlerScreen::new));
    }

    @Override
    protected void init() {
        super.init();
        final int start = leftPos + CONTROL_PANEL + 6;
        addTab(Port.INPUT, "input", start, 32);
        addTab(Port.XP, "xp", start + 32, 25);
        addTab(Port.OUTPUT, "output", start + 57, 40);
        for (int index = 0; index < SIDES.size(); index++) {
            final Direction side = SIDES.get(index);
            final int x = start + (index % 2) * 49;
            final int y = topPos + 46 + (index / 2) * 18;
            sideButtons.put(side, addRenderableWidget(Button.builder(Component.empty(), button -> send(OBXPBottlerMenu.sideButton(selected, side)))
                    .bounds(x, y, 47, 16).build()));
        }
        automation = addRenderableWidget(Button.builder(Component.empty(), button -> send(OBXPBottlerMenu.automaticButton(selected)))
                .bounds(start, topPos + 108, 97, 18).build());
        refreshControls();
    }

    private void addTab(final Port port, final String key, final int x, final int width) {
        tabs.put(port, addRenderableWidget(Button.builder(Component.translatable(KEY + key), button -> {
            selected = port;
            refreshControls();
        }).bounds(x, topPos + 22, width, 17).build()));
    }

    private void send(final int id) {
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshControls();
    }

    private void refreshControls() {
        tabs.forEach((port, button) -> button.active = port != selected);
        sideButtons.forEach((side, button) -> OBMachineScreenLayout.updateSide(button, side, menu.isSideEnabled(selected, side)));
        final boolean enabled = menu.isAutomatic(selected);
        final Component setting = Component.translatable(KEY + (enabled ? "on" : "off"));
        final String key = switch (selected) {
            case INPUT -> "auto_pull";
            case XP -> "auto_drink";
            case OUTPUT -> "auto_eject";
        };
        automation.setMessage(Component.translatable(KEY + key, setting).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(GAUGE_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT, mouseX, mouseY)) {
            OBMachineScreenLayout.gaugeTooltip(graphics, font, menu.fluid(), menu.capacity(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable(KEY + "automation"), CONTROL_PANEL + 7, 6, 0x404040, false);
        labelSlot(graphics, "input", 56);
        labelSlot(graphics, "output", 118);
    }

    private void labelSlot(final GuiGraphics graphics, final String key, final int center) {
        final Component label = Component.translatable(KEY + key);
        graphics.drawString(font, label, center - font.width(label) / 2, 18, 0x404040, false);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY) {
        OBMachineScreenLayout.drawPanels(graphics, leftPos, topPos, menu);
        graphics.blit(FURNACE, leftPos + 72, topPos + 30, 79, 34, 24, 16);
        final int progress = 24 * menu.progress() / OBXPBottlerBlockEntity.PROCESS_TICKS;
        if (progress > 0) graphics.blit(FURNACE, leftPos + 72, topPos + 30, 176, 14, progress, 16);
        OBMachineScreenLayout.drawGauge(graphics, leftPos, topPos, menu.fluid(), menu.capacity());
    }
}
