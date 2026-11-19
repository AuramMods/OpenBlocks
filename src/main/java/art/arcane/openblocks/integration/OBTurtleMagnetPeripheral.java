package art.arcane.openblocks.integration;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.entity.OBMagnet;
import com.mojang.authlib.GameProfile;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;

public final class OBTurtleMagnetPeripheral implements IPeripheral {

    private static final String ACTOR_NAME = "[OBTurtle]";
    private static final GameProfile UNOWNED = new GameProfile(UUID.nameUUIDFromBytes(ACTOR_NAME.getBytes(StandardCharsets.UTF_8)), ACTOR_NAME);
    private static final int DEPLOY_FUEL = 5;

    private final ITurtleAccess turtle;
    private final TurtleSide side;
    private final AttachedComputerSet computers = new AttachedComputerSet();
    @Nullable private OBMagnet magnet;
    private Vec3 target = Vec3.ZERO;
    private int fuelTicks;

    OBTurtleMagnetPeripheral(final ITurtleAccess turtle, final TurtleSide side) {
        this.turtle = turtle;
        this.side = side;
    }

    @Override
    public String getType() {
        return "openblocks_magnet";
    }

    @Override
    public boolean equals(@Nullable final IPeripheral other) {
        return other == this;
    }

    @Override
    public void attach(final IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(final IComputerAccess computer) {
        computers.remove(computer);
    }

    @LuaFunction(mainThread = true)
    public void activate() throws LuaException {
        if (magnet != null && !magnet.isRemoved()) throw new LuaException("Magnet already active");
        if (!OBServerConfig.TURTLE_MAGNET_ENABLED.get()) throw new LuaException("Magnet turtles are disabled");
        if (!(turtle.getLevel() instanceof ServerLevel level) || !isAttachedTo(level)) throw new LuaException("Turtle is unavailable");

        final Direction right = turtle.getDirection().getClockWise();
        final BlockPos deployment = turtle.getPosition().relative(side == TurtleSide.LEFT ? right.getOpposite() : right);
        if (!level.hasChunkAt(deployment) || !level.isEmptyBlock(deployment)) throw new LuaException("Can't deploy magnet");
        final int fuel = turtle.getFuelLevel();
        if (!turtle.consumeFuel(DEPLOY_FUEL)) throw new LuaException("No fuel");
        final int spent = fuel - turtle.getFuelLevel();

        target = new Vec3(0, 0, side == TurtleSide.LEFT ? -1 : 1);
        magnet = OBMagnet.deployRemote(level, actorProfile(), targetPosition(), () -> isAttachedTo(level) ? targetPosition() : null);
        if (magnet == null) {
            if (spent > 0) turtle.addFuel(spent);
            throw new LuaException("Can't deploy magnet");
        }
        fuelTicks = 0;
    }

    @LuaFunction(mainThread = true)
    public void deactivate() throws LuaException {
        final OBMagnet active = activeMagnet();
        final double range = OBServerConfig.TURTLE_MAGNET_RECALL_RANGE.get();
        if (turtleCenter().distanceToSqr(magnetCenter(active)) > range * range) throw new LuaException("Magnet too far");
        stop(active);
    }

    @LuaFunction(mainThread = true)
    public void setTarget(final double x, final double y, final double z) throws LuaException {
        activeMagnet();
        final double range = OBServerConfig.TURTLE_MAGNET_RANGE.get();
        if (!(Math.abs(x) <= range && Math.abs(y) <= range && Math.abs(z) <= range)) throw new LuaException("Target out of range");
        target = new Vec3(x, y, z);
    }

    @LuaFunction(mainThread = true)
    public Object[] getPosition() throws LuaException {
        return coordinates(relativePosition(activeMagnet()));
    }

    @LuaFunction(mainThread = true)
    public boolean isAboveEntity() throws LuaException {
        return activeMagnet().detectsEntity();
    }

    @LuaFunction(value = {"toggleMagnet", "toggle"}, mainThread = true)
    public boolean toggleMagnet() throws LuaException {
        final OBMagnet active = activeMagnet();
        final var actor = FakePlayerFactory.get((ServerLevel) active.level(), actorProfile());
        final Vec3 center = turtleCenter();
        actor.moveTo(center.x, center.y, center.z, turtle.getDirection().toYRot(), 0);
        return active.toggle(actor);
    }

    @LuaFunction(mainThread = true)
    public boolean isGrabbing() throws LuaException {
        return activeMagnet().isVehicle();
    }

    @LuaFunction(value = {"getDistanceToTarget", "distance"}, mainThread = true)
    public Object[] getDistanceToTarget() throws LuaException {
        return coordinates(relativePosition(activeMagnet()).subtract(target));
    }

    void tick() {
        if (magnet == null) return;
        if (magnet.isRemoved()) {
            magnet = null;
            return;
        }
        if (++fuelTicks < 20) return;
        fuelTicks = 0;
        if (!turtle.consumeFuel(magnet.isVehicle() ? 2 : 1)) stop(magnet);
    }

    private void stop(final OBMagnet active) {
        active.retire();
        magnet = null;
    }

    private OBMagnet activeMagnet() throws LuaException {
        if (magnet == null || magnet.isRemoved() || !isAttachedTo(magnet.level())) throw new LuaException("Magnet not active");
        return magnet;
    }

    private boolean isAttachedTo(final Level level) {
        return OBServerConfig.TURTLE_MAGNET_ENABLED.get() && computers.hasComputers() && !turtle.isRemoved() && turtle.getLevel() == level;
    }

    private GameProfile actorProfile() {
        final GameProfile owner = turtle.getOwningPlayer();
        return owner != null && owner.getId() != null ? owner : UNOWNED;
    }

    private Vec3 targetPosition() {
        final Direction forward = turtle.getDirection();
        return turtleCenter().add(Vec3.atLowerCornerOf(forward.getNormal()).scale(target.x))
                .add(Vec3.atLowerCornerOf(forward.getClockWise().getNormal()).scale(target.z)).add(0, target.y - OBMagnet.SIZE / 2, 0);
    }

    private Vec3 relativePosition(final OBMagnet active) {
        final Vec3 delta = magnetCenter(active).subtract(turtleCenter());
        final Direction forward = turtle.getDirection();
        return new Vec3(delta.dot(Vec3.atLowerCornerOf(forward.getNormal())), delta.y,
                delta.dot(Vec3.atLowerCornerOf(forward.getClockWise().getNormal())));
    }

    private Vec3 turtleCenter() {
        return Vec3.atCenterOf(turtle.getPosition());
    }

    private static Vec3 magnetCenter(final OBMagnet active) {
        return active.position().add(0, OBMagnet.SIZE / 2, 0);
    }

    private static Object[] coordinates(final Vec3 vector) {
        return new Object[] { vector.x, vector.y, vector.z };
    }
}
