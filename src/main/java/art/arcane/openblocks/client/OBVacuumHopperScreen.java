package art.arcane.openblocks.client;

import static art.arcane.openblocks.client.OBMachineScreenLayout.CONTROL_PANEL;
import static art.arcane.openblocks.client.OBMachineScreenLayout.GAUGE_HEIGHT;
import static art.arcane.openblocks.client.OBMachineScreenLayout.GAUGE_WIDTH;
import static art.arcane.openblocks.client.OBMachineScreenLayout.GAUGE_X;
import static art.arcane.openblocks.client.OBMachineScreenLayout.GAUGE_Y;
import static art.arcane.openblocks.client.OBMachineScreenLayout.SIDES;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBVacuumHopperBlockEntity.Output;
import art.arcane.openblocks.menu.OBVacuumHopperMenu;
import art.arcane.openblocks.registry.OBMenuTypes;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBVacuumHopperScreen extends AbstractContainerScreen<OBVacuumHopperMenu> {

    private static final String KEY = "screen.open_blocks.vacuum_hopper.";
    private final Map<Output, Button> tabs = new EnumMap<>(Output.class);
    private final Map<Direction, Button> sides = new EnumMap<>(Direction.class);
    private Output selected = Output.ITEMS;

    public OBVacuumHopperScreen(final OBVacuumHopperMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = OBMachineScreenLayout.WIDTH;
        imageHeight = OBMachineScreenLayout.HEIGHT;
        inventoryLabelY = 58;
    }

    @SubscribeEvent
    public static void setup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(OBMenuTypes.VACUUM_HOPPER.get(), OBVacuumHopperScreen::new));
    }

    @Override
    protected void init() {
        super.init();
        final int start = leftPos + CONTROL_PANEL + 6;
        addTab(Output.ITEMS, "items", start);
        addTab(Output.XP, "xp", start + 49);
        for (int index = 0; index < SIDES.size(); index++) {
            final Direction side = SIDES.get(index);
            final int x = start + (index % 2) * 49;
            final int y = topPos + 46 + (index / 2) * 18;
            sides.put(side, addRenderableWidget(Button.builder(Component.empty(), button -> {
                if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, OBVacuumHopperMenu.sideButton(selected, side));
            }).bounds(x, y, 47, 16).build()));
        }
        refreshControls();
    }

    private void addTab(final Output output, final String key, final int x) {
        tabs.put(output, addRenderableWidget(Button.builder(Component.translatable(KEY + key), button -> {
            selected = output;
            refreshControls();
        }).bounds(x, topPos + 22, 47, 17).build()));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshControls();
    }

    private void refreshControls() {
        tabs.forEach((output, button) -> button.active = output != selected);
        sides.forEach((side, button) -> OBMachineScreenLayout.updateSide(button, side, menu.isSideEnabled(selected, side)));
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(GAUGE_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT, mouseX, mouseY)) {
            OBMachineScreenLayout.gaugeTooltip(graphics, font, menu.fluid(), menu.capacity(), mouseX, mouseY);
        } else if (isHovering(CONTROL_PANEL + 7, 111, 94, 12, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(KEY + "toggle_hint"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable(KEY + "outputs"), CONTROL_PANEL + 7, 6, 0x404040, false);
        final boolean enabled = menu.isEnabled();
        graphics.drawString(font, Component.translatable(KEY + (enabled ? "active" : "paused")),
                CONTROL_PANEL + 7, 112, enabled ? 0x236824 : 0x806811, false);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX, final int mouseY) {
        OBMachineScreenLayout.drawPanels(graphics, leftPos, topPos, menu);
        OBMachineScreenLayout.drawGauge(graphics, leftPos, topPos, menu.fluid(), menu.capacity());
    }
}
