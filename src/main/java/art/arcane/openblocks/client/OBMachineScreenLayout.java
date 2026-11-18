package art.arcane.openblocks.client;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

final class OBMachineScreenLayout {

    private static final String KEY = "screen.open_blocks.machine.";
    static final int CONTROL_PANEL = 180;
    static final int WIDTH = 288;
    static final int HEIGHT = 151;
    static final int GAUGE_X = 140;
    static final int GAUGE_Y = 18;
    static final int GAUGE_WIDTH = 17;
    static final int GAUGE_HEIGHT = 37;
    static final List<Direction> SIDES = List.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    private OBMachineScreenLayout() {}

    static void updateSide(final Button button, final Direction side, final boolean enabled) {
        button.setMessage(Component.translatable(KEY + side.getName()).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        button.setTooltip(Tooltip.create(Component.translatable(KEY + (enabled ? "side_enabled" : "side_disabled"))));
    }

    static void drawPanels(final GuiGraphics graphics, final int left, final int top, final AbstractContainerMenu menu) {
        panel(graphics, left, top, 176, HEIGHT);
        panel(graphics, left + CONTROL_PANEL, top, WIDTH - CONTROL_PANEL, HEIGHT);
        for (final var slot : menu.slots) recess(graphics, left + slot.x - 1, top + slot.y - 1, 18, 18);
    }

    static void drawGauge(final GuiGraphics graphics, final int left, final int top, final FluidStack fluid, final int capacity) {
        final int x = left + GAUGE_X;
        final int y = top + GAUGE_Y;
        recess(graphics, x, y, GAUGE_WIDTH, GAUGE_HEIGHT);
        if (fluid.isEmpty() || capacity <= 0) return;
        final int full = GAUGE_HEIGHT - 2;
        final int height = (int) Math.min(full, Math.max(1L, (long) full * fluid.getAmount() / capacity));
        final int bottom = y + GAUGE_HEIGHT - 1;
        final var properties = IClientFluidTypeExtensions.of(fluid.getFluid());
        final var still = properties.getStillTexture(fluid);
        final int tint = properties.getTintColor(fluid);
        if (still == null) {
            graphics.fill(x + 1, bottom - height, x + GAUGE_WIDTH - 1, bottom, tint);
            return;
        }
        final var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
        graphics.enableScissor(x + 1, bottom - height, x + GAUGE_WIDTH - 1, bottom);
        graphics.setColor((tint >> 16 & 255) / 255.0F, (tint >> 8 & 255) / 255.0F, (tint & 255) / 255.0F, (tint >>> 24) / 255.0F);
        graphics.blit(x + 1, y + 1, 0, GAUGE_WIDTH - 2, full, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.disableScissor();
    }

    static void gaugeTooltip(final GuiGraphics graphics, final Font font, final FluidStack fluid, final int capacity,
            final int mouseX, final int mouseY) {
        final Component volume = Component.translatable(KEY + "volume", fluid.getAmount(), capacity);
        graphics.renderTooltip(font, fluid.isEmpty() ? List.of(volume) : List.of(fluid.getDisplayName(), volume),
                Optional.empty(), mouseX, mouseY);
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
