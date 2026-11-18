package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class UtilityBlockGameTests {

    @GameTest(template = "empty")
    public static void spongeRespectsConfiguredRangeAndNotifiesNeighbors(final GameTestHelper helper) {
        final BlockPos sponge = new BlockPos(3, 3, 3);
        final BlockPos near = sponge.east();
        final BlockPos far = sponge.west(2);
        helper.setBlock(near, Blocks.WATER);
        helper.setBlock(far, Blocks.WATER);
        final int oldRange = OBServerConfig.SPONGE_RANGE.get();
        final boolean oldUpdates = OBServerConfig.SPONGE_BLOCK_UPDATE.get();
        final var notified = new AtomicBoolean();
        final Consumer<BlockEvent.NeighborNotifyEvent> recordUpdate = event -> {
            if (event.getLevel() == helper.getLevel() && event.getPos().equals(helper.absolutePos(near))) notified.set(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.NeighborNotifyEvent.class, recordUpdate);
        try {
            OBServerConfig.SPONGE_RANGE.set(1);
            OBServerConfig.SPONGE_BLOCK_UPDATE.set(true);
            helper.setBlock(sponge, OBBlocks.SPONGE.get());
        } finally {
            MinecraftForge.EVENT_BUS.unregister(recordUpdate);
            OBServerConfig.SPONGE_RANGE.set(oldRange);
            OBServerConfig.SPONGE_BLOCK_UPDATE.set(oldUpdates);
        }
        helper.assertBlockPresent(Blocks.AIR, near);
        helper.assertBlockPresent(Blocks.WATER, far);
        helper.assertTrue(notified.get(), "Sponge removed a fluid without the configured neighbor update");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spongeBurnsOnAbsorbingLava(final GameTestHelper helper) {
        final BlockPos sponge = new BlockPos(3, 3, 3);
        helper.setBlock(sponge.below(), Blocks.STONE);
        helper.setBlock(sponge.west(), Blocks.LAVA);
        helper.setBlock(sponge.east(), Blocks.WATER);
        helper.setBlock(sponge, OBBlocks.SPONGE.get());
        helper.assertBlockPresent(Blocks.FIRE, sponge);
        helper.assertBlockPresent(Blocks.AIR, sponge.west());
        helper.assertBlockPresent(Blocks.AIR, sponge.east());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spongeStickCleansOnUseAndWearsOutOnlyWhenWet(final GameTestHelper helper) {
        final BlockPos origin = new BlockPos(3, 3, 3);
        final BlockPos fence = origin.east();
        final Player player = helper.makeMockSurvivalPlayer();
        final var stack = new ItemStack(OBItems.SPONGE_ON_A_STICK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.setBlock(origin, Blocks.WATER);
        helper.setBlock(fence, Blocks.OAK_FENCE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        final int oldDurability = OBServerConfig.SPONGE_STICK_DURABILITY.get();
        try {
            OBServerConfig.SPONGE_STICK_DURABILITY.set(2);
            helper.assertTrue(stack.getMaxStackSize() == 1 && stack.getMaxDamage() == 2, "Sponge on a Stick did not use tool properties");
            helper.assertTrue(useOn(helper, player, origin), "Sponge on a Stick did not absorb water");
            helper.assertTrue(stack.getDamageValue() == 1, "A successful use must charge exactly one durability");
            helper.assertBlockPresent(Blocks.AIR, origin);
            helper.assertBlockPresent(Blocks.OAK_FENCE, fence);
            helper.assertTrue(helper.getBlockState(fence).getFluidState().isEmpty(), "Sponge on a Stick left the fence waterlogged");
            helper.assertTrue(!useOn(helper, player, origin) && stack.getDamageValue() == 1, "A dry use wore out the tool");

            final Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(origin));
            player.moveTo(pos.x, pos.y, pos.z, 0, 0);
            helper.setBlock(origin.west(), Blocks.WATER);
            helper.assertTrue(stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                    "Air use did not absorb water around the player");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "The second successful use did not exhaust the tool");
        } finally {
            OBServerConfig.SPONGE_STICK_DURABILITY.set(oldDurability);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spongeStickBurnsThePlayerAndIsDestroyedByLava(final GameTestHelper helper) {
        final BlockPos origin = new BlockPos(3, 3, 3);
        final Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.SPONGE_ON_A_STICK.get()));
        helper.setBlock(origin, Blocks.LAVA);
        helper.assertTrue(useOn(helper, player, origin), "Sponge on a Stick did not absorb lava");
        helper.assertBlockPresent(Blocks.AIR, origin);
        helper.assertTrue(player.getMainHandItem().isEmpty() && player.getRemainingFireTicks() == 120,
                "Lava did not consume the tool and ignite the player for six seconds");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pathHasNoCollisionAndDropsWhenSupportIsRemoved(final GameTestHelper helper) {
        final BlockPos support = new BlockPos(3, 2, 3);
        final BlockPos path = support.above();
        final Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.PATH.get()));
        helper.setBlock(support, Blocks.STONE);
        helper.assertTrue(useOn(helper, player, support), "Path could not place on a sturdy block");
        helper.assertBlockPresent(OBBlocks.PATH.get(), path);
        final var level = helper.getLevel();
        final var pos = helper.absolutePos(path);
        final var state = helper.getBlockState(path);
        helper.assertTrue(state.getCollisionShape(level, pos).isEmpty(), "Decorative path obstructed movement");
        helper.assertTrue(state.isFaceSturdy(level, pos, Direction.DOWN), "Path lost its solid underside");
        helper.setBlock(support, Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, path);
        helper.assertItemEntityCountIs(OBItems.PATH.get(), path, 1.0, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void healBlockRestoresOneHealthAndOneFoodPerPulse(final GameTestHelper helper) {
        final BlockPos block = new BlockPos(3, 1, 3);
        final Player player = helper.makeMockSurvivalPlayer();
        final Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(block.above()));
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        player.setHealth(12.0F);
        player.getFoodData().setFoodLevel(12);
        player.getFoodData().setSaturation(0.0F);
        helper.setBlock(block, OBBlocks.HEAL.get());
        helper.getLevel().addFreshEntity(player);

        helper.succeedWhen(() -> {
            helper.assertTrue(player.getHealth() > 12.0F && player.getFoodData().getFoodLevel() > 12,
                    "Waiting for a healing pulse");
            final boolean matched = player.getHealth() == 13.0F && player.getFoodData().getFoodLevel() == 13;
            player.discard();
            helper.assertTrue(matched, "Healing pulse must restore exactly one health and one food");
        });
    }

    private static boolean useOn(final GameTestHelper helper, final Player player, final BlockPos block) {
        final var pos = helper.absolutePos(block);
        final var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos.above()), Direction.UP, pos, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }
}
