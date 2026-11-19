package art.arcane.openblocks.item;

import art.arcane.openblocks.registry.OBSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class OBSlimalyzerItem extends Item {

    private static final long SLIME_CHUNK_SALT = 987234911L;
    private static final String ACTIVE = "Active";

    public OBSlimalyzerItem() {
        super(new Properties());
    }

    public static boolean isActive(final ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(ACTIVE);
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity holder, final int slot, final boolean selected) {
        if (level instanceof ServerLevel server) update(stack, server, holder);
    }

    @Override
    public boolean onEntityItemUpdate(final ItemStack stack, final ItemEntity dropped) {
        if (dropped.level() instanceof ServerLevel server && update(stack, server, dropped)) {
            // Replace the tracked stack so item-entity metadata is sent to watching clients.
            dropped.setItem(stack.copy());
        }
        return false;
    }

    private static boolean update(final ItemStack stack, final ServerLevel level, final Entity holder) {
        final var chunk = holder.chunkPosition();
        final boolean slimeChunk = WorldgenRandom.seedSlimeChunk(chunk.x, chunk.z, level.getSeed(), SLIME_CHUNK_SALT).nextInt(10) == 0;
        if (slimeChunk == isActive(stack)) return false;

        if (slimeChunk) {
            stack.getOrCreateTag().putBoolean(ACTIVE, true);
            level.playSound(null, holder.getX(), holder.getY(), holder.getZ(), OBSounds.SLIMALYZER_SIGNAL.get(), SoundSource.PLAYERS, 1, 1);
        } else {
            stack.removeTagKey(ACTIVE);
        }
        return true;
    }
}
