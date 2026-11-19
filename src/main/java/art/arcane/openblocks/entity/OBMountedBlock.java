package art.arcane.openblocks.entity;

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
    private static final int TRANSFER_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Set<BlockEntityType<?>> PORTABLE = Set.of(BlockEntityType.BEACON, BlockEntityType.BREWING_STAND,
            BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST, BlockEntityType.COMMAND_BLOCK, BlockEntityType.DISPENSER,
            BlockEntityType.DROPPER, BlockEntityType.ENCHANTING_TABLE, BlockEntityType.ENDER_CHEST, BlockEntityType.FURNACE,
            BlockEntityType.HOPPER, BlockEntityType.JUKEBOX);

    @Nullable private CompoundTag contents;
    @Nullable private BlockPos source;
    @Nullable private UUID owner;
    @Nullable private GameProfile machineOwner;

    public OBMountedBlock(final EntityType<? extends OBMountedBlock> type, final Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(BLOCK_STATE, Block.getId(Blocks.AIR.defaultBlockState()));
    }

    public BlockState carriedState() {
        return Block.stateById(entityData.get(BLOCK_STATE));
    }

    public static boolean capture(final OBMagnet magnet, final ServerPlayer actor, final BlockPos pos) {
        final ServerLevel level = actor.serverLevel();
        if (!level.hasChunkAt(pos) || !level.mayInteract(actor, pos) || !actor.mayBuild()) return false;
        final BlockState state = level.getBlockState(pos);
        // The controller cannot mine blocks. Ask Forge about the transfer separately from a mining attempt.
        if (!canMove(level, pos, state) || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, actor))) return false;

        final OBMountedBlock cargo = OBEntities.MOUNTED_BLOCK.get().create(level);
        if (cargo == null) return false;
        final Vec3 center = Vec3.atBottomCenterOf(pos);
        cargo.setPos(center.x, center.y, center.z);
        if (!level.addFreshEntity(cargo) || !cargo.startRiding(magnet, true) || level.getBlockState(pos) != state) {
            cargo.discard();
            actor.connection.send(new ClientboundBlockUpdatePacket(level, pos));
            return false;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        final CompoundTag data = blockEntity == null ? null : blockEntity.saveWithFullMetadata();
        // Detach the original BE before onRemove can spill an inventory into the world.
        if (blockEntity != null) level.removeBlockEntity(pos);
        if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), TRANSFER_FLAGS)) {
            if (blockEntity != null) {
                blockEntity.clearRemoved();
                level.setBlockEntity(blockEntity);
            }
            cargo.discard();
            actor.connection.send(new ClientboundBlockUpdatePacket(level, pos));
            return false;
        }

        cargo.contents = data;
        cargo.source = pos.immutable();
        cargo.owner = actor.getUUID();
        cargo.machineOwner = magnet.machineOwner();
        cargo.entityData.set(BLOCK_STATE, Block.getId(state));
        magnet.alignToCargo(cargo);
        level.updateNeighborsAt(pos, state.getBlock());
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

    @Override
    public boolean isPickable() {
        return !isPassenger();
    }

    @Override
    public boolean isInvulnerableTo(final DamageSource source) {
        return true;
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        if (isPassenger()) return InteractionResult.PASS;
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
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(carriedState()));
        if (contents != null) tag.put("BlockEntity", contents.copy());
        if (source != null) tag.put("Source", NbtUtils.writeBlockPos(source));
        if (owner != null) tag.putUUID("Owner", owner);
        if (machineOwner != null) tag.put("MachineOwner", NbtUtils.writeGameProfile(new CompoundTag(), machineOwner));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
