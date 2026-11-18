package art.arcane.openblocks.player;

import net.minecraft.world.entity.player.Player;

public final class OBExperience {

    private OBExperience() {}

    public static int points(final Player player) {
        final double level = player.experienceLevel;
        final double completedLevels;
        if (level >= 32) {
            completedLevels = (9 * level * level - 325 * level + 4440) / 2;
        } else if (level >= 17) {
            completedLevels = (5 * level * level - 81 * level + 720) / 2;
        } else {
            completedLevels = level * (level + 6);
        }
        return (int) Math.min(Integer.MAX_VALUE,
                completedLevels + Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
    }
}
