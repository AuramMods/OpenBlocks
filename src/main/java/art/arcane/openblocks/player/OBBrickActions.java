package art.arcane.openblocks.player;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.advancement.OBCriterions;
import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBSounds;
import art.arcane.openblocks.registry.OBStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBBrickActions {

    private OBBrickActions() {}

    @SubscribeEvent
    public static void onFoodFinished(final LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(OBItems.TASTY_CLAY.get())
                && !OBServerConfig.SERIOUS_MODE.get()) {
            player.getCapability(OBCapabilities.BOWELS).ifPresent(OBCapabilities.BowelContents::addBrick);
        }
    }

    public static void boo(final ServerPlayer player) {
        if (OBServerConfig.SERIOUS_MODE.get() || !player.isAlive() || player.isSpectator()) return;

        player.serverLevel().playSound(null, player.blockPosition(), OBSounds.FEATURE_FART.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player.isCreative()) {
            releaseBrick(player);
            return;
        }

        player.getCapability(OBCapabilities.BOWELS).ifPresent(contents -> {
            final int available = contents.getBrickCount();
            if (available == 0) return;

            contents.setBrickCount(available - 1);
            if (!releaseBrick(player)) contents.addBrick();
        });
    }

    private static boolean releaseBrick(final ServerPlayer player) {
        final var brick = createBrick(player);
        brick.setDefaultPickUpDelay();
        if (!player.serverLevel().addFreshEntity(brick)) return false;

        OBCriterions.BRICK_DROPPED.trigger(player);
        player.awardStat(OBStats.BRICKS_DROPPED.get());
        return true;
    }

    @SubscribeEvent
    public static void onDeathDrops(final LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || OBServerConfig.SERIOUS_MODE.get()) return;

        player.getCapability(OBCapabilities.BOWELS).ifPresent(contents -> {
            final int amount = Math.min(contents.getBrickCount(), 16);
            for (int i = 0; i < amount; i++) event.getDrops().add(createBrick(player));
            contents.setBrickCount(0);
        });
    }

    private static ItemEntity createBrick(final ServerPlayer player) {
        final var brick = new ItemEntity(player.serverLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.BRICK));
        final Vec3 backward = Vec3.directionFromRotation(0, player.getYRot()).scale(-1);
        brick.move(MoverType.SELF, new Vec3(0.75 * backward.x, 0.5, 0.75 * backward.z));
        brick.setDeltaMovement(0.5 * backward.x, 0.2, 0.5 * backward.z);
        return brick;
    }
}
