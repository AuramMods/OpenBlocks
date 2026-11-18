package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.menu.OBRedstoneMachineMenu;
import art.arcane.openblocks.network.OBNetwork;
import art.arcane.openblocks.registry.OBMenuTypes;
import java.util.Locale;
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
public final class OBRedstoneMachineScreen extends AbstractContainerScreen<OBRedstoneMachineMenu> {

    private static final String DROPPER = "screen.open_blocks.item_dropper.";
    private SpeedSlider speed;
    private Button scaling;

    public OBRedstoneMachineScreen(final OBRedstoneMachineMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = menu.isDropper() ? 212 : 176;
        imageHeight = 167;
        inventoryLabelX = menu.isDropper() ? 26 : 8;
    }

    @SubscribeEvent
    public static void registerScreens(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(OBMenuTypes.BLOCK_PLACER.get(), OBRedstoneMachineScreen::new);
            MenuScreens.register(OBMenuTypes.ITEM_DROPPER.get(), OBRedstoneMachineScreen::new);
        });
    }

    @Override
    protected void init() {
        super.init();
        if (!menu.isDropper()) return;
        speed = addRenderableWidget(new SpeedSlider(leftPos + 70, topPos + 18));
        scaling = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, OBRedstoneMachineMenu.TOGGLE_REDSTONE_SCALING);
            }
        }).bounds(leftPos + 70, topPos + 45, 134, 20).build());
        scaling.setTooltip(Tooltip.create(Component.translatable(DROPPER + "redstone_hint")));
        updateControls();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.isDropper()) updateControls();
    }

    private void updateControls() {
        speed.sync();
        scaling.setMessage(Component.translatable(DROPPER + (menu.scalesWithRedstone() ? "redstone_on" : "redstone_off")));
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

    private final class SpeedSlider extends AbstractSliderButton {
        private boolean dragging;
        private int syncedSpeed = -1;
        private int maximum = -1;
        private int lastRequested = -1;

        private SpeedSlider(final int x, final int y) {
            super(x, y, 134, 20, Component.empty(), 0);
        }

        private void sync() {
            final int received = menu.speedTenths();
            final int limit = menu.maxSpeedTenths();
            if (dragging || (received == syncedSpeed && limit == maximum)) return;
            syncedSpeed = received;
            maximum = limit;
            lastRequested = received;
            active = maximum > 0;
            value = active ? (double) received / maximum : 0.0;
            updateMessage();
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            dragging = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        public void onRelease(final double mouseX, final double mouseY) {
            dragging = false;
            super.onRelease(mouseX, mouseY);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(DROPPER + "speed", String.format(Locale.ROOT, "%.1f", steps() / 10.0)));
        }

        @Override
        protected void applyValue() {
            final int requested = steps();
            if (requested == lastRequested) return;
            lastRequested = requested;
            OBNetwork.requestDropperSpeed(menu.containerId, requested);
        }

        private int steps() {
            return (int) Math.round(value * Math.max(0, maximum));
        }
    }
}
