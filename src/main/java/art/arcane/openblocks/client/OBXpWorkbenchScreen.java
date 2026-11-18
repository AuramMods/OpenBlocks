package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBXpWorkbenchBlockEntity;
import art.arcane.openblocks.block.entity.OBXpWorkbenchBlockEntity.Port;
import art.arcane.openblocks.menu.OBXpWorkbenchMenu;
import art.arcane.openblocks.registry.OBMenuTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBXpWorkbenchScreen extends AbstractContainerScreen<OBXpWorkbenchMenu> {

    private static final String KEY = "screen.open_blocks.xp_workbench.";
    private static final Port[] PORTS = Port.values();
    private static final String[] TIERS = {"I", "II", "III"};
    private final Button[] tabs = new Button[PORTS.length];
    private final Button[] faces = new Button[OBMachineScreenLayout.SIDES.size()];
    private final Button[] tiers = new Button[TIERS.length];
    private Port selected = Port.TOOL;
    private Button automatic;
    private PowerSlider power;

    public OBXpWorkbenchScreen(final OBXpWorkbenchMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = OBMachineScreenLayout.WIDTH;
        imageHeight = 176;
        inventoryLabelY = 83;
    }

    @SubscribeEvent
    public static void registerScreens(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(OBMenuTypes.AUTO_ANVIL.get(), OBXpWorkbenchScreen::new);
            MenuScreens.register(OBMenuTypes.AUTO_ENCHANTMENT_TABLE.get(), OBXpWorkbenchScreen::new);
        });
    }

    @Override
    protected void init() {
        super.init();
        final int panel = leftPos + OBMachineScreenLayout.CONTROL_PANEL;
        for (final Port port : PORTS) {
            final int index = port.ordinal();
            tabs[index] = button(portLabel(port), panel + 5 + index % 2 * 49, topPos + 18 + index / 2 * 21, 49, () -> {
                selected = port;
                updateControls();
            });
        }
        for (int index = 0; index < faces.length; index++) {
            final var face = OBMachineScreenLayout.SIDES.get(index);
            faces[index] = button(Component.empty(), panel + 5 + index % 2 * 49, topPos + 65 + index / 2 * 20, 49,
                    () -> send(OBXpWorkbenchMenu.sideButton(selected, face)));
        }
        automatic = button(Component.empty(), panel + 5, topPos + 137, 98, () -> send(OBXpWorkbenchMenu.autoButton(selected)));
        if (menu.enchanting()) {
            for (int index = 0; index < tiers.length; index++) {
                final int tier = index + 1;
                tiers[index] = button(Component.literal(TIERS[index]), leftPos + 8 + index * 20, topPos + 59, 19,
                        () -> send(OBXpWorkbenchMenu.tierButton(tier)));
                tiers[index].setTooltip(Tooltip.create(Component.translatable(KEY + "tier_hint", tier)));
            }
            power = addRenderableWidget(new PowerSlider(leftPos + 72, topPos + 59));
            power.setTooltip(Tooltip.create(Component.translatable(KEY + "power_hint")));
        }
        updateControls();
    }

    private Button button(final Component caption, final int x, final int y, final int width, final Runnable action) {
        return addRenderableWidget(Button.builder(caption, button -> action.run()).bounds(x, y, width, 19).build());
    }

    private void send(final int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private Component portLabel(final Port port) {
        return Component.translatable(KEY + switch (port) {
            case TOOL -> "tool";
            case REAGENT -> menu.enchanting() ? "lapis" : "modifier";
            case OUTPUT -> "output";
            case XP -> "xp";
        });
    }

    @Override
    protected void containerTick() { updateControls(); }

    private void updateControls() {
        for (final Port port : PORTS) tabs[port.ordinal()].active = port != selected;
        for (int index = 0; index < faces.length; index++) {
            final var face = OBMachineScreenLayout.SIDES.get(index);
            OBMachineScreenLayout.updateSide(faces[index], face, menu.isSideEnabled(selected, face));
        }
        final String action = switch (selected) {
            case XP -> "drink";
            case OUTPUT -> "eject";
            default -> "pull";
        };
        final boolean enabled = menu.isAutomatic(selected);
        automatic.setMessage(Component.translatable(KEY + action, Component.translatable(KEY + (enabled ? "on" : "off")))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        if (menu.enchanting()) {
            for (int index = 0; index < tiers.length; index++) tiers[index].active = menu.tier() != index + 1;
            power.sync();
        }
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(OBMachineScreenLayout.GAUGE_X, OBMachineScreenLayout.GAUGE_Y, OBMachineScreenLayout.GAUGE_WIDTH,
                OBMachineScreenLayout.GAUGE_HEIGHT, mouseX, mouseY)) {
            OBMachineScreenLayout.gaugeTooltip(graphics, font, menu.fluid(), menu.capacity(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY) {
        OBMachineScreenLayout.drawPanels(graphics, leftPos, topPos, menu, imageHeight);
        OBMachineScreenLayout.drawGauge(graphics, leftPos, topPos, menu.fluid(), menu.capacity());
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable(KEY + "automation"), OBMachineScreenLayout.CONTROL_PANEL + 6, 6, 0x404040, false);
        graphics.drawString(font, portLabel(Port.TOOL), 10, 21, 0x404040, false);
        graphics.drawString(font, portLabel(Port.REAGENT), 51, 21, 0x404040, false);
        graphics.drawString(font, portLabel(Port.OUTPUT), 105, 21, 0x404040, false);
        graphics.drawString(font, menu.enchanting() ? "→" : "+", 40, 37, 0x404040, false);
        graphics.drawString(font, "→", 90, 37, 0x404040, false);
        if (menu.enchanting()) graphics.drawString(font, Component.translatable(KEY + "shelf_power", menu.availablePower()), 8, 51, 0x404040, false);
    }

    private final class PowerSlider extends AbstractSliderButton {
        private static final int STEPS = OBXpWorkbenchBlockEntity.MAXIMUM_POWER - 1;
        private int synced;
        private int lastSent;
        private boolean dragging;

        private PowerSlider(final int x, final int y) { super(x, y, 96, 19, Component.empty(), 0); }

        private int limit() { return (int) Math.round(value * STEPS) + 1; }

        private void sync() {
            final int limit = Math.max(1, menu.powerLimit());
            if (dragging || synced == limit) return;
            synced = limit;
            lastSent = limit;
            value = (limit - 1) / (double) STEPS;
            updateMessage();
        }

        @Override public void onClick(final double mouseX, final double mouseY) { dragging = true; super.onClick(mouseX, mouseY); }
        @Override public void onRelease(final double mouseX, final double mouseY) { dragging = false; super.onRelease(mouseX, mouseY); }
        @Override protected void updateMessage() { setMessage(Component.translatable(KEY + "power_limit", limit())); }

        @Override
        protected void applyValue() {
            if (limit() == lastSent) return;
            lastSent = limit();
            send(OBXpWorkbenchMenu.powerButton(lastSent));
        }
    }
}
