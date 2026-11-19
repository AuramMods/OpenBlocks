package art.arcane.openblocks.item;

import art.arcane.openblocks.block.OBCanvasBlock;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import art.arcane.openblocks.registry.OBItems;
import javax.annotation.Nullable;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public final class OBStencilItem extends Item {

    public OBStencilItem() { super(new Properties()); }

    public static ItemStack create(final OBStencilPattern pattern, final int count) {
        final ItemStack stack = new ItemStack(OBItems.STENCIL.get(), count);
        stack.getOrCreateTag().putString("Pattern", pattern.getSerializedName());
        return stack;
    }

    @Nullable
    public static OBStencilPattern pattern(final ItemStack stack) {
        final var tag = stack.getTag();
        return tag != null && tag.contains("Pattern", Tag.TAG_STRING)
                ? OBStencilPattern.byName(tag.getString("Pattern")) : OBStencilPattern.CREEPER_FACE;
    }

    public static Component name(final OBStencilPattern pattern) {
        return Component.translatable("stencil.open_blocks." + pattern.getSerializedName());
    }

    @Override
    public Component getName(final ItemStack stack) {
        final OBStencilPattern pattern = pattern(stack);
        return pattern == null ? super.getName(stack) : Component.translatable("item.open_blocks.stencil.pattern", name(pattern));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final var player = context.getPlayer();
        final var level = context.getLevel();
        final var pos = context.getClickedPos();
        final var face = context.getClickedFace();
        final var stack = context.getItemInHand();
        final OBStencilPattern pattern = pattern(stack);
        if (pattern == null || player == null || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)) {
            return InteractionResult.PASS;
        }
        final var state = level.getBlockState(pos);
        OBCanvasBlockEntity canvas = level.getBlockEntity(pos) instanceof OBCanvasBlockEntity placed ? placed : null;
        if (canvas == null) {
            if (!OBCanvasBlock.canWrap(level, pos, state)) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            canvas = OBCanvasBlock.wrap(level, pos, state);
        }
        if (canvas == null || canvas.face(face).cover() != null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            canvas.attach(face, pattern);
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
