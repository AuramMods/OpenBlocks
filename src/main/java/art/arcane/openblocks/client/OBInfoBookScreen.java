package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.client.OBInfoBookContents.Entry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;

public final class OBInfoBookScreen extends Screen {

    private static final ResourceLocation BOOK = OpenBlocks.id("textures/gui/book.png");
    private static final ResourceLocation COVER = OpenBlocks.id("textures/gui/bookimage.png");
    private static final String KEY = OBInfoBookContents.KEY;
    private static final int PAGE_HEIGHT = 180;
    private static final int MARGIN = 20;
    private static final int FIRST_LINE = 36;
    private static final int RECIPE_FIRST_LINE = 94;
    private static final int TEXT_BOTTOM = 143;
    private static final int GRID_COLUMNS = 5;
    private static final int ENTRIES_PER_INDEX = 20;
    private static final int INK = 0x38271D;
    private static final Page EMPTY_PAGE = new Page(Component.empty(), List.of(), null, List.of(), null);

    private record Bookmark(Component label, ItemStack icon, int destination) {}
    private record Action(Component label, Runnable run) {}
    private record Page(Component title, List<FormattedCharSequence> lines, Entry entry,
                        List<Bookmark> bookmarks, Action action) {}

    private final List<Page> pages = new ArrayList<>();
    private int spread;
    private int contents;
    private int pageWidth;
    private int left;
    private int top;

    private OBInfoBookScreen() {
        super(Component.translatable("item.open_blocks.info_book"));
    }

    public static void open() {
        final Minecraft game = Minecraft.getInstance();
        if (game.level != null) game.setScreen(new OBInfoBookScreen());
    }

    @Override
    protected void init() {
        pageWidth = Mth.clamp((width - 8) / 2, 124, 211);
        left = (width - 2 * pageWidth) / 2;
        top = (height - PAGE_HEIGHT) / 2;
        if (minecraft.level == null) return;
        buildPages();
        spread = Mth.clamp(spread, 0, pages.size() - 2);
        refreshWidgets();
    }

    private void buildPages() {
        pages.clear();
        pages.add(EMPTY_PAGE);
        pages.add(EMPTY_PAGE);
        final List<Bookmark> sections = new ArrayList<>();
        contents = pages.size();
        pages.add(EMPTY_PAGE);
        pages.addAll(article(text("welcome.title"), text("welcome.content"), null, null));
        alignSpread();
        sections.add(bookmark("blocks", pages.size()));
        addCatalogue("blocks", OBInfoBookContents.entries(minecraft, true));
        alignSpread();
        sections.add(bookmark("items", pages.size()));
        addCatalogue("items", OBInfoBookContents.entries(minecraft, false));
        alignSpread();
        sections.add(bookmark("misc", pages.size()));
        final int miscIndex = pages.size();
        pages.add(EMPTY_PAGE);
        final List<Bookmark> topics = new ArrayList<>();
        for (final String name : List.of("credits", "config", "restore_inv", "bkey", "unstable", "laststand", "flimflam")) {
            topics.add(bookmark(name + ".title", pages.size()));
            final Action action = name.equals("restore_inv") && minecraft.getSingleplayerServer() != null
                    ? new Action(text("save_folder"), this::openSaveFolder) : null;
            pages.addAll(article(text(name + ".title"), text(name + ".content"), null, action));
        }
        pages.set(miscIndex, indexPage("misc", topics));

        final var changes = OBInfoBookContents.changelogs(minecraft);
        if (!changes.isEmpty()) {
            alignSpread();
            sections.add(bookmark("changelogs", pages.size()));
            final int indexCount = (changes.size() + 5) / 6;
            final int firstDetail = pages.size() + indexCount;
            final List<Page> details = new ArrayList<>();
            final List<Bookmark> versions = new ArrayList<>();
            for (final var change : changes) {
                final Component version = text("version", change.version());
                versions.add(new Bookmark(version, ItemStack.EMPTY, firstDetail + details.size()));
                details.addAll(article(version, OBInfoBookContents.releaseNotes(change), null, null));
            }
            addIndexPages("changelogs", versions, 6);
            pages.addAll(details);
        }
        pages.set(contents, indexPage("contents", sections));
        alignSpread();
    }

    private void addCatalogue(final String section, final List<Entry> entries) {
        final int indexCount = Math.max(1, (entries.size() + ENTRIES_PER_INDEX - 1) / ENTRIES_PER_INDEX);
        final int firstDetail = pages.size() + indexCount;
        final List<Bookmark> bookmarks = new ArrayList<>();
        final List<Page> details = new ArrayList<>();
        for (final Entry entry : entries) {
            bookmarks.add(new Bookmark(entry.title(), entry.stack(), firstDetail + details.size()));
            final Action video = entry.video() != null && minecraft.options.chatLinks().get()
                    ? new Action(text("watch_video"), () -> handleComponentClicked(Style.EMPTY.withClickEvent(
                            new ClickEvent(ClickEvent.Action.OPEN_URL, entry.video())))) : null;
            details.addAll(article(entry.title(), Component.translatable(entry.description()), entry, video));
        }
        addIndexPages(section, bookmarks, ENTRIES_PER_INDEX);
        pages.addAll(details);
    }

