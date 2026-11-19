package art.arcane.openblocks.item;

import javax.annotation.Nullable;
import net.minecraft.world.item.DyeColor;

public final class OBPaintColors {

    public static final int WHITE = 0xFFFFFF;
    private static final DyeColor[] DYES = DyeColor.values();

    private OBPaintColors() {}

    public static int palette(final DyeColor color) {
        return color.getFireworkColor();
    }

    @Nullable
    public static DyeColor nearestDye(final int rgb) {
        int best = 3 * 16 * 16;
        DyeColor result = null;
        final int red = (rgb >> 16) & 255;
        for (int index = DYES.length - 1; index >= 0; index--) {
            final int candidate = palette(DYES[index]);
            final int otherRed = (candidate >> 16) & 255;
            final int meanRed = (red + otherRed) / 2;
            final int r = red - otherRed;
            final int g = ((rgb >> 8) & 255) - ((candidate >> 8) & 255);
            final int b = (rgb & 255) - (candidate & 255);
            final int distance = ((512 + meanRed) * r * r >> 8) + 4 * g * g + ((767 - meanRed) * b * b >> 8);
            if (distance < best) {
                best = distance;
                result = DYES[index];
            }
        }
        return result;
    }
}
