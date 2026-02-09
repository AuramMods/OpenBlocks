package art.arcane.openblocks.world;

import net.minecraft.world.level.GameRules;

public final class OBGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> SPAWN_GRAVES =
            GameRules.register("openblocks:spawn_graves", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));

    private OBGameRules() {}

    public static void bootstrap() {
        // No-op: explicit call forces class init so static game rules are registered.
    }
}
