package art.arcane.openblocks.entity;

import art.arcane.openblocks.item.OBGlyphItem;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public final class OBGlyph extends HangingEntity implements IEntityAdditionalSpawnData {

    private int glyph = OBGlyphItem.DEFAULT;
    private int offsetX = 8;
    private int offsetY = 8;

    public OBGlyph(final EntityType<? extends OBGlyph> type, final Level level) {
        super(type, level);
        pos = BlockPos.ZERO;
    }

    public void mount(final BlockPos anchor, final Direction side, final int index, final int x, final int y) {
        pos = anchor.immutable();
        glyph = OBGlyphItem.available(index) ? index : OBGlyphItem.DEFAULT;
        offsetX = Mth.clamp(x, 0, 16);
        offsetY = Mth.clamp(y, 0, 16);
        setDirection(side);
    }

    public int glyph() { return glyph; }
    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }

    @Override
    protected void recalculateBoundingBox() {
        if (pos == null || direction == null) return;
        final boolean alongX = direction.getAxis() == Direction.Axis.Z;
        final double x = pos.getX() + (alongX ? offsetX / 16.0 : 0.5 - direction.getStepX() * 0.484375);
        final double y = pos.getY() + offsetY / 16.0;
        final double z = pos.getZ() + (alongX ? 0.5 - direction.getStepZ() * 0.484375 : offsetX / 16.0);
        final double halfX = alongX ? 0.25 : 0.015625;
        final double halfZ = alongX ? 0.015625 : 0.25;
        setPosRaw(x, y, z);
        setBoundingBox(new AABB(x - halfX, y - 0.25, z - halfZ, x + halfX, y + 0.25, z + halfZ));
    }

    @Override public int getWidth() { return 8; }
    @Override public int getHeight() { return 8; }

    @Override
    public ItemStack getPickedResult(final HitResult result) { return OBGlyphItem.create(glyph, 1); }

    @Override
    public void dropItem(@Nullable final Entity breaker) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) return;
        playSound(SoundEvents.PAINTING_BREAK, 1F, 1F);
        if (!(breaker instanceof Player player) || !player.getAbilities().instabuild) spawnAtLocation(OBGlyphItem.create(glyph, 1));
    }

    @Override
    public void playPlacementSound() { playSound(SoundEvents.PAINTING_PLACE, 1F, 1F); }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CharIndex", glyph);
        tag.putByte("Facing", (byte) direction.get2DDataValue());
        tag.putByte("OffsetX", (byte) offsetX);
        tag.putByte("OffsetY", (byte) offsetY);
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        mount(pos, Direction.from2DDataValue(tag.getByte("Facing")),
                tag.contains("CharIndex", Tag.TAG_ANY_NUMERIC) ? tag.getInt("CharIndex") : OBGlyphItem.DEFAULT,
                tag.contains("OffsetX", Tag.TAG_ANY_NUMERIC) ? tag.getByte("OffsetX") : 8,
                tag.contains("OffsetY", Tag.TAG_ANY_NUMERIC) ? tag.getByte("OffsetY") : 8);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override
    public void writeSpawnData(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeByte(direction.get2DDataValue());
        buffer.writeVarInt(glyph);
        buffer.writeByte(offsetX).writeByte(offsetY);
    }

    @Override
    public void readSpawnData(final FriendlyByteBuf buffer) {
        mount(buffer.readBlockPos(), Direction.from2DDataValue(buffer.readByte()), buffer.readVarInt(), buffer.readByte(), buffer.readByte());
    }
}
