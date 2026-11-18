package art.arcane.openblocks.entity;

import art.arcane.openblocks.registry.OBEntities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public final class OBFallingExperienceOrb extends ExperienceOrb implements IEntityAdditionalSpawnData {

    public OBFallingExperienceOrb(final EntityType<? extends ExperienceOrb> type, final Level level) {
        super(type, level);
    }

    public OBFallingExperienceOrb(final Level level, final Vec3 origin, final int points) {
        this(OBEntities.XP_ORB_NO_FLY.get(), level);
        setPos(origin.x, origin.y, origin.z);
        setDeltaMovement(0.0, -0.1 * level.random.nextFloat(), 0.0);
        value = points;
    }

    @Override
    protected void scanForEntities() {
        // These orbs fall at the nozzle so an XP Drain can collect them below it.
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(getValue());
    }

    @Override
    public void readSpawnData(final FriendlyByteBuf buffer) {
        value = buffer.readVarInt();
    }
}
