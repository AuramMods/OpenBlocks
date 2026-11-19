package art.arcane.openblocks.guide;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OBGuideControls {

    public record Button(String name, AABB bounds) {}

    private static final List<Button> BUTTONS = List.of(
            button("rotate_ccw", 1, 15.9, 3, 5, 16.1, 14),
            button("rotate_cw", 11, 15.9, 3, 15, 16.1, 14),
            button("rotate_ccw", 11, -0.1, 2, 15, 0.1, 13),
            button("rotate_cw", 1, -0.1, 2, 5, 0.1, 13),
            button("inc_mode", 5, -0.1, 11, 11, 0.1, 14),
            button("dec_mode", 5, -0.1, 5, 11, 0.1, 8),
            button("inc_mode", 5, 15.9, 2, 11, 16.1, 5),
            button("dec_mode", 5, 15.9, 8, 11, 16.1, 11),
            button("inc_pos_y", -0.1, 12, 9, 0.1, 15, 12),
            button("dec_pos_y", -0.1, 12, 4, 0.1, 15, 7),
            button("dec_neg_y", -0.1, 1, 9, 0.1, 4, 12),
            button("inc_neg_y", -0.1, 1, 4, 0.1, 4, 7),
            button("inc_pos_y", 15.9, 12, 4, 16.1, 15, 7),
            button("dec_pos_y", 15.9, 12, 9, 16.1, 15, 12),
            button("inc_neg_y", 15.9, 1, 9, 16.1, 4, 12),
            button("dec_neg_y", 15.9, 1, 4, 16.1, 4, 7),
            button("inc_pos_y", 4, 12, -0.1, 7, 15, 0.1),
            button("dec_pos_y", 9, 12, -0.1, 12, 15, 0.1),
            button("inc_neg_y", 9, 1, -0.1, 12, 4, 0.1),
            button("dec_neg_y", 4, 1, -0.1, 7, 4, 0.1),
            button("inc_neg_y", 4, 1, 15.9, 7, 4, 16.1),
            button("dec_neg_y", 9, 1, 15.9, 12, 4, 16.1),
            button("inc_pos_y", 9, 12, 15.9, 12, 15, 16.1),
            button("dec_pos_y", 4, 12, 15.9, 7, 15, 16.1),
            button("copy_pos_y", -0.1, 10, 6, 0.1, 12, 10),
            button("copy_neg_y", -0.1, 4, 6, 0.1, 6, 10),
            button("copy_pos_y", 15.9, 10, 6, 16.1, 12, 10),
            button("copy_neg_y", 15.9, 4, 6, 16.1, 6, 10),
            button("copy_pos_y", 6, 10, -0.1, 10, 12, 0.1),
            button("copy_neg_y", 6, 4, -0.1, 10, 6, 0.1),
            button("copy_pos_y", 6, 10, 15.9, 10, 12, 16.1),
            button("copy_neg_y", 6, 4, 15.9, 10, 6, 16.1),
            button("dec_pos_z", -0.1, 9, 12, 0.1, 12, 15),
            button("inc_pos_z", -0.1, 4, 12, 0.1, 7, 15),
            button("copy_pos_z", -0.1, 6, 10, 0.1, 10, 12),
            button("inc_neg_z", -0.1, 9, 1, 0.1, 12, 4),
            button("dec_neg_z", -0.1, 4, 1, 0.1, 7, 4),
            button("copy_neg_z", -0.1, 6, 4, 0.1, 10, 6),
            button("dec_neg_z", 15.9, 9, 1, 16.1, 12, 4),
            button("inc_neg_z", 15.9, 4, 1, 16.1, 7, 4),
            button("copy_neg_z", 15.9, 6, 4, 16.1, 10, 6),
            button("inc_pos_z", 15.9, 9, 12, 16.1, 12, 15),
            button("dec_pos_z", 15.9, 4, 12, 16.1, 7, 15),
            button("copy_pos_z", 15.9, 6, 10, 16.1, 10, 12),
            button("dec_neg_x", 1, 9, -0.1, 4, 12, 0.1),
            button("inc_neg_x", 1, 4, -0.1, 4, 7, 0.1),
            button("copy_neg_x", 4, 6, -0.1, 6, 10, 0.1),
            button("inc_pos_x", 12, 9, -0.1, 15, 12, 0.1),
            button("dec_pos_x", 12, 4, -0.1, 15, 7, 0.1),
            button("copy_pos_x", 10, 6, -0.1, 12, 10, 0.1),
            button("dec_pos_x", 12, 9, 15.9, 15, 12, 16.1),
            button("inc_pos_x", 12, 4, 15.9, 15, 7, 16.1),
            button("copy_pos_x", 10, 6, 15.9, 12, 10, 16.1),
            button("inc_neg_x", 1, 9, 15.9, 4, 12, 16.1),
            button("dec_neg_x", 1, 4, 15.9, 4, 7, 16.1),
            button("copy_neg_x", 4, 6, 15.9, 6, 10, 16.1));

    private OBGuideControls() {}

    private static Button button(final String name, final double minX, final double minY, final double minZ,
            final double maxX, final double maxY, final double maxZ) {
        return new Button(name, new AABB(minX / 16, minY / 16, minZ / 16, maxX / 16, maxY / 16, maxZ / 16));
    }

    @Nullable
    public static Button find(final OBGuideOrientation orientation, final Vec3 worldHit) {
        final Vec3 hit = orientation.toLocal(worldHit);
        for (final Button button : BUTTONS) if (button.bounds().contains(hit)) return button;
        return null;
    }
}
