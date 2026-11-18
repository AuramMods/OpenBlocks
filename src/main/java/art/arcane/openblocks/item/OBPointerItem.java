package art.arcane.openblocks.item;

import art.arcane.openblocks.block.entity.OBCannonBlockEntity;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class OBPointerItem extends Item {

    private static final String SOURCE = "Cannon";

    public OBPointerItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        point(context.getLevel(), player, context.getItemInHand(), context.getClickedPos());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && player.pick(10, 1, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            point(level, player, stack, hit.getBlockPos());
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static void point(final Level level, final Player player, final ItemStack stack, final BlockPos target) {
        if (level.isClientSide || !level.hasChunkAt(target)) return;

        final String dimension = level.dimension().location().toString();
        if (level.getBlockEntity(target) instanceof OBCannonBlockEntity) {
            final CompoundTag source = new CompoundTag();
            source.putString("Dimension", dimension);
            source.putLong("Pos", target.asLong());
            stack.addTagElement(SOURCE, source);
            player.displayClientMessage(Component.translatable("message.open_blocks.cannon.selected"), true);
            return;
        }

        final CompoundTag source = stack.getTagElement(SOURCE);
        if (source == null || !source.contains("Pos", Tag.TAG_LONG) || !dimension.equals(source.getString("Dimension"))) return;
        final BlockPos origin = BlockPos.of(source.getLong("Pos"));
        if (!level.hasChunkAt(origin) || !(level.getBlockEntity(origin) instanceof OBCannonBlockEntity cannon)) return;
        final Component message = cannon.aimAt(target)
                ? Component.translatable("message.open_blocks.cannon.aimed", target.getX(), target.getY(), target.getZ())
                : Component.translatable("message.open_blocks.cannon.out_of_range");
        player.displayClientMessage(message, true);
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip, final TooltipFlag flags) {
        tooltip.add(Component.translatable("tooltip.open_blocks.pointer").withStyle(ChatFormatting.GRAY));
    }
}
