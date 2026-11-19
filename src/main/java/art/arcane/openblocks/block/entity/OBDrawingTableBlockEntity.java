package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.item.OBGlyphItem;
import art.arcane.openblocks.item.OBStencilItem;
import art.arcane.openblocks.item.OBStencilPattern;
import art.arcane.openblocks.menu.OBDrawingTableMenu;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OBDrawingTableBlockEntity extends BlockEntity implements MenuProvider {

    public static final int MAX_PRINT_LENGTH = 256;
    private static final int PRINT_LIMIT = 32;
    private final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) { return stack.is(OBItems.UNPREPARED_STENCIL.get()); }

        @Override
        public void setChanged() {
            super.setChanged();
            OBDrawingTableBlockEntity.this.setChanged();
        }
    };
    private OBStencilPattern pattern = OBStencilPattern.CREEPER_FACE;
    private int glyph = OBGlyphItem.indexOf('A');
    private boolean glyphMode;
    private String remainingText = "";
    private int printRevision;

    public OBDrawingTableBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.DRAWING_TABLE.get(), pos, state);
    }

    public SimpleContainer input() { return input; }
    public OBStencilPattern pattern() { return pattern; }
    public int glyph() { return glyph; }
    public boolean glyphMode() { return glyphMode; }
    public String remainingText() { return remainingText; }
    public int printRevision() { return printRevision; }

    public void cycleMode() {
        glyphMode = !glyphMode;
        setChanged();
    }

    public void cycleSelection(final int step) {
        if (glyphMode) {
            do {
                glyph = Math.floorMod(glyph + step, OBGlyphItem.COUNT);
            } while (!OBGlyphItem.available(glyph));
        } else {
            pattern = OBStencilPattern.cycle(pattern.ordinal() + step);
        }
        setChanged();
    }

    public ItemStack result() {
        final ItemStack stock = input.getItem(0);
        if (!input.canPlaceItem(0, stock)) return ItemStack.EMPTY;
        final int count = stock.getCount();
        return glyphMode ? OBGlyphItem.create(glyph, count) : OBStencilItem.create(pattern, count);
    }

    public void consume(final int count) {
        if (count > 0) input.removeItem(0, count);
    }

    public void print(final String text) {
        if (!(level instanceof ServerLevel server) || !glyphMode || text.length() > MAX_PRINT_LENGTH) return;
        final int count = input.canPlaceItem(0, input.getItem(0)) ? Math.min(Math.min(input.getItem(0).getCount(), text.length()), PRINT_LIMIT) : 0;
        int printed = 0;
        while (printed < count) {
            final int index = OBGlyphItem.indexOf(text.charAt(printed));
            if (index < 0) break;
            final ItemStack reserved = input.removeItem(0, 1);
            if (reserved.isEmpty()) break;
            final var drop = new ItemEntity(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                    OBGlyphItem.create(index, 1));
            drop.setDefaultPickUpDelay();
            if (!server.addFreshEntity(drop)) {
                input.addItem(reserved);
                break;
            }
            printed++;
        }
        remainingText = text.substring(printed);
        printRevision++;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void dropInput() {
        if (level != null) Block.popResource(level, worldPosition, input.removeItemNoUpdate(0));
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new OBDrawingTableMenu(id, inventory, this);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Input", Tag.TAG_LIST)) input.fromTag(tag.getList("Input", Tag.TAG_COMPOUND));
        final OBStencilPattern selected = OBStencilPattern.byName(tag.getString("Pattern"));
        pattern = selected == null ? OBStencilPattern.CREEPER_FACE : selected;
        glyph = tag.contains("Glyph", Tag.TAG_ANY_NUMERIC) && OBGlyphItem.available(tag.getInt("Glyph")) ? tag.getInt("Glyph") : OBGlyphItem.indexOf('A');
        glyphMode = tag.getBoolean("GlyphMode");
        loadText(tag);
    }

    private void loadText(final CompoundTag tag) {
        final String text = tag.getString("RemainingText");
        remainingText = text.substring(0, Math.min(text.length(), MAX_PRINT_LENGTH));
        printRevision = tag.getInt("PrintRevision");
    }

    private void saveText(final CompoundTag tag) {
        tag.putString("RemainingText", remainingText);
        tag.putInt("PrintRevision", printRevision);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Input", input.createTag());
        tag.putString("Pattern", pattern.getSerializedName());
        tag.putInt("Glyph", glyph);
        tag.putBoolean("GlyphMode", glyphMode);
        saveText(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        final var tag = new CompoundTag();
        saveText(tag);
        return tag;
    }

    @Override public void handleUpdateTag(final CompoundTag tag) { loadText(tag); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(final net.minecraft.network.Connection connection, final ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
}
