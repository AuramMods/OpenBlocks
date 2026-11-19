package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.block.OBGuideBlock;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.guide.OBGuideOrientation;
import art.arcane.openblocks.guide.OBGuideShape;
import art.arcane.openblocks.guide.OBGuideShape.Bounds;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;

public final class OBGuideBlockEntity extends BlockEntity {

    private static final int CREATIVE_BATCH = 128;
    private static final int FILL_FLAGS = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private Bounds bounds = Bounds.DEFAULT;
    private OBGuideShape shape = OBGuideShape.SPHERE;
    private int color = 0xFFFFFF;
    private List<BlockPos> points;
    private OBGuideOrientation cachedOrientation;
    private CreativeFill fill;

    private static final class CreativeFill {
        private final UUID player;
        private final ItemStack item;
        private final BlockHitResult click;
        private final List<BlockPos> points;
        private int next;

        private CreativeFill(final Player player, final BlockHitResult click, final List<BlockPos> points) {
            this.player = player.getUUID();
            item = player.getMainHandItem().copy();
            this.click = click;
            this.points = points;
        }
    }

    public OBGuideBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    public boolean builder() { return getBlockState().getBlock() instanceof OBGuideBlock block && block.builder(); }
    public OBGuideOrientation orientation() { return getBlockState().getValue(OBGuideBlock.ORIENTATION); }
    public OBGuideShape shape() { return shape; }
    public Bounds bounds() { return bounds; }
    public int color() { return color; }

    public boolean active() {
        final int sensitivity = OBServerConfig.SPEC.isLoaded() ? OBServerConfig.GUIDE_REDSTONE.get() : 1;
        return sensitivity == 0 || (sensitivity > 0) == getBlockState().getValue(BlockStateProperties.POWERED);
    }

    public static int sizeLimit() {
        return OBServerConfig.SPEC.isLoaded() ? OBServerConfig.GUIDE_MAX_HALF_AXIS.get() : 64;
    }

    public List<BlockPos> points() {
        final OBGuideOrientation current = orientation();
        if (points == null || cachedOrientation != current) {
            points = shape.points(bounds, current, builder());
            cachedOrientation = current;
        }
        return points;
    }

    public void setShape(final OBGuideShape next) {
        if (shape == next) return;
        shape = next;
        changesShape();
    }

    public void setBounds(final Bounds next) {
        final int cap = sizeLimit();
        final Bounds limited = new Bounds(Mth.clamp(next.negX(), 0, cap), Mth.clamp(next.negY(), 0, cap), Mth.clamp(next.negZ(), 0, cap),
                Mth.clamp(next.posX(), 0, cap), Mth.clamp(next.posY(), 0, cap), Mth.clamp(next.posZ(), 0, cap));
        if (bounds.equals(limited)) return;
        bounds = limited;
        changesShape();
    }

    public void setColor(final int rgb) {
        if (color == (rgb & 0xFFFFFF)) return;
        color = rgb & 0xFFFFFF;
        sync();
    }

