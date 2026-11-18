package art.arcane.openblocks.entity;

import art.arcane.openblocks.registry.OBEntities;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public final class OBItemProjectile extends ItemEntity {

    public static final double GRAVITY = 0.04D;
    private boolean movedThroughAir;

    public OBItemProjectile(final EntityType<? extends ItemEntity> type, final Level level) {
        super(type, level);
    }

    public OBItemProjectile(final Level level, final Vec3 origin, final Vec3 velocity, final ItemStack stack) {
        this(OBEntities.ITEM_PROJECTILE.get(), level);
        setPos(origin.x, origin.y, origin.z);
        setDeltaMovement(velocity);
        setItem(stack);
        setDefaultPickUpDelay();
        lifespan = stack.getEntityLifespan(level);
    }

    @Override
    public void tick() {
        movedThroughAir = false;
        super.tick();
        // Keep vanilla gravity, collisions, fluid motion, pickup and aging. Remove only dry air damping.
        if (movedThroughAir && !isRemoved() && !isInFluidType()) {
            setDeltaMovement(getDeltaMovement().multiply(1.0D / 0.98F, 1.0D / 0.98D, 1.0D / 0.98F));
        }
    }

    @Override
    public void move(final MoverType type, final Vec3 motion) {
        super.move(type, motion);
        movedThroughAir = type == MoverType.SELF && !onGround() && !isInFluidType();
    }

    public boolean canBeVacuumed() {
        return getDeltaMovement().y < 0.01D;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
