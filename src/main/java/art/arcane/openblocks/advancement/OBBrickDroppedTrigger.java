package art.arcane.openblocks.advancement;

import art.arcane.openblocks.OpenBlocks;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class OBBrickDroppedTrigger extends SimpleCriterionTrigger<OBBrickDroppedTrigger.Instance> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "brick_dropped");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(
            final JsonObject json,
            final ContextAwarePredicate playerPredicate,
            final DeserializationContext context) {
        return new Instance(playerPredicate);
    }

    public void trigger(final ServerPlayer player) {
        trigger(player, (instance) -> true);
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        public Instance(final ContextAwarePredicate playerPredicate) {
            super(ID, playerPredicate);
        }
    }
}
