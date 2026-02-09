package art.arcane.openblocks.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

public abstract class OBInventoryEvent extends PlayerEvent {

    protected final Map<String, SubInventory> subInventories;

    protected OBInventoryEvent(final Player player, final Map<String, SubInventory> subInventories) {
        super(player);
        this.subInventories = subInventories;
    }

    public Map<String, SubInventory> getSubInventories() {
        return Collections.unmodifiableMap(subInventories);
    }

    public static final class SubInventory {
        private final Map<Integer, ItemStack> slots = new LinkedHashMap<>();

        public SubInventory addItemStack(final int slot, final ItemStack stack) {
            if (slot < 0) throw new IllegalArgumentException("Slot index must be non-negative");
            if (stack == null || stack.isEmpty()) {
                slots.remove(slot);
                return this;
            }

            slots.put(slot, stack.copy());
            return this;
        }

        public ItemStack getItemStack(final int slot) {
            final ItemStack stack = slots.get(slot);
            return stack == null ? ItemStack.EMPTY : stack.copy();
        }

        public Map<Integer, ItemStack> asMap() {
            final Map<Integer, ItemStack> copy = new LinkedHashMap<>();
            for (final Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
                copy.put(entry.getKey(), entry.getValue().copy());
            }
            return Collections.unmodifiableMap(copy);
        }

        public SubInventory copy() {
            final SubInventory copy = new SubInventory();
            for (final Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
                copy.slots.put(entry.getKey(), entry.getValue().copy());
            }
            return copy;
        }
    }

    public static final class Store extends OBInventoryEvent {
        public Store(final Player player) {
            super(player, new LinkedHashMap<>());
        }

        public SubInventory createSubInventory(final String id) {
            final String normalizedId = normalizeId(id);
            if (subInventories.containsKey(normalizedId)) {
                throw new IllegalStateException("Sub inventory with id '" + normalizedId + "' already exists");
            }

            final SubInventory result = new SubInventory();
            subInventories.put(normalizedId, result);
            return result;
        }

        public Store putSubInventory(final String id, final SubInventory subInventory) {
            final String normalizedId = normalizeId(id);
            if (subInventory == null) throw new IllegalArgumentException("Sub inventory cannot be null");
            if (subInventories.containsKey(normalizedId)) {
                throw new IllegalStateException("Sub inventory with id '" + normalizedId + "' already exists");
            }

            subInventories.put(normalizedId, subInventory.copy());
            return this;
        }
    }

    public static final class Load extends OBInventoryEvent {
        public Load(final Player player, final Map<String, SubInventory> subInventories) {
            super(player, immutableSubInventoryCopy(subInventories));
        }

        public SubInventory getSubInventory(final String id) {
            return subInventories.get(id);
        }
    }

    private static Map<String, SubInventory> immutableSubInventoryCopy(final Map<String, SubInventory> source) {
        final Map<String, SubInventory> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, SubInventory> entry : source.entrySet()) {
            final String id = normalizeId(entry.getKey());
            final SubInventory subInventory = entry.getValue();
            if (subInventory == null) continue;
            copy.put(id, subInventory.copy());
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String normalizeId(final String id) {
        if (id == null) throw new IllegalArgumentException("Sub inventory id cannot be null");
        final String normalized = id.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Sub inventory id cannot be blank");
        return normalized;
    }
}
