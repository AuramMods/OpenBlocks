package art.arcane.openblocks.item;

import art.arcane.openblocks.api.OBPaintableBlock;
import art.arcane.openblocks.block.OBCanvasBlock;
import art.arcane.openblocks.block.OBElevatorBlock;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class OBPaintBrushItem extends Item {

    public static final int MAX_USES = 24;
    private static final String COLOR = "color";
    private static final List<String> COLOR_FAMILIES = List.of("wool", "carpet", "stained_glass", "stained_glass_pane",
            "terracotta", "concrete", "concrete_powder");

    public OBPaintBrushItem() {
        super(new Properties().durability(MAX_USES).setNoRepair());
    }

    public static int color(final ItemStack stack) {
        final var tag = stack.getTag();
        return tag != null && tag.contains(COLOR, Tag.TAG_ANY_NUMERIC) ? tag.getInt(COLOR) & 0xFFFFFF : OBCanvasBlockEntity.UNPAINTED;
    }

    public static void loadPaint(final ItemStack stack, final int rgb) {
        stack.getOrCreateTag().putInt(COLOR, rgb & 0xFFFFFF);
        stack.setDamageValue(0);
    }

    public static ItemStack create(final int rgb) {
        final var stack = new ItemStack(OBItems.PAINTBRUSH.get());
        loadPaint(stack, rgb);
        return stack;
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> lines, final TooltipFlag flags) {
        final int rgb = color(stack);
        if (rgb != OBCanvasBlockEntity.UNPAINTED) lines.add(Component.literal(String.format(Locale.ROOT, "#%06X", rgb)));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final ItemStack stack = context.getItemInHand();
        final Player player = context.getPlayer();
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final Direction face = context.getClickedFace();
        final int rgb = color(stack);
        if (context.getHand() != InteractionHand.MAIN_HAND || player == null || rgb == OBCanvasBlockEntity.UNPAINTED) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)) return InteractionResult.FAIL;
        final BlockState state = level.getBlockState(pos);
        final Direction[] sides = player.isShiftKeyDown() ? Direction.values() : new Direction[]{face};
        final boolean changed;
        if (state.getBlock() instanceof OBPaintableBlock paintable) {
            changed = level.isClientSide() || paintable.applyPaint(level, pos, rgb, sides);
        } else {
            final BlockState recolored = recolor(state, OBPaintColors.nearestDye(rgb));
            if (recolored != state) {
                changed = level.isClientSide() || level.setBlockAndUpdate(pos, recolored);
            } else if ((!OBServerConfig.SPEC.isLoaded() || OBServerConfig.CANVAS_REPLACE.get()) && OBCanvasBlock.canWrap(level, pos, state)) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                final OBCanvasBlockEntity canvas = OBCanvasBlock.wrap(level, pos, state);
                changed = canvas != null && canvas.paint(rgb, sides);
            } else {
                changed = false;
            }
        }
        if (!changed) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.1F, 0.8F);
            if (!player.getAbilities().instabuild && stack.hurt(1, level.random, player instanceof net.minecraft.server.level.ServerPlayer server ? server : null)) {
                stack.removeTagKey(COLOR);
                stack.setDamageValue(0);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static BlockState recolor(final BlockState source, @Nullable final DyeColor dye) {
        if (dye == null) return source;
        if (source.getBlock() instanceof OBElevatorBlock) return source.setValue(OBElevatorBlock.COLOR, dye);
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(source.getBlock());
        if (!id.getNamespace().equals("minecraft")) return source;
        final String name = id.getPath();
        for (final String family : COLOR_FAMILIES) {
            for (final DyeColor current : DyeColor.values()) {
                if (!name.equals(current.getName() + "_" + family)) continue;
                if (current == dye) return source;
                BlockState result = BuiltInRegistries.BLOCK.get(ResourceLocation.tryBuild("minecraft", dye.getName() + "_" + family)).defaultBlockState();
                for (final Property<?> property : source.getProperties()) result = copyProperty(source, result, property);
                return result;
            }
        }
        return source;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(final BlockState from, final BlockState to, final Property<T> property) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }
}
