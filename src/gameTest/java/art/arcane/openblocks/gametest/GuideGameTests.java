package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBGuideBlock;
import art.arcane.openblocks.block.entity.OBGuideBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.guide.OBGuideOrientation;
import art.arcane.openblocks.guide.OBGuideShape;
import art.arcane.openblocks.guide.OBGuideShape.Bounds;
import art.arcane.openblocks.item.OBPaintColors;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class GuideGameTests {

    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final OBGuideOrientation BASE = OBGuideOrientation.XP_YP;

    @GameTest(template = "empty")
    public static void legacyGuideRecipesAndAllElevenShapesAreAvailable(final GameTestHelper helper) {
        final var guideGrid = ComponentRecipeGameTests.input(helper, Items.GLASS, Items.REDSTONE, Items.GLASS,
                Items.GLASS, Items.TORCH, Items.GLASS, Items.GLASS, Items.REDSTONE, Items.GLASS);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, guideGrid).is(OBItems.GUIDE.get()), "Guide recipe failed");
        guideGrid.setItem(3, new ItemStack(Items.ENDER_PEARL));
        guideGrid.setItem(5, new ItemStack(Items.ENDER_PEARL));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, guideGrid).is(OBItems.BUILDER_GUIDE.get()), "Builder Guide recipe failed");

        final Bounds cube = new Bounds(1, 1, 1, 1, 1, 1);
        helper.assertTrue(OBGuideShape.CUBOID.points(cube, BASE, false).size() == 20
                && OBGuideShape.FULL_CUBOID.points(cube, BASE, false).size() == 26
                && OBGuideShape.AXES.points(cube, BASE, false).size() == 6
                && OBGuideShape.PLANES.points(cube, BASE, false).size() == 18, "Guide edge, wall, axis, or plane geometry was wrong");
        for (final OBGuideShape shape : OBGuideShape.values()) {
            final var points = shape.points(Bounds.DEFAULT, BASE, false);
            helper.assertTrue(!points.isEmpty() && points.size() == new HashSet<>(points).size() && !points.contains(BlockPos.ZERO),
                    "Non-unique marker layout or missing " + shape.id());
        }
        final var triangle = OBGuideShape.TRIANGLE.points(Bounds.DEFAULT, BASE, false);
        helper.assertTrue(triangle.size() == 38 * 17 && triangle.contains(new BlockPos(8, 0, 0)), "Triangle lost its legacy raster or extrusion");

        final Bounds asymmetric = new Bounds(6, 3, 1, 1, 2, 8);
        for (final OBGuideShape shape : OBGuideShape.values()) {
            final var points = shape.points(asymmetric, BASE, false);
            helper.assertTrue(points.stream().allMatch(p -> p.getX() >= -6 && p.getX() <= 1 && p.getY() >= -3 && p.getY() <= 2
                    && p.getZ() >= -1 && p.getZ() <= 8), shape.id() + " exceeded its selected bounds");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guideFaceButtonsChangeBoundsModeAndOrientation(final GameTestHelper helper) {
        final var guide = place(helper, false);
        final var pos = guide.getBlockPos();
        final var player = player(helper, guide);
        final Vec3 origin = Vec3.atLowerCornerOf(pos);

        use(guide, player, new BlockHitResult(origin.add(0.5, 1, 0.2), Direction.UP, pos, false));
        helper.assertTrue(guide.shape() == OBGuideShape.CYLINDER, "The original top mode button failed");
        use(guide, player, new BlockHitResult(origin.add(0.33, 0.83, 0), Direction.NORTH, pos, false));
        helper.assertTrue(guide.bounds().posY() == 9 && guide.bounds().negY() == 8, "The positive Y button edited the wrong half-axis");
        use(guide, player, new BlockHitResult(origin.add(0.5, 0.69, 0), Direction.NORTH, pos, false));
        helper.assertTrue(guide.bounds().negY() == 9, "The mirrored size button failed");
        use(guide, player, new BlockHitResult(origin.add(0.82, 1, 0.5), Direction.UP, pos, false));
        helper.assertTrue(guide.orientation() == OBGuideOrientation.ZP_YP, "The clockwise button did not rotate the local axes");
        final var blue = new ItemStack(Items.BLUE_DYE, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, blue);
        use(guide, player, new BlockHitResult(origin.add(0.5, 1, 0.5), Direction.UP, pos, false));
        helper.assertTrue(guide.color() == OBPaintColors.palette(DyeColor.BLUE) && blue.getCount() == 2, "Dye did not set the preview color");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.WRENCH.get()));
        use(guide, player, new BlockHitResult(origin.add(0.5, 0.5, 0), Direction.NORTH, pos, false));
        helper.assertTrue(guide.orientation() == OBGuideOrientation.XP_ZN, "The Wrench failed to orient a Guide on a wall axis");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guideSettingsSurviveDropsPlacementAndWorldLoading(final GameTestHelper helper) {
        final var guide = place(helper, false);
        guide.setShape(OBGuideShape.DOME);
        guide.setBounds(new Bounds(3, 1, 0, 6, 2, 4));
        guide.setColor(0x36C658);
        final BlockPos pos = guide.getBlockPos();
        final CompoundTag saved = guide.saveWithFullMetadata();
        final var loaded = (OBGuideBlockEntity) BlockEntity.loadStatic(pos, guide.getBlockState(), saved);
        helper.assertTrue(loaded != null && loaded.settings().equals(guide.settings()), "The six guide bounds, mode, or color failed to reload");

        final var level = helper.getLevel();
        level.destroyBlock(pos, true);
        final ItemStack picked = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(OBItems.GUIDE.get())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(!picked.isEmpty() && picked.getTag().equals(guide.settings()), "Breaking the guide lost its settings");
        final FakePlayer player = player(helper, guide);
        player.setItemInHand(InteractionHand.MAIN_HAND, picked.copy());
        final BlockPos wall = pos.east();
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(wall).relative(Direction.WEST, 0.5), Direction.WEST, wall, false));
        helper.assertTrue(level.getBlockEntity(pos) instanceof OBGuideBlockEntity placed && placed.orientation() == OBGuideOrientation.ZN_XN
                && placed.settings().equals(loaded.settings()), "Replacing the saved Guide lost its settings or wall orientation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guideRedstoneCanBePoweredInvertedOrIgnored(final GameTestHelper helper) {
        final var guide = place(helper, true);
        final var level = helper.getLevel();
        final var config = OBServerConfig.GUIDE_REDSTONE;
        final int previous = config.get();
        try {
            config.set(1);
            helper.assertTrue(!guide.active(), "An unpowered Builder Guide was active");
            level.setBlockAndUpdate(guide.getBlockPos().south(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            helper.assertTrue(guide.getBlockState().getValue(BlockStateProperties.POWERED) && guide.active(), "The guide did not react to redstone");
            config.set(-1);
            helper.assertTrue(!guide.active(), "Inversion did not disable the powered Builder Guide");
            level.removeBlock(guide.getBlockPos().south(), false);
            helper.assertTrue(guide.active(), "The inverted guide stayed disabled after removal of its signal");
            config.set(0);
            helper.assertTrue(guide.active(), "The always-on setting did not ignore redstone");
        } finally {
            config.set(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void builderPlacesOneSurvivalBlockAndDoesNotSpendOnForgeDenial(final GameTestHelper helper) {
        final var guide = place(helper, true);
        final var level = helper.getLevel();
        final BlockPos pos = guide.getBlockPos();
        level.setBlockAndUpdate(pos.south(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        guide.setShape(OBGuideShape.AXES);
        guide.setBounds(new Bounds(0, 0, 0, 3, 0, 0));
        helper.assertTrue(guide.points().equals(java.util.List.of(new BlockPos(3, 0, 0), new BlockPos(2, 0, 0))),
                "The builder did not protect its access space or prefer the farther axial marker");
        final var player = player(helper, guide);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_LOG, 4));
        final var attempts = new AtomicInteger();
        final Consumer<BlockEvent.EntityPlaceEvent> veto = event -> {
            if (event.getEntity() == player && event.getPos().equals(pos.east(3))) {
                attempts.incrementAndGet();
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(veto);
        try {
            use(guide, player, clickTop(guide));
            helper.assertTrue(attempts.get() == 1 && level.isEmptyBlock(pos.east(3)) && level.getBlockState(pos.east(2)).is(Blocks.OAK_LOG)
                    && player.getMainHandItem().getCount() == 3, "The builder consumed a denied block or did not continue to the next marker");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(veto);
        }
        use(guide, player, clickTop(guide));
        helper.assertTrue(level.getBlockState(pos.east(3)).is(Blocks.OAK_LOG) && player.getMainHandItem().getCount() == 2,
                "The next gesture did not place exactly one remaining block");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeBuilderFillsWithoutDestroyingProtectedOrStoredBlocks(final GameTestHelper helper) {
        final var guide = place(helper, true);
        final var level = helper.getLevel();
        final BlockPos pos = guide.getBlockPos();
        level.setBlockAndUpdate(pos.south(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.OBSIDIAN.defaultBlockState());
        guide.setShape(OBGuideShape.AXES);
        guide.setBounds(new Bounds(4, 0, 0, 4, 0, 0));
        level.setBlockAndUpdate(pos.east(2), Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(pos.east(3), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.west(3), Blocks.COBBLESTONE.defaultBlockState());
        level.setBlockAndUpdate(pos.west(4), Blocks.GRANITE.defaultBlockState());

        final var player = player(helper, guide);
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.QUARTZ_BLOCK, 1));
        final Consumer<BlockEvent.BreakEvent> denyBreak = event -> {
            if (event.getPlayer() == player && event.getPos().equals(pos.west(3))) event.setCanceled(true);
        };
        final Consumer<BlockEvent.EntityPlaceEvent> denyPlace = event -> {
            if (event.getEntity() == player && event.getPos().equals(pos.west(4))) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(denyBreak);
        MinecraftForge.EVENT_BUS.addListener(denyPlace);
        try {
            use(guide, player, clickTop(guide));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(denyBreak);
            MinecraftForge.EVENT_BUS.unregister(denyPlace);
        }
        helper.assertTrue(level.getBlockState(pos.east(2)).is(Blocks.CHEST) && level.getBlockState(pos.west(3)).is(Blocks.COBBLESTONE)
                && level.getBlockState(pos.west(4)).is(Blocks.GRANITE), "Creative fill replaced a stored block or a protected destination");
        helper.assertTrue(level.getBlockState(pos.east(3)).is(Blocks.QUARTZ_BLOCK) && level.getBlockState(pos.east(4)).is(Blocks.QUARTZ_BLOCK)
                && level.getBlockState(pos.west(2)).is(Blocks.QUARTZ_BLOCK) && player.getMainHandItem().getCount() == 1,
                "Creative obsidian fill did not place and replace blocks in one gesture without spending inventory");
        helper.succeed();
    }

    private static OBGuideBlockEntity place(final GameTestHelper helper, final boolean builder) {
        helper.setBlock(POS, (builder ? OBBlocks.BUILDER_GUIDE : OBBlocks.GUIDE).get());
        return (OBGuideBlockEntity) helper.getBlockEntity(POS);
    }

    private static FakePlayer player(final GameTestHelper helper, final OBGuideBlockEntity guide) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "GuideQA"));
        final Vec3 position = Vec3.atBottomCenterOf(guide.getBlockPos().north(2));
        player.moveTo(position.x, position.y, position.z, 0, 0);
        return player;
    }

    private static BlockHitResult clickTop(final OBGuideBlockEntity guide) {
        return new BlockHitResult(Vec3.atCenterOf(guide.getBlockPos()).relative(Direction.UP, 0.5), Direction.UP, guide.getBlockPos(), false);
    }

    private static void use(final OBGuideBlockEntity guide, final FakePlayer player, final BlockHitResult hit) {
        guide.getBlockState().use(guide.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }
}