    private void changesShape() {
        points = null;
        fill = null;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void build(final ServerPlayer player, final BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !active() || !player.mayBuild()) return;
        final ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof BlockItem)) return;
        if (player.getAbilities().instabuild && server.getBlockState(worldPosition.above()).is(Blocks.OBSIDIAN)) {
            fill = new CreativeFill(player, hit, points());
            tickFill(server, player);
            return;
        }
        for (final BlockPos offset : points()) {
            final BlockPos target = worldPosition.offset(offset);
            if (place(server, player, target, hit, false)) {
                animate(server, target);
                return;
            }
            if (held.isEmpty()) return;
        }
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state, final OBGuideBlockEntity guide) {
        if (guide.fill != null && level instanceof ServerLevel server) {
            guide.tickFill(server, server.getServer().getPlayerList().getPlayer(guide.fill.player));
        }
    }

    private void tickFill(final ServerLevel server, @Nullable final ServerPlayer player) {
        final CreativeFill job = fill;
        if (player == null || player.serverLevel() != server || !player.getAbilities().instabuild
                || !ItemStack.isSameItemSameTags(job.item, player.getMainHandItem()) || !active()
                || !server.getBlockState(worldPosition.above()).is(Blocks.OBSIDIAN)
                || player.distanceToSqr(Vec3.atCenterOf(worldPosition)) > 64 || cachedOrientation != orientation()) {
            fill = null;
            return;
        }
        final int last = Math.min(job.next + CREATIVE_BATCH, job.points.size());
        while (fill == job && job.next < last) {
            place(server, player, worldPosition.offset(job.points.get(job.next++)), job.click, true);
        }
        if (job.next >= job.points.size()) fill = null;
    }

    private boolean place(final ServerLevel server, final ServerPlayer player, final BlockPos target, final BlockHitResult click, final boolean replace) {
        if (!server.isInWorldBounds(target) || !server.hasChunkAt(target) || !server.getWorldBorder().isWithinBounds(target)
                || !server.mayInteract(player, target)) return false;
        final ItemStack item = player.getMainHandItem();
        if (!(item.getItem() instanceof BlockItem placement) || !player.mayUseItemAt(target, click.getDirection(), item)) return false;

        final BlockState old = server.getBlockState(target);
        final BlockSnapshot snapshot;
        if (old.isAir()) {
            snapshot = null;
        } else {
            if (!replace || old.is(placement.getBlock()) || old.hasBlockEntity() || old.getDestroySpeed(server, target) < 0
                    || old.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || old.hasProperty(BlockStateProperties.BED_PART)
                    || old.getOptionalValue(BlockStateProperties.EXTENDED).orElse(false) || old.getBlock() instanceof PistonHeadBlock
                    || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server, target, old, player))) return false;
            snapshot = BlockSnapshot.create(server.dimension(), server, target, FILL_FLAGS);
            if (!server.setBlock(target, Blocks.AIR.defaultBlockState(), FILL_FLAGS)) return false;
        }

        final Vec3 hit = click.getLocation().subtract(Vec3.atLowerCornerOf(worldPosition)).add(Vec3.atLowerCornerOf(target));
        final BlockHitResult remote = new BlockHitResult(hit, click.getDirection(), target, false);
        player.gameMode.useItemOn(player, server, item, InteractionHand.MAIN_HAND, remote);
        if (!server.getBlockState(target).isAir()) return true;
        if (snapshot != null) snapshot.restore(true, true);
        return false;
    }

    private void animate(final ServerLevel server, final BlockPos target) {
        final Vec3 start = Vec3.atCenterOf(worldPosition);
        final Vec3 end = Vec3.atCenterOf(target);
        final int steps = Math.min(64, Mth.ceil(start.distanceTo(end)));
        final var dust = new BlockParticleOption(ParticleTypes.BLOCK, server.getBlockState(target));
        for (int i = 0; i <= steps; i++) {
            final Vec3 sample = start.lerp(end, i / (double) steps);
            server.sendParticles(ParticleTypes.PORTAL, sample.x, sample.y, sample.z, 2, 0.12, 0.12, 0.12, 0);
            server.sendParticles(dust, sample.x, sample.y, sample.z, 2, 0.12, 0.12, 0.12, 0);
        }
    }

    public CompoundTag settings() {
        final var tag = new CompoundTag();
        tag.putInt("NegX", bounds.negX());
        tag.putInt("NegY", bounds.negY());
        tag.putInt("NegZ", bounds.negZ());
        tag.putInt("PosX", bounds.posX());
        tag.putInt("PosY", bounds.posY());
        tag.putInt("PosZ", bounds.posZ());
        tag.putString("Mode", shape.id());
        tag.putInt("Color", color);
        return tag;
    }

    public static Bounds readBounds(final CompoundTag tag) {
        final int limit = sizeLimit();
        return new Bounds(extent(tag, "NegX", "Width", limit), extent(tag, "NegY", "Height", limit), extent(tag, "NegZ", "Depth", limit),
                extent(tag, "PosX", "Width", limit), extent(tag, "PosY", "Height", limit), extent(tag, "PosZ", "Depth", limit));
    }

    private static int extent(final CompoundTag tag, final String key, final String legacy, final int limit) {
        if (tag.contains(key, Tag.TAG_ANY_NUMERIC)) return Mth.clamp(tag.getInt(key), 0, limit);
        return tag.contains(legacy, Tag.TAG_ANY_NUMERIC) ? Mth.clamp(tag.getInt(legacy), 0, limit) : 8;
    }

    public static OBGuideShape readShape(final CompoundTag tag) {
        if (tag.contains("Mode", Tag.TAG_ANY_NUMERIC)) return OBGuideShape.byIndex(tag.getInt("Mode"));
        final OBGuideShape mode = OBGuideShape.byName(tag.getString("Mode"));
        return mode == null ? OBGuideShape.SPHERE : mode;
    }

    private void readSettings(final CompoundTag tag) {
        final Bounds nextBounds = readBounds(tag);
        final OBGuideShape nextShape = readShape(tag);
        if (!bounds.equals(nextBounds) || shape != nextShape) points = null;
        bounds = nextBounds;
        shape = nextShape;
        color = tag.contains("Color", Tag.TAG_ANY_NUMERIC) ? tag.getInt("Color") & 0xFFFFFF : 0xFFFFFF;
    }

    public void applyItem(final CompoundTag tag) {
        readSettings(tag);
        sync();
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        readSettings(tag);
        fill = null;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);
        tag.merge(settings());
    }

    @Override public CompoundTag getUpdateTag() { return settings(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void handleUpdateTag(final CompoundTag tag) { readSettings(tag); }

    @Override
    public void onDataPacket(final net.minecraft.network.Connection connection, final ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) readSettings(packet.getTag());
    }
}
