package art.arcane.openblocks.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerXpEvent;

public final class OBExperience {

    private OBExperience() {}

    public static int points(final Player player) {
        final double completedLevels = pointsAtLevel(player.experienceLevel);
        return (int) Math.min(Integer.MAX_VALUE,
                completedLevels + Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
    }

    public static boolean trySpend(final ServerPlayer player, final int cost) {
        if (cost < 0) return false;
        if (cost == 0) return true;
        final var event = new PlayerXpEvent.XpChange(player, -cost);
        if (MinecraftForge.EVENT_BUS.post(event) || event.getAmount() != -cost) return false;

        final int remaining = points(player) - cost;
        if (remaining < 0) return false;
        int low = 0;
        int high = player.experienceLevel;
        while (low < high) {
            final int middle = low + (high - low) / 2 + 1;
            if (pointsAtLevel(middle) <= remaining) low = middle;
            else high = middle - 1;
        }
        player.setExperienceLevels(low);
        player.setExperiencePoints(remaining - (int) pointsAtLevel(low));
        player.totalExperience = remaining;
        return true;
    }

    private static double pointsAtLevel(final double level) {
        if (level >= 32) {
            return (9 * level * level - 325 * level + 4440) / 2;
        }
        if (level >= 17) return (5 * level * level - 81 * level + 720) / 2;
        return level * (level + 6);
    }
}
