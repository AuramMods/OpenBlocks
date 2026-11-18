package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBXPBottlerBlockEntity;
import art.arcane.openblocks.block.entity.OBXPBottlerBlockEntity.Port;
import art.arcane.openblocks.menu.OBXPBottlerMenu;
import art.arcane.openblocks.registry.OBMenuTypes;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBXPBottlerScreen extends AbstractContainerScreen<OBXPBottlerMenu> {

    private static final ResourceLocation FURNACE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final String KEY = "screen.open_blocks.xp_bottler.";
    private static final int CONTROL_PANEL = 180;
    private static final int GAUGE_X = 140;
    private static final int GAUGE_Y = 18;
    private static final int GAUGE_WIDTH = 17;
    private static final int GAUGE_HEIGHT = 37;
    private static final Direction[] SIDES = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final Map<Port, Button> tabs = new EnumMap<>(Port.class);
    private final Map<Direction, Button> sideButtons = new EnumMap<>(Direction.class);
    private Port selected = Port.INPUT;
    private Button automation;

    public OBXPBottlerScreen(final OBXPBottlerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 288;
        imageHeight = 151;
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
        for (int index = 0; index < SIDES.length; index++) {
            final Direction side = SIDES[index];
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
        sideButtons.forEach((side, button) -> {
            final boolean enabled = menu.isSideEnabled(selected, side);
            button.setMessage(Component.translatable(KEY + side.getName()).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            button.setTooltip(Tooltip.create(Component.translatable(KEY + (enabled ? "side_enabled" : "side_disabled"))));
        });
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
            final var fluid = menu.fluid();
            final Component volume = Component.translatable(KEY + "volume", fluid.getAmount(), menu.capacity());
            graphics.renderTooltip(font, fluid.isEmpty() ? List.of(volume) : List.of(fluid.getDisplayName(), volume),
                    Optional.empty(), mouseX, mouseY);
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
        panel(graphics, leftPos, topPos, 176, imageHeight);
        panel(graphics, leftPos + CONTROL_PANEL, topPos, imageWidth - CONTROL_PANEL, imageHeight);
        for (final var slot : menu.slots) recess(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
        graphics.blit(FURNACE, leftPos + 72, topPos + 30, 79, 34, 24, 16);
        final int progress = 24 * menu.progress() / OBXPBottlerBlockEntity.PROCESS_TICKS;
        if (progress > 0) graphics.blit(FURNACE, leftPos + 72, topPos + 30, 176, 14, progress, 16);
        drawGauge(graphics);
    }

    private void drawGauge(final GuiGraphics graphics) {
        final int x = leftPos + GAUGE_X;
        final int y = topPos + GAUGE_Y;
        recess(graphics, x, y, GAUGE_WIDTH, GAUGE_HEIGHT);
        final var fluid = menu.fluid();
        if (fluid.isEmpty() || menu.capacity() <= 0) return;
        final int full = GAUGE_HEIGHT - 2;
        final int height = (int) Math.min(full, Math.max(1L, (long) full * fluid.getAmount() / menu.capacity()));
        final int bottom = y + GAUGE_HEIGHT - 1;
        final var properties = IClientFluidTypeExtensions.of(fluid.getFluid());
        final var still = properties.getStillTexture(fluid);
        final int tint = properties.getTintColor(fluid);
        if (still == null) {
            graphics.fill(x + 1, bottom - height, x + GAUGE_WIDTH - 1, bottom, tint);
            return;
        }
        final var sprite = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
        graphics.enableScissor(x + 1, bottom - height, x + GAUGE_WIDTH - 1, bottom);
        graphics.setColor((tint >> 16 & 255) / 255.0F, (tint >> 8 & 255) / 255.0F, (tint & 255) / 255.0F, (tint >>> 24) / 255.0F);
        graphics.blit(x + 1, y + 1, 0, GAUGE_WIDTH - 2, full, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.disableScissor();
    }

    private static void panel(final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        graphics.fill(x, y, x + width, y + height, 0xff252525);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff555555);
        graphics.fill(x + 1, y + 1, x + width - 2, y + height - 2, 0xffffffff);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xffc6c6c6);
    }

    private static void recess(final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        graphics.fill(x, y, x + width, y + height, 0xffffffff);
        graphics.fill(x, y, x + width - 1, y + height - 1, 0xff373737);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff8b8b8b);
    }
}
