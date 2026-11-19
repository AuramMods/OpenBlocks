package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.entity.OBMiniMe;
import art.arcane.openblocks.entity.OBMountedBlock;
import art.arcane.openblocks.registry.OBBlockEntities;
import art.arcane.openblocks.registry.OBEntities;
import com.mojang.authlib.GameProfile;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;

public final class OBGoldenEggBlockEntity extends BlockEntity {

    private static final int CHANGE_INTERVAL = 100;
    private static final int RISE_TICKS = 400;
    private static final int FALL_TICKS = 10;
    private static final double RISE_HEIGHT = 5.0;
    private static final Stage[] STAGES = Stage.values();

    public enum Stage {
        INERT(0), ROTATING_SLOW(1), ROTATING_MEDIUM(10), ROTATING_FAST(50), FLOATING(100), FALLING(150), EXPLODING(666);

        final float rotationSpeed;

        Stage(final float rotationSpeed) { this.rotationSpeed = rotationSpeed; }

        public boolean hasAura() { return this == FLOATING || this == FALLING; }
    }

    @Nullable private GameProfile owner;
    private Stage stage = Stage.INERT;
    private int stageAge;
    private float previousRotation;
    private float rotation;
    private float rotationSpeed;

    public OBGoldenEggBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.GOLDEN_EGG.get(), pos, state);
    }

    public void setOwner(final GameProfile profile) {
        owner = profile;
        setChanged();
    }

    @Nullable public GameProfile owner() { return owner; }
    public Stage stage() { return stage; }
    public int stageAge() { return stageAge; }

    public float rotation(final float partialTick) {
        return previousRotation + rotationSpeed * partialTick;
    }

    public float progress(final float partialTick) {
        return switch (stage) {
            case FLOATING -> Mth.clamp((stageAge + partialTick) / RISE_TICKS, 0, 1);
            case FALLING -> Mth.clamp(1 - (stageAge + partialTick) / FALL_TICKS, 0, 1);
            default -> 0;
        };
    }

    public double offset(final float partialTick) { return progress(partialTick) * RISE_HEIGHT; }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final OBGoldenEggBlockEntity egg) {
        if (level instanceof ServerLevel server) egg.tickServer(server, pos);
        else {
            egg.stageAge++;
            egg.previousRotation = egg.rotation;
            egg.rotationSpeed = Mth.lerp(0.1F, egg.rotationSpeed, egg.stage.rotationSpeed);
            egg.rotation = Mth.wrapDegrees(egg.rotation + egg.rotationSpeed);
        }
    }

    private void tickServer(final ServerLevel level, final BlockPos pos) {
        stageAge++;
        setChanged();
        switch (stage) {
            case INERT, ROTATING_SLOW, ROTATING_MEDIUM, ROTATING_FAST -> {
                if (stageAge % CHANGE_INTERVAL == 0 && level.random.nextDouble() < 0.8) advance();
            }
            case FLOATING -> {
                if (stageAge >= RISE_TICKS) advance();
                else if (OBServerConfig.GOLDEN_EGG_PICK_BLOCKS.get() && level.random.nextInt(6) == 0) {
                    final BlockPos target = pos.offset(level.random.nextInt(20) - 10, level.random.nextInt(2) - 1, level.random.nextInt(20) - 10);
                    lift(level, target);
                }
            }
            case FALLING -> {
                if (stageAge >= FALL_TICKS) {
                    advance();
                    hatch(level, pos);
                }
            }
            case EXPLODING -> {
                if (stageAge % 20 == 0) hatch(level, pos);
            }
        }
    }

    private void advance() {
        stage = STAGES[stage.ordinal() + 1];
        stageAge = 0;
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public boolean lift(final ServerLevel level, final BlockPos source) {
        if (stage != Stage.FLOATING || !OBServerConfig.GOLDEN_EGG_PICK_BLOCKS.get() || worldPosition.equals(source)) return false;
        final var actor = owner != null && owner.getId() != null ? FakePlayerFactory.get(level, owner) : FakePlayerFactory.getMinecraft(level);
        return OBMountedBlock.liftFromEgg(level, source, worldPosition, actor);
    }

    private void hatch(final ServerLevel level, final BlockPos pos) {
        final var mini = OBEntities.MINI_ME.get().create(level);
        if (mini == null) return;

        mini.setOwner(owner == null ? OBMiniMe.DEFAULT_OWNER : owner);
        mini.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);
        if (!level.addFreshEntity(mini)) return;
        if (level.getBlockEntity(pos) != this || !level.removeBlock(pos, false)) {
            mini.discard();
            return;
        }

        // Using the newborn as the source keeps the blast from damaging the companion itself.
        level.explode(mini, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, Level.ExplosionInteraction.BLOCK);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Stage", stage.name());
        tag.putInt("StageAge", stageAge);
        if (owner != null) tag.put("Owner", NbtUtils.writeGameProfile(new CompoundTag(), owner));
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        owner = OBMiniMe.readOwner(tag);
        if (tag.contains("Stage", Tag.TAG_STRING)) {
            try {
                stage = Stage.valueOf(tag.getString("Stage"));
            } catch (final IllegalArgumentException ignored) {
                stage = Stage.INERT;
            }
        } else {
            final int ordinal = tag.getInt("stage");
            stage = ordinal >= 0 && ordinal < STAGES.length ? STAGES[ordinal] : Stage.INERT;
        }
        stageAge = Math.max(0, tag.getInt("StageAge"));
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(stage.hasAura() ? 18 : 1).expandTowards(0, RISE_HEIGHT, 0);
    }
}
