package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.registry.OBBlockEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;

public final class OBFoldingTargetBlockEntity extends BlockEntity {

    private static final String TARGET_POS = "open_blocks:folding_target";
    private static final GameProfile RETURN_PLAYER = new GameProfile(UUID.fromString("9259329a-8636-44f0-a241-2edcced23343"), "[OpenBlocksTarget]");
    private static final int PULSE_LENGTH = 10;
    private int score;
    private long expiresAt;
    private boolean checkingInput;

    public OBFoldingTargetBlockEntity(final BlockPos pos, final BlockState state) { super(OBBlockEntities.TARGET.get(), pos, state); }

    public int outputPower() { return checkingInput ? 0 : score; }

    public boolean hasInputPower(final ServerLevel level) {
        // Strong output through a supporting block must not count as the target's own input.
        checkingInput = true;
        try {
            return level.hasNeighborSignal(worldPosition);
        } finally {
            checkingInput = false;
        }
    }

    public void recordHit(@Nullable final AbstractArrow arrow, final int strength) {
        if (!(level instanceof ServerLevel server)) return;
        if (arrow != null) arrow.getPersistentData().putLong(TARGET_POS, worldPosition.asLong());
        setScore(server, strength, server.getGameTime() + PULSE_LENGTH);
    }

    private void setScore(final ServerLevel level, final int next, final long expires) {
        final boolean changed = score != next;
        score = next;
        expiresAt = expires;
        setChanged();
        if (changed) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final OBFoldingTargetBlockEntity target) {
        if (level instanceof ServerLevel server && target.score != 0 && server.getGameTime() >= target.expiresAt) {
            target.setScore(server, 0, 0);
        }
    }

    public void releaseArrows(final ServerLevel level) {
        final long position = worldPosition.asLong();
        final var arrows = level.getEntitiesOfClass(AbstractArrow.class, new AABB(worldPosition).inflate(0.25), arrow -> {
            final CompoundTag data = arrow.getPersistentData();
            return arrow.isAlive() && data.contains(TARGET_POS, Tag.TAG_LONG) && data.getLong(TARGET_POS) == position;
        });
        if (arrows.isEmpty()) return;

        final var collector = new ArrowCollector(level, worldPosition);
        for (final AbstractArrow arrow : arrows) {
            arrow.getPersistentData().remove(TARGET_POS);
            if (arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY) {
                arrow.discard();
            } else if (arrow.pickup == AbstractArrow.Pickup.ALLOWED) {
                arrow.shakeTime = 0;
                arrow.playerTouch(collector);
            }
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("score", score);
        tag.putLong("expiresAt", expiresAt);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        score = Mth.clamp(tag.getInt("score"), 0, 15);
        expiresAt = tag.getLong("expiresAt");
    }

    private static final class ArrowCollector extends FakePlayer {
        private final Inventory drops;

        private ArrowCollector(final ServerLevel level, final BlockPos pos) {
            super(level, RETURN_PLAYER);
            drops = new Inventory(this) {
                @Override public boolean add(final int slot, final ItemStack stack) {
                    if (stack.isEmpty()) return false;
                    // An arrow is removed by vanilla only after its original pickup stack is accepted.
                    final var item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, stack.copy());
                    item.setDefaultPickUpDelay();
                    return level.addFreshEntity(item);
                }
            };
        }

        @Override public Inventory getInventory() { return drops == null ? super.getInventory() : drops; }
        @Override public void take(final Entity entity, final int count) {}
    }
}
