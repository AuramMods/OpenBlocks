package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBDrawingTableBlockEntity;
import art.arcane.openblocks.item.OBGlyphItem;
import art.arcane.openblocks.item.OBStencilItem;
import art.arcane.openblocks.menu.OBDrawingTableMenu;
import art.arcane.openblocks.network.OBNetwork;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBMenuTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBDrawingTableScreen extends AbstractContainerScreen<OBDrawingTableMenu> {

    private static final String LABEL = "gui.open_blocks.drawing_table.";
    private EditBox text;
    private Button print;
    private Button mode;
    private int lastRevision = Integer.MIN_VALUE;

    public OBDrawingTableScreen(final OBDrawingTableMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageHeight = 206;
        inventoryLabelY = 114;
    }

    @SubscribeEvent
    public static void setup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(OBMenuTypes.DRAWING_TABLE.get(), OBDrawingTableScreen::new);
            ItemProperties.register(OBItems.STENCIL.get(), OpenBlocks.id("stencil_pattern"), (stack, level, holder, seed) -> {
                final var pattern = OBStencilItem.pattern(stack);
                return pattern == null ? 0 : pattern.ordinal() + 1;
            });
        });
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(Component.empty(), button -> select(OBDrawingTableMenu.CYCLE_MODE))
                .bounds(leftPos + 8, topPos + 35, 48, 16).build());
        addRenderableWidget(Button.builder(Component.literal("˄"), button -> select(OBDrawingTableMenu.NEXT))
                .tooltip(Tooltip.create(label("next"))).bounds(leftPos + 123, topPos + 29, 17, 14).build());
        addRenderableWidget(Button.builder(Component.literal("˅"), button -> select(OBDrawingTableMenu.PREVIOUS))
                .tooltip(Tooltip.create(label("previous"))).bounds(leftPos + 123, topPos + 45, 17, 14).build());

        final String previous = text == null ? "" : text.getValue();
        text = new EditBox(font, leftPos + 8, topPos + 91, 117, 16, label("text"));
        text.setMaxLength(OBDrawingTableBlockEntity.MAX_PRINT_LENGTH);
        text.setValue(previous);
        addRenderableWidget(text);
        print = addRenderableWidget(Button.builder(label("print"), button -> OBNetwork.requestGlyphPrint(menu.containerId, text.getValue()))
                .bounds(leftPos + 128, topPos + 91, 40, 16).build());
        updateControls();
    }

    private void select(final int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        text.tick();
        updateControls();
    }

    private void updateControls() {
        final boolean glyphs = menu.glyphMode();
        mode.setMessage(label(glyphs ? "glyphs" : "stencils"));
        text.setVisible(glyphs);
        text.setEditable(glyphs);
        if (!glyphs) text.setFocused(false);
        print.visible = glyphs;
        final var table = menu.table();
        if (table != null && table.printRevision() != lastRevision) {
            lastRevision = table.printRevision();
            text.setValue(table.remainingText());
        }
        print.active = glyphs && !text.getValue().isEmpty() && menu.getSlot(0).hasItem();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partial, final int mouseX, final int mouseY) {
        rectangle(graphics, 0, 0, imageWidth, imageHeight, 0xFF383838);
        rectangle(graphics, 1, 1, imageWidth - 2, imageHeight - 2, 0xFFC6C6C6);
        for (final var slot : menu.slots) well(graphics, slot.x - 1, slot.y - 1, 18, 18);
        well(graphics, 144, 31, 24, 24);
        graphics.drawString(font, "→", leftPos + 83, topPos + 38, 0xFF505050, false);
        final var icon = menu.glyphMode() ? OBGlyphItem.create(menu.glyph(), 1) : OBStencilItem.create(menu.pattern(), 1);
        graphics.renderFakeItem(icon, leftPos + 148, topPos + 35);
        final var name = menu.glyphMode() ? icon.getHoverName() : OBStencilItem.name(menu.pattern());
        graphics.drawCenteredString(font, name, leftPos + 88, topPos + 69, 0xFFECECEC);
    }

    private void well(final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        rectangle(graphics, x, y, width, height, 0xFF575757);
        rectangle(graphics, x + 1, y + 1, width - 1, height - 1, 0xFFFFFFFF);
        rectangle(graphics, x + 1, y + 1, width - 2, height - 2, 0xFF8B8B8B);
    }

    private void rectangle(final GuiGraphics graphics, final int x, final int y, final int width, final int height, final int color) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, color);
    }

    private static Component label(final String key) { return Component.translatable(LABEL + key); }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key != GLFW.GLFW_KEY_ESCAPE && text.isFocused() && (text.keyPressed(key, scanCode, modifiers) || text.canConsumeInput())) return true;
        return super.keyPressed(key, scanCode, modifiers);
    }
}
