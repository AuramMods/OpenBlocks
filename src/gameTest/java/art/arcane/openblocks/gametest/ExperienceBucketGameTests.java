package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBCreativeTabs;
import art.arcane.openblocks.registry.OBFluids;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class ExperienceBucketGameTests {

    private static final BlockPos SOURCE = new BlockPos(3, 3, 3);

    @GameTest(template = "empty")
    public static void experienceBucketPicksUpAndPlacesSourcesWithVanillaInteraction(final GameTestHelper helper) {
        final boolean wasEnabled = OBServerConfig.XP_BUCKET_DIRECT_FILL.get();
        OBServerConfig.XP_BUCKET_DIRECT_FILL.set(true);
        try {
            final var level = helper.getLevel();
            final var juice = OBFluids.XP_JUICE_BLOCK.get();
            final var pos = helper.absolutePos(SOURCE);
            helper.setBlock(SOURCE.below(), Blocks.STONE);
            helper.setBlock(SOURCE, juice.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
            final var player = playerAbove(helper, pos);
            helper.assertTrue(!useBucket(helper, player).is(OBItems.XP_BUCKET.get()) && !level.getFluidState(pos).isSource(),
                    "An empty bucket picked up a flowing Liquid XP block");

            helper.setBlock(SOURCE, juice);
            final var filled = useBucket(helper, player);
            helper.assertTrue(filled.is(OBItems.XP_BUCKET.get()) && filled.getCount() == 1 && level.getFluidState(pos).isEmpty(),
                    "Survival use did not exchange an empty bucket and a Liquid XP source");
            final var contents = FluidUtil.getFluidContained(filled).orElseThrow();
            helper.assertTrue(contents.getFluid() == OBFluids.XP_JUICE.get() && contents.getAmount() == FluidType.BUCKET_VOLUME,
                    "The returned bucket did not expose exactly one bucket of Liquid XP through Forge");
            helper.assertTrue(useBucket(helper, player).is(Items.BUCKET) && level.getFluidState(pos).isSource(),
                    "Placing Liquid XP did not return an empty bucket and restore the source");
        } finally {
            OBServerConfig.XP_BUCKET_DIRECT_FILL.set(wasEnabled);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void experienceBucketSourceSettingPreservesTankExchange(final GameTestHelper helper) {
        final boolean wasEnabled = OBServerConfig.XP_BUCKET_DIRECT_FILL.get();
        final boolean tankDrainWasEnabled = OBServerConfig.TANK_BUCKET_DRAIN.get();
        OBServerConfig.XP_BUCKET_DIRECT_FILL.set(false);
        OBServerConfig.TANK_BUCKET_DRAIN.set(true);
        try {
            helper.setBlock(SOURCE, OBFluids.XP_JUICE_BLOCK.get());
            final var level = helper.getLevel();
            final var pos = helper.absolutePos(SOURCE);
            final var player = playerAbove(helper, pos);
            helper.assertTrue(useBucket(helper, player).is(Items.BUCKET) && level.getFluidState(pos).isSource(),
                    "The disabled source setting let a survival player drain the Liquid XP block");
            final var pickup = FluidUtil.tryPickUpFluid(new ItemStack(Items.BUCKET), null, level, pos, Direction.UP);
            // Forge marks a successful simulation as success even if the block rejects the actual drain.
            helper.assertTrue(!pickup.getResult().is(OBItems.XP_BUCKET.get()) && level.getFluidState(pos).isSource(),
                    "Forge container pickup transferred Liquid XP with the source setting disabled");

            final var tankPos = helper.absolutePos(SOURCE.east());
            helper.setBlock(SOURCE.east(), OBBlocks.TANK.get());
            final var tank = (OBTankBlockEntity) level.getBlockEntity(tankPos);
            final var port = tank.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElseThrow(IllegalStateException::new);
            port.fill(new FluidStack(OBFluids.XP_JUICE.get(), 1_000), FluidAction.EXECUTE);
            final var hit = new BlockHitResult(Vec3.atCenterOf(tankPos).add(0, 0.5, 0), Direction.UP, tankPos, false);
            helper.assertTrue(tank.getBlockState().use(level, player, InteractionHand.MAIN_HAND, hit).consumesAction()
                            && player.getMainHandItem().is(OBItems.XP_BUCKET.get()) && tank.contents().isEmpty(),
                    "Disabling world source pickup also disabled the supported tank exchange");
        } finally {
            OBServerConfig.XP_BUCKET_DIRECT_FILL.set(wasEnabled);
            OBServerConfig.TANK_BUCKET_DRAIN.set(tankDrainWasEnabled);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void experienceBucketCreativeSettingControlsTabAndSearch(final GameTestHelper helper) {
        final var tab = OBCreativeTabs.OPEN_BLOCKS_TAB.get();
        final var level = helper.getLevel();
        final var params = new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess());
        final var bucket = OBItems.XP_BUCKET.get();
        final boolean wasVisible = OBServerConfig.XP_BUCKET_IN_CREATIVE.get();
        try {
            OBServerConfig.XP_BUCKET_IN_CREATIVE.set(false);
            tab.buildContents(params);
            helper.assertTrue(!tab.contains(new ItemStack(bucket)) && tab.getDisplayItems().stream().noneMatch(stack -> stack.is(bucket)),
                    "The hidden XP Bucket appeared in the Open Blocks tab or its search index");
            OBServerConfig.XP_BUCKET_IN_CREATIVE.set(true);
            tab.buildContents(params);
            helper.assertTrue(tab.contains(new ItemStack(bucket)) && tab.getDisplayItems().stream().anyMatch(stack -> stack.is(bucket)),
                    "The enabled XP Bucket was missing from the Open Blocks tab or its search index");
        } finally {
            OBServerConfig.XP_BUCKET_IN_CREATIVE.set(wasVisible);
            tab.buildContents(params);
        }
        helper.succeed();
    }

    private static FakePlayer playerAbove(final GameTestHelper helper, final BlockPos pos) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "BucketGameTest"));
        player.moveTo(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 0, 90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        return player;
    }

    private static ItemStack useBucket(final GameTestHelper helper, final FakePlayer player) {
        final var result = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, result.getObject());
        return result.getObject();
    }
}
