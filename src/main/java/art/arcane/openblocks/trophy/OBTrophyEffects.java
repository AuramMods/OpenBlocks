package art.arcane.openblocks.trophy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

public final class OBTrophyEffects {

    private static final int PLACE_FLAGS = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private OBTrophyEffects() {}

    public static int activate(final OBTrophyType type, final ServerLevel level, final BlockPos pos, final ServerPlayer player) {
        final var reward = type.reward();
        if (reward != null) {
            final var drop = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), new ItemStack(reward));
            drop.setDefaultPickUpDelay();
            if (!level.addFreshEntity(drop)) return 0;
            if (type == OBTrophyType.CHICKEN) {
                level.playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1,
                        1 + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F);
            }
            return type.rewardCooldown();
        }

        switch (type) {
            case BLAZE -> player.setSecondsOnFire(4);
            case CAVE_SPIDER -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 3));
            case CREEPER -> level.explode(player, pos.getX(), pos.getY(), pos.getZ(), 2, Level.ExplosionInteraction.NONE);
            case ENDERMAN -> {
                final var pearl = new ThrownEnderpearl(level, player);
                pearl.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                pearl.setDeltaMovement(level.random.nextGaussian(), 1, level.random.nextGaussian());
                return level.addFreshEntity(pearl) ? 10 : 0;
            }
            case EVOKER -> {
                player.connection.send(new ClientboundEntityEventPacket(player, (byte) 35));
                return 100;
            }
            case GUARDIAN, ELDER_GUARDIAN -> {
                player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, 0));
                return 100;
            }
            case LLAMA -> spit(level, pos, player);
            case MOOSHROOM, SNOW_GOLEM -> {
                spread(level, pos, player, type == OBTrophyType.SNOW_GOLEM);
                return type == OBTrophyType.SNOW_GOLEM ? 10 : 100;
            }
            case SHULKER -> {
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 1));
                return 100;
            }
            case SKELETON, STRAY -> {
                final var arrow = new Arrow(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                arrow.setBaseDamage(0.1);
                arrow.shoot(level.random.nextInt(10) - 5, 40, level.random.nextInt(10) - 5, 1.0F, 6.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1,
                        1 / (level.random.nextFloat() * 0.4F + 1.2F) + 0.5F);
                level.addFreshEntity(arrow);
            }
            case SQUID -> {
                final BlockPos above = pos.above();
                if (level.dimensionType().ultraWarm()) {
                    level.playSound(null, above, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
                } else {
                    place(level, above, player, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
                }
                return 10;
            }
            case WITCH -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 1));
                player.sendSystemMessage(Component.translatable("block.open_blocks.trophy.witched"));
            }
            default -> { }
        }
        return 0;
    }

    private static void spit(final ServerLevel level, final BlockPos pos, final ServerPlayer player) {
        final var spit = EntityType.LLAMA_SPIT.create(level);
        if (spit == null) return;
        spit.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        final double x = player.getX() - spit.getX();
        final double z = player.getZ() - spit.getZ();
        final double y = player.getY() + player.getBbHeight() / 3 - spit.getY();
        spit.shoot(x, y + Math.hypot(x, z) * 0.2, z, 1.5F, 10.0F);
        level.playSound(null, pos, SoundEvents.LLAMA_SPIT, SoundSource.NEUTRAL, 1,
                1 + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F);
        level.addFreshEntity(spit);
    }

    private static void spread(final ServerLevel level, final BlockPos center, final ServerPlayer player, final boolean snow) {
        for (final BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            final BlockState state = snow
                    ? Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, level.random.nextInt(8) + 1)
                    : Blocks.BROWN_MUSHROOM.defaultBlockState();
            place(level, pos, player, state);
        }
    }

    private static void place(final ServerLevel level, final BlockPos pos, final ServerPlayer player, final BlockState state) {
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayBuild()
                || !level.isEmptyBlock(pos) || !state.canSurvive(level, pos)) return;

        final BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos, PLACE_FLAGS);
        if (!level.setBlock(pos, state, PLACE_FLAGS)) return;
        if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)) {
            snapshot.restore(true, true);
        } else {
            level.sendBlockUpdated(pos, snapshot.getReplacedBlock(), state, Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }
}
