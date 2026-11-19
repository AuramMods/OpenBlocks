package art.arcane.openblocks.entity;

import art.arcane.openblocks.block.entity.OBGoldenEggBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBEntities;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

public final class OBMountedBlock extends Entity {

    private static final EntityDataAccessor<Integer> BLOCK_STATE = SynchedEntityData.defineId(OBMountedBlock.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EGG_LIFTED = SynchedEntityData.defineId(OBMountedBlock.class, EntityDataSerializers.BOOLEAN);
    private static final int TRANSFER_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Set<BlockEntityType<?>> PORTABLE = Set.of(BlockEntityType.BEACON, BlockEntityType.BREWING_STAND,
            BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST, BlockEntityType.COMMAND_BLOCK, BlockEntityType.DISPENSER,
            BlockEntityType.DROPPER, BlockEntityType.ENCHANTING_TABLE, BlockEntityType.ENDER_CHEST, BlockEntityType.FURNACE,
            BlockEntityType.HOPPER, BlockEntityType.JUKEBOX);

    @Nullable private CompoundTag contents;
    @Nullable private BlockPos source;
    @Nullable private UUID owner;
    @Nullable private GameProfile machineOwner;
    @Nullable private BlockPos liftingEgg;
    private boolean fallingFromEgg;

    public OBMountedBlock(final EntityType<? extends OBMountedBlock> type, final Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(BLOCK_STATE, Block.getId(Blocks.AIR.defaultBlockState()));
        entityData.define(EGG_LIFTED, false);
    }

    public BlockState carriedState() {
        return Block.stateById(entityData.get(BLOCK_STATE));
    }

    public static boolean capture(final OBMagnet magnet, final ServerPlayer actor, final BlockPos pos) {
        final ServerLevel level = actor.serverLevel();
        final OBMountedBlock cargo = prepareCapture(level, pos, actor);
        if (cargo == null) return false;

        cargo.machineOwner = magnet.machineOwner();
        if (!level.addFreshEntity(cargo) || !cargo.startRiding(magnet, true) || !cargo.removeSource(level, pos)) {
            cargo.discard();
            actor.connection.send(new ClientboundBlockUpdatePacket(level, pos));
            return false;
        }
        magnet.alignToCargo(cargo);
        level.updateNeighborsAt(pos, cargo.carriedState().getBlock());
        return true;
    }

    public static boolean liftFromEgg(final ServerLevel level, final BlockPos pos, final BlockPos egg, final ServerPlayer actor) {
        final OBMountedBlock cargo = prepareCapture(level, pos, actor);
        if (cargo == null) return false;

        cargo.liftingEgg = egg.immutable();
        cargo.machineOwner = actor.getGameProfile();
        cargo.entityData.set(EGG_LIFTED, true);
        if (!level.addFreshEntity(cargo) || !cargo.removeSource(level, pos)) {
            cargo.discard();
            return false;
        }
        level.updateNeighborsAt(pos, cargo.carriedState().getBlock());
        return true;
    }

    @Nullable
    private static OBMountedBlock prepareCapture(final ServerLevel level, final BlockPos pos, final ServerPlayer actor) {
        if (!level.hasChunkAt(pos) || !level.mayInteract(actor, pos) || !actor.mayBuild()) return null;
        final BlockState state = level.getBlockState(pos);
        // Ask Forge about transferring the block and its inventory before detaching either.
        if (!canMove(level, pos, state) || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, actor))) return null;