    private void addIndexPages(final String title, final List<Bookmark> bookmarks, final int pageSize) {
        if (bookmarks.isEmpty()) pages.add(indexPage(title, List.of()));
        for (int start = 0; start < bookmarks.size(); start += pageSize) {
            pages.add(indexPage(title, bookmarks.subList(start, Math.min(start + pageSize, bookmarks.size()))));
        }
    }

    private List<Page> article(final Component title, final Component description, final Entry entry, final Action action) {
        final var text = font.split(description, pageWidth - 2 * MARGIN);
        final List<Page> article = new ArrayList<>();
        int start = 0;
        do {
            final boolean first = article.isEmpty();
            final int y = first && entry != null ? RECIPE_FIRST_LINE : FIRST_LINE;
            final int lineCapacity = 1 + (TEXT_BOTTOM - y - font.lineHeight) / (font.lineHeight + 1);
            final int end = Math.min(start + lineCapacity, text.size());
            article.add(new Page(title, text.subList(start, end), first ? entry : null, List.of(), first ? action : null));
            start = end;
        } while (start < text.size());
        return article;
    }

    private static Bookmark bookmark(final String key, final int page) {
        return new Bookmark(text(key), ItemStack.EMPTY, page);
    }

    private static Page indexPage(final String title, final List<Bookmark> bookmarks) {
        return new Page(text(title), List.of(), null, bookmarks, null);
    }

    private static Component text(final String key, final Object... arguments) {
        return Component.translatable(KEY + key, arguments);
    }

    private void alignSpread() {
        if ((pages.size() & 1) != 0) pages.add(EMPTY_PAGE);
    }

    private void refreshWidgets() {
        clearWidgets();
        setFocused(null);
        final PageButton previous = addRenderableWidget(new PageButton(left + MARGIN, top + 155, false, button -> turnTo(spread - 2), false));
        previous.setMessage(text("previous"));
        previous.visible = spread > 0;
        final PageButton next = addRenderableWidget(new PageButton(left + 2 * pageWidth - MARGIN - 23, top + 155, true, button -> turnTo(spread + 2), false));
        next.setMessage(text("next"));
        next.visible = spread + 2 < pages.size();
        addRenderableWidget(Button.builder(text("contents"), button -> turnTo(contents))
                .bounds(width / 2 - 45, top + PAGE_HEIGHT + 3, 90, 18).build());
        addPageWidgets(pages.get(spread), left);
        addPageWidgets(pages.get(spread + 1), left + pageWidth);
    }

    private void addPageWidgets(final Page page, final int x) {
        final int innerWidth = pageWidth - 2 * MARGIN;
        for (int i = 0; i < page.bookmarks().size(); i++) {
            final Bookmark bookmark = page.bookmarks().get(i);
            if (bookmark.icon().isEmpty()) {
                addRenderableWidget(Button.builder(bookmark.label(), button -> turnTo(bookmark.destination()))
                        .bounds(x + MARGIN, top + FIRST_LINE + i * 15, innerWidth, 14).build());
            } else {
                final int cell = innerWidth / GRID_COLUMNS;
                final int slotX = x + MARGIN + cell * (i % GRID_COLUMNS) + (cell - 18) / 2;
                addRenderableWidget(new ItemBookmark(slotX, top + FIRST_LINE + 22 * (i / GRID_COLUMNS), bookmark));
            }
        }
        if (page.action() != null) {
            addRenderableWidget(Button.builder(page.action().label(), button -> page.action().run().run())
                    .bounds(x + MARGIN, top + TEXT_BOTTOM, innerWidth, 15).build());
        }
    }

