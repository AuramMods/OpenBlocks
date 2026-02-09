package art.arcane.openblocks.advancement;

import net.minecraft.advancements.CriteriaTriggers;

public final class OBCriterions {

    public static final OBDevNullStackTrigger DEV_NULL_STACK = new OBDevNullStackTrigger();
    public static final OBBrickDroppedTrigger BRICK_DROPPED = new OBBrickDroppedTrigger();

    private static boolean bootstrapped;

    private OBCriterions() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;

        CriteriaTriggers.register(DEV_NULL_STACK);
        CriteriaTriggers.register(BRICK_DROPPED);
    }
}
