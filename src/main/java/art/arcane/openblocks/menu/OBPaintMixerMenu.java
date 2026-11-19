package art.arcane.openblocks.menu;

import art.arcane.openblocks.block.entity.OBPaintMixerBlockEntity;
import art.arcane.openblocks.registry.OBMenuTypes;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class OBPaintMixerMenu extends AbstractContainerMenu {

    private static final int DATA_COUNT = 8;
    private static final int SLOT_COUNT = OBPaintMixerBlockEntity.SLOT_COUNT;
    private final BlockPos pos;
    @Nullable private final OBPaintMixerBlockEntity mixer;
    private final ContainerData data;

    public OBPaintMixerMenu(final int id, final Inventory player, final FriendlyByteBuf buffer) {
        this(id, player, buffer.readBlockPos(), null, new ItemStackHandler(SLOT_COUNT) {
            @Override public boolean isItemValid(final int slot, final ItemStack stack) { return OBPaintMixerBlockEntity.accepts(slot, stack); }
            @Override public int getSlotLimit(final int slot) { return slot == 0 ? 1 : super.getSlotLimit(slot); }
        }, new SimpleContainerData(DATA_COUNT));
    }

    public OBPaintMixerMenu(final int id, final Inventory player, final OBPaintMixerBlockEntity mixer) {
        this(id, player, mixer.getBlockPos(), mixer, mixer.items(), new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case 0 -> mixer.color() >>> 16;
                    case 1 -> mixer.color() & 0xFFFF;
                    case 2 -> mixer.progress();
                    case 3 -> mixer.mixing() ? 1 : 0;
                    default -> mixer.pigment(index - 4);
                };
            }

            @Override public int getCount() { return DATA_COUNT; }
            @Override public void set(final int index, final int value) {}
        });
    }

    private OBPaintMixerMenu(final int id, final Inventory player, final BlockPos pos, @Nullable final OBPaintMixerBlockEntity mixer,
            final ItemStackHandler items, final ContainerData data) {
        super(OBMenuTypes.PAINT_MIXER.get(), id);
        this.pos = pos;
        this.mixer = mixer;
        this.data = data;
        addSlot(new SlotItemHandler(items, 0, 135, 24) {
            @Override
            public int getMaxStackSize(final ItemStack stack) { return 1; }
        });
        for (int i = 0; i < 4; i++) addSlot(new SlotItemHandler(items, 1 + i, 124 + 24 * (i % 2), 79 + 21 * (i / 2)));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(player, col + row * 9 + 9, 8 + col * 18, 127 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 185));
        addDataSlots(data);
    }

    public int color() { return ((data.get(0) & 255) << 16) | (data.get(1) & 0xFFFF); }
    public int progress() { return data.get(2); }
    public boolean mixing() { return data.get(3) != 0; }
    public int pigment(final int channel) { return data.get(4 + channel); }

    public void requestColor(final Player player, final int rgb) {
        if (mixer != null && stillValid(player)) mixer.start(rgb);
    }

    @Override
    public boolean stillValid(final Player player) {
        return mixer == null || !mixer.isRemoved() && player.level() == mixer.getLevel()
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64 && player.level().getBlockEntity(pos) == mixer;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        final Slot slot = getSlot(index);
        final ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        final ItemStack original = stack.copy();
        if (index < SLOT_COUNT ? !moveItemStackTo(stack, SLOT_COUNT, slots.size(), true) : !moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