    private void turnTo(final int page) {
        final int target = Mth.clamp(page & ~1, 0, pages.size() - 2);
        if (target == spread) return;
        spread = target;
        refreshWidgets();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    private void openSaveFolder() {
        final var server = minecraft.getSingleplayerServer();
        if (server == null) return;
        final var folder = server.getWorldPath(LevelResource.ROOT).resolve("data").toFile();
        minecraft.setScreen(new ConfirmScreen(accepted -> {
            if (accepted) Util.getPlatform().openFile(folder);
            minecraft.setScreen(this);
        }, text("save_folder"), Component.literal(folder.getAbsolutePath())));
    }

    @Override
    public boolean keyPressed(final int key, final int scancode, final int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_PAGE_UP -> turnTo(spread - 2);
            case GLFW.GLFW_KEY_PAGE_DOWN -> turnTo(spread + 2);
            case GLFW.GLFW_KEY_HOME -> turnTo(0);
            case GLFW.GLFW_KEY_END -> turnTo(pages.size() - 2);
            default -> { return super.keyPressed(key, scancode, modifiers); }
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics);
        if (pages.isEmpty()) return;
        graphics.blit(BOOK, left, top, pageWidth, PAGE_HEIGHT, 211f, 0f, -211, 180, 256, 256);
        graphics.blit(BOOK, left + pageWidth, top, pageWidth, PAGE_HEIGHT, 0f, 0f, 211, 180, 256, 256);
        final ItemStack leftHover = renderPage(graphics, spread, left, mouseX, mouseY);
        final ItemStack rightHover = renderPage(graphics, spread + 1, left + pageWidth, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        final ItemStack hovered = leftHover.isEmpty() ? rightHover : leftHover;
        if (!hovered.isEmpty()) graphics.renderTooltip(font, hovered, mouseX, mouseY);
    }

    private ItemStack renderPage(final GuiGraphics graphics, final int index, final int x, final int mouseX, final int mouseY) {
        if (index == 1) graphics.blit(COVER, x + (pageWidth - 95) / 2, top + 53, 0f, 0f, 95, 70, 128, 128);
        final Page page = pages.get(index);
        final int center = x + pageWidth / 2;
        final int maxWidth = pageWidth - 2 * MARGIN;
        final float titleScale = Math.min(1f, (float) maxWidth / Math.max(1, font.width(page.title())));
        graphics.pose().pushPose();
        graphics.pose().translate(center, top + 19, 0);
        graphics.pose().scale(titleScale, titleScale, 1f);
        graphics.drawString(font, page.title(), -font.width(page.title()) / 2, 0, INK, false);
        graphics.pose().popPose();

        int y = top + (page.entry() == null ? FIRST_LINE : RECIPE_FIRST_LINE);
        for (final FormattedCharSequence line : page.lines()) {
            graphics.drawString(font, line, x + MARGIN, y, INK, false);
            y += font.lineHeight + 1;
        }
        final Component number = text("page", index + 1, pages.size());
        graphics.drawString(font, number, center - font.width(number) / 2, top + 159, INK, false);
        return page.entry() == null ? ItemStack.EMPTY : renderRecipe(graphics, page.entry(), x, mouseX, mouseY);
    }

    private ItemStack renderRecipe(final GuiGraphics graphics, final Entry entry, final int x, final int mouseX, final int mouseY) {
        final int area = Math.min(144, pageWidth - 2 * MARGIN);
        final int start = x + (pageWidth - area) / 2;
        final var recipe = entry.recipe();
        if (recipe == null) {
            return renderStack(graphics, entry.stack(), x + pageWidth / 2 - 8, top + 49, mouseX, mouseY);
        }

        graphics.blit(BOOK, start, top + 31, 0, 180, 56, 56);
        graphics.blit(BOOK, start + 59, top + 51, area - 79, 15, 60f, 198f, 48, 15, 256, 256);
        final var result = recipe.getResultItem(minecraft.level.registryAccess());
        ItemStack hovered = renderStack(graphics, result, start + area - 17, top + 50, mouseX, mouseY);
        final int craftingWidth = OBInfoBookContents.recipeWidth(recipe);
        final long second = Util.getMillis() / 1000L;
        for (int i = 0; i < Math.min(recipe.getIngredients().size(), 3 * craftingWidth); i++) {
            final ItemStack[] choices = recipe.getIngredients().get(i).getItems();
            if (choices.length == 0) continue;
            final ItemStack candidate = choices[(int) (second % choices.length)];
            final ItemStack slot = renderStack(graphics, candidate, start + 1 + 18 * (i % craftingWidth),
                    top + 32 + 18 * (i / craftingWidth), mouseX, mouseY);
            if (!slot.isEmpty()) hovered = slot;
        }
        return hovered;
    }

    private ItemStack renderStack(final GuiGraphics graphics, final ItemStack stack, final int x, final int y, final int mouseX, final int mouseY) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16 ? stack : ItemStack.EMPTY;
    }

    private final class ItemBookmark extends Button {
        private final ItemStack icon;

        private ItemBookmark(final int x, final int y, final Bookmark bookmark) {
            super(x, y, 18, 18, bookmark.label(), button -> turnTo(bookmark.destination()), DEFAULT_NARRATION);
            this.icon = bookmark.icon();
            setTooltip(Tooltip.create(bookmark.label()));
        }

        @Override
        protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
            final int x = getX();
            final int y = getY();
            if (isHoveredOrFocused()) graphics.fill(x, y, x + 18, y + 18, 0xFFDCC59D);
            graphics.renderItem(icon, x + 1, y + 1);
        }
    }
}
