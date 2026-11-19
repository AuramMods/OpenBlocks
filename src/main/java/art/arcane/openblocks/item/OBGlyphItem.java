package art.arcane.openblocks.item;

import art.arcane.openblocks.client.OBGlyphRendering;
import art.arcane.openblocks.entity.OBGlyph;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class OBGlyphItem extends Item {

    // Keep the original font-sheet order for the character selector and existing glyph IDs.
    private static final String CHARACTERS = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";
    public static final String CREATIVE_CHARACTERS = " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static final int DEFAULT = CHARACTERS.indexOf('?');
    public static final int COUNT = CHARACTERS.length();

    public OBGlyphItem() { super(new Properties()); }

    public static boolean available(final int index) {
        if (index < 0 || index >= COUNT) return false;
        final char character = CHARACTERS.charAt(index);
        return character == ' ' || character != 0 && !Character.isWhitespace(character);
    }

    public static int indexOf(final char character) {
        final int index = CHARACTERS.indexOf(character);
        return available(index) ? index : -1;
    }

    public static char character(final int index) {
        return available(index) ? CHARACTERS.charAt(index) : '?';
    }

    public static int index(final ItemStack stack) {
        final var tag = stack.getTag();
        final int index = tag != null && tag.contains("CharIndex", Tag.TAG_ANY_NUMERIC) ? tag.getInt("CharIndex") : DEFAULT;
        return available(index) ? index : DEFAULT;
    }

    public static ItemStack create(final int index, final int count) {
        final var stack = new ItemStack(OBItems.GLYPH.get(), count);
        stack.getOrCreateTag().putInt("CharIndex", available(index) ? index : DEFAULT);
        return stack;
    }

    @Override
    public Component getName(final ItemStack stack) {
        final int index = index(stack);
        return index == DEFAULT ? super.getName(stack) : Component.translatable("item.open_blocks.glyph.character", character(index));
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> lines, final TooltipFlag flags) {
        final char character = character(index(stack));
        lines.add(Component.literal(String.format(Locale.ROOT, "%04X (%s)", (int) character, Character.getName(character))));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final var player = context.getPlayer();
        final var level = context.getLevel();
        final var side = context.getClickedFace();
        final var wall = context.getClickedPos();
        final var stack = context.getItemInHand();
        final var anchor = wall.relative(side);
        if (player == null || !side.getAxis().isHorizontal() || !level.mayInteract(player, wall)
                || !level.mayInteract(player, anchor) || !player.mayUseItemAt(anchor, side, stack)) return InteractionResult.FAIL;
        final var glyph = new OBGlyph(OBEntities.GLYPH.get(), level);
        final var hit = context.getClickLocation();
        final double horizontal = side.getAxis() == Direction.Axis.Z ? hit.x - wall.getX() : hit.z - wall.getZ();
        glyph.mount(anchor, side, index(stack), pixel(horizontal), pixel(hit.y - wall.getY()));
        if (!glyph.survives()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (!level.addFreshEntity(glyph)) return InteractionResult.FAIL;
            glyph.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, anchor);
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static int pixel(final double local) {
        return Mth.clamp(Mth.floor(local * 16), 0, 16);
    }

    @Override
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new OBGlyphRendering.ItemRenderer();
                return renderer;
            }
        });
    }
}
