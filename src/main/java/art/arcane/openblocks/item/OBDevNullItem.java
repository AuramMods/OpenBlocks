package art.arcane.openblocks.item;

import art.arcane.openblocks.client.OBDevNullRendering;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.menu.OBDevNullMenu;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.NetworkHooks;

public final class OBDevNullItem extends Item {

    public static final int STACK_LIMIT = 5;
    private static final String CONTENTS_TAG = "Contents";

    public OBDevNullItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack getStored(final ItemStack device) {
        final CompoundTag data = device.getTagElement(CONTENTS_TAG);
        return data == null ? ItemStack.EMPTY : ItemStack.of(data);
    }

    public static void setStored(final ItemStack device, final ItemStack stored) {
        if (stored.isEmpty()) device.removeTagKey(CONTENTS_TAG);
        else device.addTagElement(CONTENTS_TAG, stored.save(new CompoundTag()));
    }

    public static Contents contents(final ItemStack device) {
        ItemStack current = device;
        int depth = 0;
        while (current.getItem() instanceof OBDevNullItem && depth < STACK_LIMIT) {
            current = getStored(current);
            depth++;
        }
        return new Contents(depth, depth == STACK_LIMIT ? ItemStack.EMPTY : current);
    }

    public record Contents(int depth, ItemStack item) {}

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);

        if (player instanceof ServerPlayer serverPlayer && (player.isShiftKeyDown() || !OBServerConfig.DEV_NULL_SNEAK_GUI.get())) {
            final int hotbarSlot = player.getInventory().selected;
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, owner) -> new OBDevNullMenu(id, inventory, hotbarSlot),
                    stack.getHoverName()), data -> data.writeVarInt(hotbarSlot));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        final ItemStack device = context.getItemInHand();
        final ItemStack stored = getStored(device);
        if (player == null || !(stored.getItem() instanceof BlockItem blockItem) || !player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }

        final InteractionHand hand = context.getHand();
        final var hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        player.setItemInHand(hand, stored);
        try {
            // Use the outer stack's Forge placement transaction so cancellation restores both the world and our contents.
            final InteractionResult result = blockItem.useOn(new UseOnContext(player, hand, hit));
            if (result.consumesAction()) setStored(device, player.getItemInHand(hand));
            return result;
        } finally {
            player.setItemInHand(hand, device);
        }
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip,
            final TooltipFlag flag) {
        final Contents contents = contents(stack);
        if (contents.depth() == STACK_LIMIT) {
            tooltip.add(Component.literal("╔"));
            tooltip.add(Component.literal("║ ").append(Component.literal("WHOOPS").withStyle(ChatFormatting.OBFUSCATED)));
            tooltip.add(Component.literal("╚"));
        } else if (!contents.item().isEmpty()) {
            final ItemStack stored = contents.item();
            final List<Component> details = stored.getTooltipLines(null, flag);
            tooltip.add(Component.literal("╔"));
            tooltip.add(Component.literal("║ " + stored.getCount() + " × ").append(stored.getHoverName()));
            for (int i = 1; i < details.size(); i++) tooltip.add(Component.literal("║ ").append(details.get(i)));
            tooltip.add(Component.literal("╚"));
        }
    }

    @Override
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new OBDevNullRendering();
                return renderer;
            }
        });
    }
}
