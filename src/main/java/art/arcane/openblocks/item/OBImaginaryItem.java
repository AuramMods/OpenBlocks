package art.arcane.openblocks.item;

import art.arcane.openblocks.block.OBImaginaryBlock.Shape;
import art.arcane.openblocks.block.entity.OBImaginaryBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class OBImaginaryItem extends BlockItem {

    public static final float CRAFTING_COST = 1.0F;

    public OBImaginaryItem(final Block block) {
        super(block, new Properties().stacksTo(1));
    }

    public enum Mode {
        BLOCK(Shape.BLOCK, 1.0F),
        PANEL(Shape.PANEL, 0.5F),
        HALF_PANEL(Shape.HALF_PANEL, 0.5F),
        STAIRS(Shape.STAIRS, 0.75F),
        INVERTED_BLOCK(Shape.BLOCK, 1.5F),
        INVERTED_PANEL(Shape.PANEL, 1.0F),
        INVERTED_HALF_PANEL(Shape.HALF_PANEL, 1.0F),
        INVERTED_STAIRS(Shape.STAIRS, 1.25F);

        private static final Mode[] VALUES = values();
        private final Shape shape;
        private final float cost;
        private final String translation;

        Mode(final Shape shape, final float cost) {
            this.shape = shape;
            this.cost = cost;
            translation = "item.open_blocks.imaginary.mode." + name().toLowerCase(Locale.ROOT);
        }

        public boolean inverted() { return ordinal() >= INVERTED_BLOCK.ordinal(); }

        private Mode next() { return VALUES[(ordinal() + 1) % VALUES.length]; }

        public static Mode of(final Shape shape, final boolean inverted) {
            return VALUES[shape.ordinal() + (inverted ? INVERTED_BLOCK.ordinal() : 0)];
        }
    }

    public static float defaultUses() {
        return OBServerConfig.SPEC.isLoaded() ? OBServerConfig.IMAGINARY_USES.get().floatValue() : 10.0F;
    }

    public static ItemStack create(@Nullable final Integer rgb, final Mode mode, final float uses) {
        final var stack = new ItemStack(OBItems.IMAGINARY.get());
        final var tag = stack.getOrCreateTag();
        if (rgb != null) tag.putInt("Color", rgb & 0xFFFFFF);
        tag.putByte("Mode", (byte) mode.ordinal());
        tag.putFloat("Uses", uses);
        return stack;
    }

    @Nullable
    public static Integer color(final ItemStack stack) {
        final var tag = stack.getTag();
        return tag != null && tag.contains("Color", Tag.TAG_ANY_NUMERIC) ? tag.getInt("Color") & 0xFFFFFF : null;
    }

    public static Mode mode(final ItemStack stack) {
        final var tag = stack.getTag();
        final int value = tag == null ? 0 : tag.getByte("Mode");
        return value >= 0 && value < Mode.VALUES.length ? Mode.VALUES[value] : Mode.BLOCK;
    }

    public static float uses(final ItemStack stack) {
        final var tag = stack.getTag();
        if (tag == null || !tag.contains("Uses", Tag.TAG_ANY_NUMERIC)) return defaultUses();
        final float uses = tag.getFloat("Uses");
        return Float.isFinite(uses) ? Math.max(0.0F, uses) : 0.0F;
    }

    @Override
    public InteractionResult place(final BlockPlaceContext context) {
        final var stack = context.getItemInHand();
        final var player = context.getPlayer();
        final float remaining = uses(stack) - mode(stack).cost;
        if (remaining < 0) return InteractionResult.FAIL;

        final int count = stack.getCount();
        final var result = super.place(context);
        if (result.consumesAction() && (player == null || !player.getAbilities().instabuild)) {
            if (remaining > 0) {
                stack.setCount(count);
                stack.getOrCreateTag().putFloat("Uses", remaining);
            } else if (!stack.isEmpty()) {
                // A pre-port inventory can contain stacked pencils. The next pencil has its own charges.
                stack.getOrCreateTag().putFloat("Uses", defaultUses());
            }
        }
        return result;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(final BlockPos pos, final Level level, @Nullable final Player player,
            final ItemStack stack, final BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof OBImaginaryBlockEntity imaginary)) return false;

        final var mode = mode(stack);
        imaginary.setDesign(color(stack), mode.shape, mode.inverted());
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final var stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && player.isShiftKeyDown()) {
            final var mode = mode(stack).next();
            stack.getOrCreateTag().putByte("Mode", (byte) mode.ordinal());
            player.displayClientMessage(Component.translatable("item.open_blocks.imaginary.mode",
                    Component.translatable(mode.translation)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public boolean hasCraftingRemainingItem(final ItemStack stack) { return true; }

    @Override
    public ItemStack getCraftingRemainingItem(final ItemStack stack) {
        final float uses = uses(stack) - CRAFTING_COST;
        if (uses <= 0) return ItemStack.EMPTY;
        final var remainder = stack.copyWithCount(1);
        remainder.getOrCreateTag().putFloat("Uses", uses);
        return remainder;
    }

    @Override
    public String getDescriptionId(final ItemStack stack) {
        return color(stack) == null ? "item.open_blocks.magic_pencil" : "item.open_blocks.magic_crayon";
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> lines, final TooltipFlag flag) {
        lines.add(Component.translatable("item.open_blocks.imaginary.uses", String.format(Locale.ROOT, "%.2f", uses(stack))));
        final var color = color(stack);
        if (color != null) lines.add(OBImaginationGlasses.colorDescription(color));
        lines.add(Component.translatable("item.open_blocks.imaginary.mode", Component.translatable(mode(stack).translation)));
    }
}
