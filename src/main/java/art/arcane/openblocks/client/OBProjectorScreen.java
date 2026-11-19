package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.menu.OBProjectorMenu;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Matrix4f;

public final class OBProjectorScreen extends AbstractContainerScreen<OBProjectorMenu> {

    private static final ResourceLocation TEXTURE = OpenBlocks.id("textures/gui/projector.png");
    private static final int VIEW_WIDTH = 160;
    private static final int VIEW_HEIGHT = 118;
    private float yaw = 35;
    private float pitch = 28;
    private float verticalOffset;
    private boolean dragging;

    public OBProjectorScreen(final OBProjectorMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 234;
    }

    @Override
    protected void init() {
        super.init();
        rotationButton(7, "‹", "rotate_left", OBProjectorMenu.ROTATE_LEFT);
        rotationButton(152, "›", "rotate_right", OBProjectorMenu.ROTATE_RIGHT);
    }

    private void rotationButton(final int x, final String label, final String tooltip, final int action) {
        addRenderableWidget(Button.builder(Component.literal(label),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action))
                .bounds(leftPos + x, topPos + 130, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.open_blocks.projector." + tooltip))).build());
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partial, final int mouseX, final int mouseY) {
        final int x = leftPos + 8;
        final int y = topPos + 8;
        graphics.fill(x, y, x + VIEW_WIDTH, topPos + 147, 0xFF182027);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        final var projector = menu.projector();
        final var data = projector == null ? null : projector.map();
        if (data == null) {
            if (menu.getSlot(0).hasItem()) drawPreviewMessage(graphics, x, y, "loading");
            return;
        }
        final var mesh = OBProjectorRendering.mesh(data);
        if (mesh.isEmpty()) {
            drawPreviewMessage(graphics, x, y, "empty_map");
            return;
        }
        final float zoom = Math.min(88, 114 / Math.max(1.2F, mesh.height() + 0.8F));
        final float centerY = mesh.midpoint() + verticalOffset;
        graphics.enableScissor(x, y, x + VIEW_WIDTH, y + VIEW_HEIGHT);
        final var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + VIEW_WIDTH / 2F, y + VIEW_HEIGHT / 2F, 100);
        pose.scale(zoom, -zoom, zoom);
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw - 90 * menu.rotation()));
        pose.translate(-0.5, -centerY, -0.5);
        final var type = RenderType.debugQuads();
        final var vertices = graphics.bufferSource().getBuffer(type);
        mesh.render(pose, vertices);
        for (int level = 0; level <= 4; level++) outline(vertices, pose.last().pose(), level, 0x8B31CC75);
        outline(vertices, pose.last().pose(), centerY, 0xCCFF6D65);
        graphics.bufferSource().endBatch(type);
        pose.popPose();
        graphics.disableScissor();
    }

    private static void outline(final VertexConsumer vertices, final Matrix4f pose, final float y, final int color) {
        final float stroke = 0.012F;
        OBProjectorRendering.rectangle(vertices, pose, 0, 0, stroke, 1, y, color);
        OBProjectorRendering.rectangle(vertices, pose, 1 - stroke, 0, 1, 1, y, color);
        OBProjectorRendering.rectangle(vertices, pose, stroke, 0, 1 - stroke, stroke, y, color);
        OBProjectorRendering.rectangle(vertices, pose, stroke, 1 - stroke, 1 - stroke, 1, y, color);
    }

    private void drawPreviewMessage(final GuiGraphics graphics, final int x, final int y, final String key) {
        graphics.drawCenteredString(font, Component.translatable("gui.open_blocks.projector." + key), x + VIEW_WIDTH / 2,
                y + VIEW_HEIGHT / 2 - 4, 0xFFCCCBC9);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {}

    private boolean inPreview(final double x, final double y) {
        return x >= leftPos + 8 && y >= topPos + 8 && x < leftPos + 8 + VIEW_WIDTH && y < topPos + 8 + VIEW_HEIGHT;
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final int button) {
        if (inPreview(x, y) && button == 0) {
            dragging = true;
            return true;
        }
        if (inPreview(x, y) && button == 2) {
            yaw = 35;
            pitch = 28;
            verticalOffset = 0;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(final double x, final double y, final int button, final double dx, final double dy) {
        if (!dragging || button != 0) return super.mouseDragged(x, y, button, dx, dy);
        yaw = Mth.wrapDegrees(yaw + (float) dx * 2);
        pitch = Mth.clamp(pitch - (float) dy * 2, -89, 89);
        return true;
    }

    @Override
    public boolean mouseReleased(final double x, final double y, final int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double amount) {
        if (!inPreview(x, y)) return super.mouseScrolled(x, y, amount);
        verticalOffset = Mth.clamp(verticalOffset + (float) amount * 0.25F, -5, 10);
        return true;
    }
}
