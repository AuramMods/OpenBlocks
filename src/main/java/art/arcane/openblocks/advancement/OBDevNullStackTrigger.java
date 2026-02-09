package art.arcane.openblocks.advancement;

import art.arcane.openblocks.OpenBlocks;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class OBDevNullStackTrigger extends SimpleCriterionTrigger<OBDevNullStackTrigger.Instance> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "dev_null_stacked");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(
            final JsonObject json,
            final ContextAwarePredicate playerPredicate,
            final DeserializationContext context) {
        final MinMaxBounds.Ints depth = json.has("depth")
                ? MinMaxBounds.Ints.fromJson(json.get("depth"))
                : MinMaxBounds.Ints.ANY;
        return new Instance(playerPredicate, depth);
    }

    public void trigger(final ServerPlayer player, final int depth) {
        trigger(player, (instance) -> instance.matches(depth));
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final MinMaxBounds.Ints depth;

        public Instance(final ContextAwarePredicate playerPredicate, final MinMaxBounds.Ints depth) {
            super(ID, playerPredicate);
            this.depth = depth;
        }

        public boolean matches(final int depth) {
            return this.depth.matches(depth);
        }

        @Override
        public JsonObject serializeToJson(final SerializationContext context) {
            final JsonObject json = super.serializeToJson(context);
            json.add("depth", depth.serializeToJson());
            return json;
        }
    }
}
