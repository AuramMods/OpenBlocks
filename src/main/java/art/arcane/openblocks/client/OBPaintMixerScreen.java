package art.arcane.openblocks.client;

import art.arcane.openblocks.block.entity.OBPaintMixerBlockEntity;
import art.arcane.openblocks.item.OBPaintColors;
import art.arcane.openblocks.menu.OBPaintMixerMenu;
import art.arcane.openblocks.network.OBNetwork;
import java.awt.Color;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

public final class OBPaintMixerScreen extends AbstractContainerScreen<OBPaintMixerMenu> {

    private static final int PICK_X = 10;
    private static final int PICK_Y = 42;
    private static final int PICK_WIDTH = 96;
    private static final int PICK_HEIGHT = 36;
    private static final int TONE_Y = 83;
    private static final int TONE_HEIGHT = 9;
    private static final int SWATCH_SIZE = 6;
    private static final int[] PIGMENT_COLORS = {0xFF4B9FC1, 0xFFDB7AD5, 0xFFE7E72A, 0xFF252525};

    private EditBox hex;
    private Button mix;
    private int synced;
    private float hue;
    private float saturation;
    private float value;
    private boolean editing;
    private int dragControl;

    public OBPaintMixerScreen(final OBPaintMixerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageHeight = 209;
        inventoryLabelY = 117;
    }

    @Override
    protected void init() {
        super.init();
        hex = new EditBox(font, leftPos + 62, topPos + 97, 44, 15, Component.translatable("gui.open_blocks.paint_mixer.hex"));
        hex.setMaxLength(6);
        hex.setFilter(text -> text.matches("[0-9a-fA-F]{0,6}"));
        hex.setResponder(text -> {
            if (!editing && text.length() == 6) setHsv(Integer.parseInt(text, 16));
        });
        addRenderableWidget(hex);
        mix = addRenderableWidget(Button.builder(Component.translatable("gui.open_blocks.paint_mixer.mix"),
                button -> OBNetwork.requestPaintMix(menu.containerId, Integer.parseInt(hex.getValue(), 16)))
                .bounds(leftPos + 123, topPos + 55, 42, 15).build());
        synced = menu.color();
        select(synced);
        updateButton();
    }

    private void setHsv(final int rgb) {
        final float[] hsv = Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
    }

    private void select(final int rgb) {
        setHsv(rgb);
        syncHex(rgb);
    }