        final OBMountedBlock cargo = OBEntities.MOUNTED_BLOCK.get().create(level);
        if (cargo == null) return null;
        final Vec3 center = Vec3.atBottomCenterOf(pos);
        cargo.setPos(center.x, center.y, center.z);
        cargo.source = pos.immutable();
        cargo.owner = actor.getUUID();
        cargo.entityData.set(BLOCK_STATE, Block.getId(state));
        return cargo;
    }

    private boolean removeSource(final ServerLevel level, final BlockPos pos) {
        if (level.getBlockState(pos) != carriedState()) return false;
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        final CompoundTag data = blockEntity == null ? null : blockEntity.saveWithFullMetadata();
        // Detach the original BE before onRemove can spill an inventory into the world.
        if (blockEntity != null) level.removeBlockEntity(pos);
        if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), TRANSFER_FLAGS)) {
            if (blockEntity != null) {
                blockEntity.clearRemoved();
                level.setBlockEntity(blockEntity);
            }
            return false;
        }
        contents = data;
        return true;
    }

    private static boolean canMove(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.hasProperty(BlockStateProperties.BED_PART) || state.getOptionalValue(BlockStateProperties.CHEST_TYPE).orElse(ChestType.SINGLE) != ChestType.SINGLE
                || state.is(Blocks.MOVING_PISTON) || state.is(Blocks.PISTON_HEAD) || state.getOptionalValue(BlockStateProperties.EXTENDED).orElse(false)) return false;

        final BlockEntity tile = level.getBlockEntity(pos);
        if (state.hasBlockEntity()) return tile != null && (PORTABLE.contains(tile.getType()) || OBServerConfig.MAGNET_BLOCK_ENTITY_ALLOWLIST.get()
                .contains(ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(tile.getType()).toString()));
        return state.getRenderShape() == RenderShape.MODEL || OBServerConfig.MAGNET_BLOCK_ALLOWLIST.get()
                .contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString());
    }

    public boolean release(final ServerPlayer actor) {
        return placeAt(actor, placementPosition());
    }

    private BlockPos placementPosition() {
        // Collision math can leave the cargo a fraction below the surface it rests on.
        return BlockPos.containing(getX(), getY() + 1.0E-7, getZ());
    }

    private boolean placeAt(final ServerPlayer actor, final BlockPos pos) {
        if (!(level() instanceof ServerLevel level) || actor.level() != level || !level.hasChunkAt(pos) || !level.isInWorldBounds(pos)
                || !level.mayInteract(actor, pos) || !actor.mayBuild() || !level.isEmptyBlock(pos)) return false;
        final BlockState block = carriedState();
        if (block.isAir() || !block.canSurvive(level, pos)
                || !level.isUnobstructed(this, block.getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ()))) return false;

        final BlockSnapshot replaced = BlockSnapshot.create(level.dimension(), level, pos, TRANSFER_FLAGS);
        if (!level.setBlock(pos, block, TRANSFER_FLAGS)) return false;
        if (ForgeEventFactory.onBlockPlace(actor, replaced, Direction.UP) || level.getBlockState(pos) != block) {
            replaced.restore(true, true);
            return false;
        }
        if (contents != null) {
            final var tile = level.getBlockEntity(pos);
            if (tile == null) {
                replaced.restore(true, true);
                return false;
            }
            try {
                final CompoundTag moved = contents.copy();
                moved.putInt("x", pos.getX());
                moved.putInt("y", pos.getY());
                moved.putInt("z", pos.getZ());
                tile.load(moved);
                tile.setChanged();
                level.sendBlockUpdated(pos, block, block, Block.UPDATE_CLIENTS);
            } catch (final RuntimeException error) {
                level.removeBlockEntity(pos);
                replaced.restore(true, true);
                LogUtils.getLogger().warn("Could not restore carried block entity at {}", pos, error);
                return false;
            }
        }
        discard();
        level.updateNeighborsAt(pos, block.getBlock());
        return true;
    }

    public void recover(final ServerPlayer actor) {
        final BlockPos here = placementPosition();
        if (placeAt(actor, here)) return;
        for (final Direction direction : Direction.values()) {
            if (placeAt(actor, here.relative(direction))) return;
        }
        if (source != null && !source.equals(here)) placeAt(actor, source);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server && !isPassenger()) {
            if (carriedState().isAir()) {
                discard();
                return;
            }
            if (liftingEgg != null) {
                tickEggLift(server, liftingEgg);
                super.tick();
                return;
            }
            if (getY() < level().getMinBuildHeight() - 10 && source != null) {
                final Vec3 safe = Vec3.atBottomCenterOf(source);
                setPos(safe.x, safe.y, safe.z);
            }
            if (tickCount % 20 == 0) {
                if (machineOwner != null) recover(FakePlayerFactory.get(server, machineOwner));
                else if (owner != null && server.getPlayerByUUID(owner) instanceof ServerPlayer player) recover(player);
            }
        }
        super.tick();
    }

    private void tickEggLift(final ServerLevel level, final BlockPos eggPos) {
        if (!fallingFromEgg) {
            if (!level.hasChunkAt(eggPos)) return;
            if (level.getBlockEntity(eggPos) instanceof OBGoldenEggBlockEntity egg && egg.stage() == OBGoldenEggBlockEntity.Stage.FLOATING) {
                setPos(getX(), getY() + 0.1, getZ());
                return;
            }
            fallingFromEgg = true;
            setDeltaMovement(0, -0.9, 0);
        }

        noPhysics = false;
        move(MoverType.SELF, getDeltaMovement());
        final double speed = Math.max(-1.5, getDeltaMovement().y - 0.04);
        setDeltaMovement(0, speed, 0);
        if (onGround() || getY() < level.getMinBuildHeight()) {
            noPhysics = true;
            setDeltaMovement(Vec3.ZERO);
            if (getY() < level.getMinBuildHeight() && source != null) {
                final Vec3 safe = Vec3.atBottomCenterOf(source);
                setPos(safe.x, safe.y, safe.z);
            }
            recover(machineOwner == null ? FakePlayerFactory.getMinecraft(level) : FakePlayerFactory.get(level, machineOwner));
            liftingEgg = null;
            fallingFromEgg = false;
            if (!isRemoved()) entityData.set(EGG_LIFTED, false);
        }
    }

    @Override
    public boolean isPickable() {
        return !isPassenger() && !entityData.get(EGG_LIFTED);
    }

    @Override
    public boolean isInvulnerableTo(final DamageSource source) {
        return true;
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        if (isPassenger() || entityData.get(EGG_LIFTED)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) recover(serverPlayer);
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        entityData.set(BLOCK_STATE, Block.getId(NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState"))));
        contents = tag.contains("BlockEntity", Tag.TAG_COMPOUND) ? tag.getCompound("BlockEntity").copy() : null;
        source = tag.contains("Source", Tag.TAG_COMPOUND) ? NbtUtils.readBlockPos(tag.getCompound("Source")) : null;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        machineOwner = tag.contains("MachineOwner") ? NbtUtils.readGameProfile(tag.getCompound("MachineOwner")) : null;
        liftingEgg = tag.contains("LiftingEgg", Tag.TAG_COMPOUND) ? NbtUtils.readBlockPos(tag.getCompound("LiftingEgg")) : null;
        fallingFromEgg = tag.getBoolean("FallingFromEgg");
        entityData.set(EGG_LIFTED, liftingEgg != null);
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(carriedState()));
        if (contents != null) tag.put("BlockEntity", contents.copy());
        if (source != null) tag.put("Source", NbtUtils.writeBlockPos(source));
        if (owner != null) tag.putUUID("Owner", owner);
        if (machineOwner != null) tag.put("MachineOwner", NbtUtils.writeGameProfile(new CompoundTag(), machineOwner));
        if (liftingEgg != null) tag.put("LiftingEgg", NbtUtils.writeBlockPos(liftingEgg));
        if (fallingFromEgg) tag.putBoolean("FallingFromEgg", true);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
