package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.item.OBPaintCanItem;
import art.arcane.openblocks.item.OBPaintColors;
import art.arcane.openblocks.menu.OBPaintMixerMenu;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBItems;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class OBPaintMixerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int DURATION = 300;
    public static final int UNIT = 10_000;
    public static final int SLOT_COUNT = 5;
    private static final List<TagKey<Item>> DYE_TAGS = List.of(dye("cyan"), dye("magenta"), dye("yellow"), dye("black"));
    private static final int DEFAULT_COLOR = 0xFF0000;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override public boolean isItemValid(final int slot, final ItemStack stack) { return accepts(slot, stack); }
        @Override public int getSlotLimit(final int slot) { return slot == 0 ? 1 : super.getSlotLimit(slot); }
        @Override protected void onContentsChanged(final int slot) {
            if (slot == 0) {
                mixing = false;
                progress = 0;
                final ItemStack stack = getStackInSlot(0);
                if (stack.is(OBItems.PAINT_CAN.get())) color = OBPaintCanItem.color(stack);
                updateDisplay();
            }
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> access = LazyOptional.of(() -> items);
    private final int[] pigment = new int[DYE_TAGS.size()];
    private int[] cost = cost(DEFAULT_COLOR);
    private int color = DEFAULT_COLOR;
    private int progress;
    private boolean mixing;
    private boolean displayCan;
    private int displayStartColor;
    private long displayTick;

    public OBPaintMixerBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.PAINT_MIXER.get(), pos, state);
    }

    private static TagKey<Item> dye(final String name) {
        return TagKey.create(Registries.ITEM, OpenBlocks.id("legacy_ore_dict/dye_" + name));
    }

    public static boolean accepts(final int slot, final ItemStack stack) {
        return slot == 0 ? stack.is(Items.MILK_BUCKET) || stack.is(OBItems.PAINT_CAN.get())
                : slot > 0 && slot <= DYE_TAGS.size() && stack.is(DYE_TAGS.get(slot - 1));
    }

    public ItemStackHandler items() { return items; }
    public int progress() { return progress; }
    public int color() { return color; }
    public int pigment(final int channel) { return pigment[channel]; }
    public boolean mixing() { return mixing; }
    public boolean hasCan() { return displayCan; }

    public int animatedColor(final float partial) {
        if (!mixing || level == null) return displayStartColor;
        final float fraction = Mth.clamp((level.getGameTime() - displayTick + progress + partial) / DURATION, 0F, 1F);
        return FastColor.ARGB32.lerp(fraction, 0xFF000000 | displayStartColor, 0xFF000000 | color);
    }

    public boolean start(final int rgb) {
        if (rgb < 0 || rgb > 0xFFFFFF || mixing && rgb == color) return false;
        color = rgb;
        cost = cost(rgb);
        refillPigments();
        mixing = accepts(0, items.getStackInSlot(0)) && canAfford();
        progress = 0;
        setChanged();
        updateDisplay();
        return true;
    }

    private static int[] cost(final int rgb) {
        final int r = (rgb >> 16) & 255;
        final int g = (rgb >> 8) & 255;
        final int b = rgb & 255;
        final int max = Math.max(r, Math.max(g, b));
        if (max == 0) return new int[]{0, 0, 0, UNIT};
        return new int[]{
                Math.round((max - r) * (float) UNIT / max),
                Math.round((max - g) * (float) UNIT / max),
                Math.round((max - b) * (float) UNIT / max),
                Math.round((255 - max) * (float) UNIT / 255)
        };
    }

    private boolean canAfford() {
        for (int i = 0; i < pigment.length; i++) if (pigment[i] < cost[i]) return false;
        return true;
    }

    private void refillPigments() {
        for (int i = 0; i < pigment.length; i++) {
            if (pigment[i] <= UNIT && accepts(i + 1, items.getStackInSlot(i + 1))) {
                items.extractItem(i + 1, 1, false);
                pigment[i] += UNIT;
            }
        }
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final OBPaintMixerBlockEntity mixer) {
        mixer.refillPigments();
        if (!mixer.mixing) return;
        if (!accepts(0, mixer.items.getStackInSlot(0)) || !mixer.canAfford()) {
            mixer.mixing = false;
            mixer.progress = 0;
            mixer.updateDisplay();
        } else if (++mixer.progress >= DURATION) {
            for (int i = 0; i < mixer.pigment.length; i++) mixer.pigment[i] -= mixer.cost[i];
            mixer.items.setStackInSlot(0, OBPaintCanItem.create(mixer.color, OBPaintCanItem.FULL));
        }
        mixer.setChanged();
    }

    private void updateDisplay() {
        if (level == null || level.isClientSide()) return;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void dropInventory() {
        for (int i = 0; i < items.getSlots(); i++) Block.popResource(level, worldPosition, items.getStackInSlot(i));
    }

    public CompoundTag recipeSettings() {
        final var tag = new CompoundTag();
        tag.putInt("Color", color);
        tag.putIntArray("Pigment", pigment);
        return tag;
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new OBPaintMixerMenu(id, inventory, this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction side) {
        return !isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER ? access.cast() : super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        access.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        access = LazyOptional.of(() -> items);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) items.deserializeNBT(tag.getCompound("Inventory"));
        color = tag.contains("Color", Tag.TAG_ANY_NUMERIC) ? tag.getInt("Color") & 0xFFFFFF : DEFAULT_COLOR;
        cost = cost(color);
        progress = Mth.clamp(tag.getInt("Progress"), 0, DURATION - 1);
        mixing = tag.getBoolean("Mixing");
        final int[] saved = tag.getIntArray("Pigment");
        Arrays.fill(pigment, 0);
        for (int i = 0; i < Math.min(pigment.length, saved.length); i++) pigment[i] = Mth.clamp(saved[i], 0, UNIT * 2);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.merge(recipeSettings());
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putBoolean("Mixing", mixing);
    }

    @Override
    public CompoundTag getUpdateTag() {
        final var tag = new CompoundTag();
        final var can = items.getStackInSlot(0);
        tag.putBoolean("Can", accepts(0, can));
        tag.putInt("StartColor", OBPaintCanItem.color(can));
        tag.putInt("Color", color);
        tag.putInt("Progress", progress);
        tag.putBoolean("Mixing", mixing);
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        displayCan = tag.getBoolean("Can");
        displayStartColor = tag.getInt("StartColor");
        progress = tag.getInt("Progress");
        color = tag.getInt("Color");
        mixing = tag.getBoolean("Mixing");
        displayTick = level == null ? 0 : level.getGameTime();
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void onDataPacket(final net.minecraft.network.Connection connection, final ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
}
