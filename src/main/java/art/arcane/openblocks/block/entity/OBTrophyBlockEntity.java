package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.trophy.OBTrophyEffects;
import art.arcane.openblocks.trophy.OBTrophyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OBTrophyBlockEntity extends BlockEntity {

    private OBTrophyType type = OBTrophyType.DEFAULT;
    private int cooldown;

    public OBTrophyBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.TROPHY.get(), pos, state);
    }

    public OBTrophyType trophy() { return type; }
    public int cooldown() { return cooldown; }

    public void setTrophy(final OBTrophyType type, final int cooldown) {
        this.type = type;
        this.cooldown = Math.max(0, cooldown);
        setChanged();
        if (level != null && !level.captureBlockSnapshots) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void activate(final ServerPlayer player) {
        if (!(level instanceof ServerLevel server)) return;
        final var ambient = type.ambientSound();
        if (ambient != null) server.playSound(null, worldPosition, ambient, SoundSource.NEUTRAL, 1, 1);
        if (cooldown > 0) return;
        cooldown = OBTrophyEffects.activate(type, server, worldPosition, player);
        if (cooldown > 0) setChanged();
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final OBTrophyBlockEntity trophy) {
        if (trophy.cooldown > 0) {
            trophy.cooldown--;
            level.getChunkAt(pos).setUnsaved(true);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(OBTrophyType.ENTITY_ID, type.id().toString());
        tag.putInt(OBTrophyType.COOLDOWN, cooldown);
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        type = OBTrophyType.readOrDefault(tag);
        cooldown = Math.max(0, tag.getInt(OBTrophyType.COOLDOWN));
    }

    @Override
    public CompoundTag getUpdateTag() {
        final var tag = new CompoundTag();
        tag.putString(OBTrophyType.ENTITY_ID, type.id().toString());
        return tag;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
