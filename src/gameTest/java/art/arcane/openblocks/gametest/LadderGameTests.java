package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBRopeLadderBlock;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class LadderGameTests {

    @GameTest(template = "empty")
    public static void jadedLadderClimbsOnlyWhenOpenAndRespondsToRedstone(final GameTestHelper helper) {
        final BlockPos trapdoor = new BlockPos(3, 2, 3);
        final Player player = helper.makeMockSurvivalPlayer();
        helper.setBlock(trapdoor.west(), Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.LADDER.get()));
        helper.assertTrue(useItem(helper, player, trapdoor.west(), Direction.EAST), "Jaded ladder did not place");
        final var placed = helper.getBlockState(trapdoor);
        helper.assertTrue(placed.is(OBBlocks.LADDER.get()) && placed.getValue(TrapDoorBlock.FACING) == Direction.EAST,
                "Jaded ladder did not orient toward the clicked face");
        final Vec3 standing = Vec3.atBottomCenterOf(helper.absolutePos(trapdoor));
        player.moveTo(standing.x, standing.y, standing.z, -90.0F, 0.0F);
        helper.assertTrue(!placed.getValue(TrapDoorBlock.OPEN) && !player.onClimbable(), "Closed ladder was climbable");

        helper.useBlock(trapdoor, player);
        player.baseTick();
        helper.assertTrue(helper.getBlockState(trapdoor).getValue(TrapDoorBlock.OPEN) && player.onClimbable(),
                "Opening the trapdoor did not make it climbable");
        helper.setBlock(trapdoor.north(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(helper.getBlockState(trapdoor).getValue(TrapDoorBlock.POWERED), "Power did not reach the trapdoor");
        helper.setBlock(trapdoor.north(), Blocks.AIR);
        player.baseTick();
        helper.assertTrue(!helper.getBlockState(trapdoor).getValue(TrapDoorBlock.OPEN) && !player.onClimbable(),
                "Unpowering the trapdoor did not close it");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ropeLadderUnrollsOnTheClickedFaceAndDropsPaidRungs(final GameTestHelper helper) {
        final BlockPos anchor = new BlockPos(3, 5, 3);
        prepareWallAndFloor(helper, anchor);
        final Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.ROPE_LADDER.get(), 8));
        helper.assertTrue(useItem(helper, player, anchor.west(), Direction.EAST), "Rope ladder did not place");

        assertRungs(helper, anchor, true);
        helper.assertTrue(player.getMainHandItem().getCount() == 4, "Rope ladder did not consume one item per rung");
        final var state = helper.getBlockState(anchor);
        final BlockPos absolute = helper.absolutePos(anchor);
        player.setYRot(90.0F);
        helper.assertTrue(!state.getCollisionShape(helper.getLevel(), absolute, CollisionContext.of(player)).isEmpty(),
                "Rope ladder did not stop a player facing into it");
        player.setYRot(-90.0F);
        helper.assertTrue(state.getCollisionShape(helper.getLevel(), absolute, CollisionContext.of(player)).isEmpty(),
                "Rope ladder blocked a player facing away from it");
        helper.getLevel().destroyBlock(absolute.below(), true);
        helper.assertBlockPresent(OBBlocks.ROPE_LADDER.get(), anchor);
        for (int offset = 1; offset < 4; ++offset) helper.assertBlockPresent(Blocks.AIR, anchor.below(offset));
        helper.assertTrue(itemCount(helper, anchor, OBItems.ROPE_LADDER.get()) == 3, "Detached rope did not drop each paid rung exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void infiniteRopeDoesNotCreateDropsAfterConfigChange(final GameTestHelper helper) {
        final BlockPos anchor = new BlockPos(3, 5, 3);
        prepareWallAndFloor(helper, anchor);
        final Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.ROPE_LADDER.get()));
        final boolean previous = OBServerConfig.ROPE_LADDER_INFINITE.get();
        try {
            OBServerConfig.ROPE_LADDER_INFINITE.set(true);
            helper.assertTrue(useItem(helper, player, anchor.west(), Direction.EAST), "Infinite rope did not place");
            assertRungs(helper, anchor, false);
            helper.assertTrue(player.getMainHandItem().isEmpty(), "Infinite mode did not use the anchor item");
        } finally {
            OBServerConfig.ROPE_LADDER_INFINITE.set(previous);
        }
        helper.getLevel().destroyBlock(helper.absolutePos(anchor.west()), false);
        for (int offset = 0; offset < 4; ++offset) helper.assertBlockPresent(Blocks.AIR, anchor.below(offset));
        helper.assertTrue(itemCount(helper, anchor, OBItems.ROPE_LADDER.get()) == 0,
                "Rungs created for free dropped items after the config changed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cancelledRopePlacementRestoresTheEntireStack(final GameTestHelper helper) {
        final BlockPos anchor = new BlockPos(3, 5, 3);
        prepareWallAndFloor(helper, anchor);
        final Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.ROPE_LADDER.get(), 4));
        final Consumer<BlockEvent.EntityMultiPlaceEvent> cancelPlacement = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityMultiPlaceEvent.class, cancelPlacement);
        try {
            helper.assertTrue(!useItem(helper, player, anchor.west(), Direction.EAST), "Forge did not cancel the entire rope placement");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancelPlacement);
        }
        for (int offset = 0; offset < 4; ++offset) helper.assertBlockPresent(Blocks.AIR, anchor.below(offset));
        helper.assertTrue(player.getMainHandItem().getCount() == 4 && itemCount(helper, anchor, OBItems.ROPE_LADDER.get()) == 0,
                "Cancelling placement changed the stack or spawned drops");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scaffoldingSupportsPlayersAndDespawnsWithDrops(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(3, 4, 3);
        final var block = OBBlocks.SCAFFOLDING.get();
        final var item = OBItems.SCAFFOLDING.get();
        final var level = helper.getLevel();
        final BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, block);
        final var state = helper.getBlockState(pos);
        helper.assertTrue(state.isRandomlyTicking() && Block.isShapeFullBlock(state.getCollisionShape(level, absolute)),
                "Scaffolding did not behave as a temporary solid platform");
        helper.assertTrue(ForgeHooks.getBurnTime(new ItemStack(item), RecipeType.SMELTING) == 100,
                "Scaffolding cannot fuel a furnace for 100 ticks");
        final int previous = OBServerConfig.SCAFFOLDING_DESPAWN_RATE.get();
        try {
            OBServerConfig.SCAFFOLDING_DESPAWN_RATE.set(0);
            state.randomTick(level, absolute, level.random);
        } finally {
            OBServerConfig.SCAFFOLDING_DESPAWN_RATE.set(previous);
        }
        helper.assertBlockPresent(Blocks.AIR, pos);
        helper.assertTrue(itemCount(helper, pos, item) == 1, "Despawned scaffolding did not drop itself");
        helper.succeed();
    }

    private static void prepareWallAndFloor(final GameTestHelper helper, final BlockPos anchor) {
        helper.setBlock(anchor.west(), Blocks.STONE);
        helper.setBlock(anchor.below(4), Blocks.STONE);
    }

    private static void assertRungs(final GameTestHelper helper, final BlockPos anchor, final boolean recoverable) {
        for (int offset = 0; offset < 4; ++offset) {
            final BlockPos pos = anchor.below(offset);
            helper.assertBlockPresent(OBBlocks.ROPE_LADDER.get(), pos);
            final var state = helper.getBlockState(pos);
            helper.assertTrue(state.getValue(OBRopeLadderBlock.FACING) == Direction.EAST
                    && state.getValue(OBRopeLadderBlock.RECOVERABLE) == recoverable, "Incorrect state for rung " + offset);
        }
        helper.assertBlockPresent(Blocks.STONE, anchor.below(4));
    }

    private static boolean useItem(final GameTestHelper helper, final Player player, final BlockPos pos, final Direction face) {
        final BlockPos absolute = helper.absolutePos(pos);
        final Vec3 surface = Vec3.atCenterOf(absolute).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        final var hit = new BlockHitResult(surface, face, absolute, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }

    private static int itemCount(final GameTestHelper helper, final BlockPos pos, final Item item) {
        final AABB bounds = new AABB(helper.absolutePos(pos.below(3))).minmax(new AABB(helper.absolutePos(pos.above()))).inflate(0.5);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds, entity -> entity.getItem().is(item)).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }
}
