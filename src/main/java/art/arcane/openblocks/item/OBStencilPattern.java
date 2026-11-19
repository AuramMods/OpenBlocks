package art.arcane.openblocks.item;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.util.StringRepresentable;

public enum OBStencilPattern implements StringRepresentable {

    CREEPER_FACE("0000 0000 3C3C 3C3C 3C3C 3C3C 03C0 03C0 0FF0 0FF0 0FF0 0FF0 0C30 0C30 0000 0000"),
    BORDER("0003 0003 0003 0003 0003 0003 0003 0003 0003 0003 0003 0003 0003 0003 0003 0003"),
    STRIPES("5555 5555 5555 5555 5555 5555 5555 5555 5555 5555 5555 5555 5555 5555 5555 5555"),
    CORNER("0000 0000 00FC 00FC 000C 000C 000C 000C 0000 0000 0000 0000 0000 0000 0000 0000"),
    CORNER2("00FF 007F 003F 001F 000F 0007 0003 0001 0000 0000 0000 0000 0000 0000 0000 0000"),
    CORNER3("0000 00FE 00FE 0006 00F6 00F6 0036 0036 0000 0000 0000 0000 0000 0000 0000 0000"),
    HOLE("0000 0000 0000 0000 0FF0 0FF0 0FF0 0FF0 0FF0 0FF0 0FF0 0FF0 0000 0000 0000 0000"),
    SPIRAL("0000 7FFF 4000 5FFE 5002 57FA 540A 55EA 552A 542A 57EA 500A 5FFA 4002 7FFE 0000"),
    THICKSTRIPES("3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C 3C3C"),
    SPLAT("6106 E387 8183 1810 1C38 8438 F030 C000 0100 1980 41C6 C1CE 008E 1800 3830 3070"),
    STORAGE("0000 0000 0000 1FF8 1008 1008 1188 1FF8 1188 1008 1008 1008 1FF8 0000 0000 0000"),
    HEART("0000 0000 1C38 2244 4182 4002 4002 4002 2004 1008 0810 0420 0240 0180 0000 0000"),
    HEART2("0000 0000 0000 1C38 3E7C 3FFC 3FFC 3FFC 1FF8 0FF0 07E0 03C0 0180 0000 0000 0000"),
    MUSIC("0000 0000 1F80 1FFC 10FC 1004 1004 1004 1004 3004 700C 701C 201C 0008 0000 0000"),
    BALLOON("0000 03C0 07E0 0FF0 1FF8 1FF8 1FF8 1FF8 1FF8 0FF0 07E0 03C0 0080 01C0 0200 1C00");

    public static final int SIZE = 16;
    private static final OBStencilPattern[] PATTERNS = values();
    private final String id = name().toLowerCase(Locale.ROOT);
    private final int[] rows = new int[SIZE];

    OBStencilPattern(final String encoded) {
        final String[] masks = encoded.split(" ");
        for (int row = 0; row < SIZE; row++) rows[row] = Integer.parseInt(masks[row], 16);
    }

    public boolean isOpen(final int x, final int y) { return (rows[y] & (1 << x)) != 0; }

    public static OBStencilPattern cycle(final int index) { return PATTERNS[Math.floorMod(index, PATTERNS.length)]; }

    @Nullable
    public static OBStencilPattern byName(final String id) {
        for (final OBStencilPattern pattern : PATTERNS) if (pattern.id.equals(id)) return pattern;
        return null;
    }

    @Override public String getSerializedName() { return id; }
}
