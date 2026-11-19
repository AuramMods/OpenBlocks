package art.arcane.openblocks.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Customize a death's grave before any block or item is changed. */
@Cancelable
public final class OBGraveSpawnEvent extends Event {

    private final ServerPlayer player;
    private final List<ItemStack> loot;
    @Nullable private BlockPos location;
    private String epitaph;
    private Component deathMessage;

    public OBGraveSpawnEvent(final ServerPlayer player, @Nullable final BlockPos location,
            final List<ItemStack> loot, final String epitaph, final Component deathMessage) {
        this.player = Objects.requireNonNull(player);
        this.loot = new ArrayList<>(loot.stream().map(ItemStack::copy).toList());
        setLocation(location);
        setEpitaph(epitaph);
        setDeathMessage(deathMessage);
    }

    public ServerPlayer getPlayer() { return player; }

    /** Item edits apply to the new grave if placement succeeds. */
    public List<ItemStack> getLoot() { return loot; }

    @Nullable public BlockPos getLocation() { return location; }
    public void setLocation(@Nullable final BlockPos location) { this.location = location == null ? null : location.immutable(); }

    public String getEpitaph() { return epitaph; }
    public void setEpitaph(final String epitaph) { this.epitaph = Objects.requireNonNull(epitaph); }

    public Component getDeathMessage() { return deathMessage; }
    public void setDeathMessage(final Component deathMessage) { this.deathMessage = Objects.requireNonNull(deathMessage).copy(); }
}
