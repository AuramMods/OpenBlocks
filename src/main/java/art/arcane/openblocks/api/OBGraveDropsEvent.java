package art.arcane.openblocks.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class OBGraveDropsEvent extends Event {

    public enum Action {
        STORE,
        DELETE,
        DROP
    }

    public static final class ItemAction {
        private final ItemEntity item;
        private Action action;

        private ItemAction(final ItemEntity item, final Action action) {
            if (item == null) throw new IllegalArgumentException("item cannot be null");
            if (action == null) throw new IllegalArgumentException("action cannot be null");
            this.item = item;
            this.action = action;
        }

        public ItemEntity getItem() {
            return item;
        }

        public Action getAction() {
            return action;
        }

        public void setAction(final Action action) {
            if (action == null) throw new IllegalArgumentException("action cannot be null");
            this.action = action;
        }
    }

    private final ServerPlayer player;
    private final List<ItemAction> drops = new ArrayList<>();

    public OBGraveDropsEvent(final ServerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public List<ItemAction> getDrops() {
        return drops;
    }

    public void addItem(final ItemEntity item) {
        addItem(item, Action.STORE);
    }

    public void addItem(final ItemEntity item, final Action action) {
        drops.add(new ItemAction(item, action));
    }
}