    private void syncHex(final int rgb) {
        editing = true;
        hex.setValue(String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF));
        editing = false;
    }

    @Override
    protected void containerTick() {
        hex.tick();
        if (synced != menu.color()) {
            synced = menu.color();
            select(synced);
        }
        updateButton();
    }

    private void updateButton() {
        mix.active = hex.getValue().length() == 6 && !menu.getSlot(0).getItem().isEmpty();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
        final int palette = paletteIndex(mouseX - leftPos, mouseY - topPos);
        if (palette >= 0) graphics.renderTooltip(font, Component.translatable("color.minecraft." + DyeColor.byId(palette).getName()), mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partial, final int mouseX, final int mouseY) {
        graphics.drawManaged(() -> {
            rectangle(graphics, 0, 0, imageWidth, imageHeight, 0xFF383838);
            rectangle(graphics, 1, 1, imageWidth - 2, imageHeight - 2, 0xFFC6C6C6);
            for (final var slot : menu.slots) well(graphics, slot.x - 1, slot.y - 1, 18, 18);
            well(graphics, PICK_X - 1, PICK_Y - 1, PICK_WIDTH + 2, PICK_HEIGHT + 2);
            for (int x = 0; x < PICK_WIDTH; x++) {
                final int x0 = leftPos + PICK_X + x;
                graphics.fillGradient(x0, topPos + PICK_Y, x0 + 1, topPos + PICK_Y + PICK_HEIGHT,
                        Color.HSBtoRGB(x / (float) PICK_WIDTH, 1, value), Color.HSBtoRGB(x / (float) PICK_WIDTH, 0, value));
            }
            well(graphics, PICK_X - 1, TONE_Y - 1, PICK_WIDTH + 2, TONE_HEIGHT + 2);
            for (int x = 0; x < PICK_WIDTH; x++) {
                rectangle(graphics, PICK_X + x, TONE_Y, 1, TONE_HEIGHT, Color.HSBtoRGB(hue, saturation, x / (float) (PICK_WIDTH - 1)));
            }
            final int cursorX = PICK_X + Math.round(hue * (PICK_WIDTH - 1));
            final int cursorY = PICK_Y + Math.round((1 - saturation) * (PICK_HEIGHT - 1));
            rectangle(graphics, cursorX - 2, cursorY, 5, 1, 0xFFFFFFFF);
            rectangle(graphics, cursorX, cursorY - 2, 1, 5, 0xFFFFFFFF);
            rectangle(graphics, PICK_X + Math.round(value * (PICK_WIDTH - 1)), TONE_Y, 1, TONE_HEIGHT, 0xFFFFFFFF);
            well(graphics, 9, 97, 39, 15);
            rectangle(graphics, 10, 98, 37, 13, Color.HSBtoRGB(hue, saturation, value));
            for (int index = 0; index < 16; index++) {
                rectangle(graphics, 10 + index * SWATCH_SIZE, 28, SWATCH_SIZE, 9, 0xFF000000 | OBPaintColors.palette(DyeColor.byId(index)));
            }
            well(graphics, 123, 46, 42, 6);
            rectangle(graphics, 124, 47, 40 * menu.progress() / OBPaintMixerBlockEntity.DURATION, 4, 0xFF4FA76B);
            for (int i = 0; i < 4; i++) {
                final int x = 119 + 44 * (i % 2);
                final int y = 79 + 21 * (i / 2);
                final int height = 16 * menu.pigment(i) / (2 * OBPaintMixerBlockEntity.UNIT);
                rectangle(graphics, x, y, 3, 16, 0xFF646464);
                rectangle(graphics, x, y + 16 - height, 3, height, PIGMENT_COLORS[i]);
            }
        });
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, "#", 55, 101, 0x404040, false);
    }

    private void well(final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        rectangle(graphics, x, y, width, height, 0xFF595959);
        rectangle(graphics, x + 1, y + 1, width - 1, height - 1, 0xFFFFFFFF);
        rectangle(graphics, x + 1, y + 1, width - 2, height - 2, 0xFF8B8B8B);
    }

    private void rectangle(final GuiGraphics graphics, final int x, final int y, final int width, final int height, final int color) {
        if (width > 0 && height > 0) graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, color);
    }

    private static int paletteIndex(final double x, final double y) {
        return y >= 28 && y < 37 && x >= 10 && x < 10 + 16 * SWATCH_SIZE ? (int) (x - 10) / SWATCH_SIZE : -1;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = mouseX - leftPos;
        final double y = mouseY - topPos;
        final int palette = paletteIndex(x, y);
        if (button == 0 && palette >= 0) {
            select(OBPaintColors.palette(DyeColor.byId(palette)));
            return true;
        }
        if (button == 0 && x >= PICK_X && x < PICK_X + PICK_WIDTH) {
            if (y >= PICK_Y && y < PICK_Y + PICK_HEIGHT) dragControl = 1;
            else if (y >= TONE_Y && y < TONE_Y + TONE_HEIGHT) dragControl = 2;
            if (dragControl != 0) {
                pick(x, y);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double x, final double y, final int button, final double dx, final double dy) {
        if (button == 0 && dragControl != 0) {
            pick(x - leftPos, y - topPos);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(final double x, final double y, final int button) {
        if (button == 0 && dragControl != 0) {
            dragControl = 0;
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    private void pick(final double x, final double y) {
        final float position = Mth.clamp((float) (x - PICK_X) / (PICK_WIDTH - 1), 0, 1);
        if (dragControl == 1) {
            hue = Math.min(position, 0.9999F);
            saturation = 1 - Mth.clamp((float) (y - PICK_Y) / (PICK_HEIGHT - 1), 0, 1);
        } else {
            value = position;
        }
        syncHex(Color.HSBtoRGB(hue, saturation, value));
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (hex.isFocused() && (hex.keyPressed(key, scanCode, modifiers) || hex.canConsumeInput())
                && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) return true;
        return super.keyPressed(key, scanCode, modifiers);
    }
}
