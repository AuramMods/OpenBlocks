package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBElevatorBlock;
import art.arcane.openblocks.block.OBElevatorRotatingBlock;
import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import art.arcane.openblocks.block.entity.OBPaintCanBlockEntity;
import art.arcane.openblocks.block.entity.OBPaintMixerBlockEntity;
import art.arcane.openblocks.item.OBPaintBrushItem;
import art.arcane.openblocks.item.OBPaintCanItem;
import art.arcane.openblocks.item.OBPaintColors;
import art.arcane.openblocks.menu.OBPaintMixerMenu;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class PaintGameTests {

    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final int CORAL = 0xE6324C;

    @GameTest(template = "empty")
    public static void paintRecipesCraftAndRestoreAllBrushColors(final GameTestHelper helper) {
        final var mixerGrid = ComponentRecipeGameTests.input(helper, Items.OBSIDIAN, Items.OBSIDIAN, Items.OBSIDIAN,
                Items.IRON_INGOT, Items.AIR, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, mixerGrid).is(OBItems.PAINT_MIXER.get()), "Original mixer recipe failed");

        final var brushGrid = ComponentRecipeGameTests.input(helper, Items.WHITE_WOOL, Items.AIR, Items.AIR,
                Items.AIR, Items.STICK, Items.AIR, Items.AIR, Items.AIR, Items.STICK);
        final ItemStack brush = ComponentRecipeGameTests.craft(helper, brushGrid);
        helper.assertTrue(brush.is(OBItems.PAINTBRUSH.get()) && OBPaintBrushItem.color(brush) == OBCanvasBlockEntity.UNPAINTED,
                "Original brush recipe did not make a blank brush");
        for (final DyeColor dye : DyeColor.values()) {
            final var colorGrid = ComponentRecipeGameTests.input(helper, OBItems.PAINTBRUSH.get(), DyeItem.byColor(dye));
            colorGrid.setItem(0, OBPaintBrushItem.create(CORAL));
            final ItemStack result = ComponentRecipeGameTests.craft(helper, colorGrid);
            helper.assertTrue(result.is(OBItems.PAINTBRUSH.get()) && OBPaintBrushItem.color(result) == OBPaintColors.palette(dye),
                    "Brush recipe failed to restore " + dye.getName());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mixerConsumesExactPigmentsAndResumesSavedProgress(final GameTestHelper helper) {
        final var mixer = mixer(helper);
        final var input = mixer.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH).orElseThrow(IllegalStateException::new);
        helper.assertTrue(input.insertItem(1, new ItemStack(Items.BLUE_DYE), false).is(Items.BLUE_DYE)
                && input.insertItem(0, new ItemStack(Items.MILK_BUCKET), true).isEmpty() && input.getStackInSlot(0).isEmpty(),
                "Mixer accepted the wrong dye or changed inventory in simulation");
        input.insertItem(0, new ItemStack(Items.MILK_BUCKET), false);
        mixer.start(0x804020);
        helper.assertTrue(!mixer.mixing() && input.getStackInSlot(0).is(Items.MILK_BUCKET), "A color started with missing pigments");

        input.insertItem(2, new ItemStack(Items.MAGENTA_DYE), false);
        input.insertItem(3, new ItemStack(Items.YELLOW_DYE), false);
        input.insertItem(4, new ItemStack(Items.BLACK_DYE), false);
        final Player player = player(helper);
        final var menu = new OBPaintMixerMenu(1, player.getInventory(), mixer);
        menu.requestColor(player, 0x804020);
        helper.assertTrue(menu.color() == 0x804020 && menu.mixing(), "The open menu did not start the requested RGB batch");
        for (int tick = 0; tick < 187; tick++) tick(helper, mixer);
        final var saved = mixer.saveWithFullMetadata();
        final var fresh = (OBPaintMixerBlockEntity) BlockEntity.loadStatic(mixer.getBlockPos(), mixer.getBlockState(), saved);
        helper.assertTrue(fresh != null && fresh.mixing() && fresh.progress() == 187 && fresh.items().getStackInSlot(0).is(Items.MILK_BUCKET),
                "Loading a work in progress lost the milk or elapsed ticks");

        for (int tick = 187; tick < OBPaintMixerBlockEntity.DURATION - 1; tick++) tick(helper, fresh);
        helper.assertTrue(fresh.items().getStackInSlot(0).is(Items.MILK_BUCKET), "The batch completed before 300 ticks");
        tick(helper, fresh);
        final ItemStack result = fresh.items().getStackInSlot(0);
        helper.assertTrue(!fresh.mixing() && OBPaintCanItem.color(result) == 0x804020 && OBPaintCanItem.amount(result) == 30
                && fresh.pigment(0) == 0 && fresh.pigment(1) == 5_000 && fresh.pigment(2) == 2_500 && fresh.pigment(3) == 5_020,
                "The finished batch used the wrong CMYK proportions or paint-can color");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void changingMixerInputCancelsAndBreakPreservesOnlyStoredPigment(final GameTestHelper helper) {
        final var mixer = mixer(helper);
        final Player player = player(helper);
        final var menu = new OBPaintMixerMenu(4, player.getInventory(), mixer);
        mixer.items().insertItem(0, OBPaintCanItem.create(CORAL, 6), false);
        mixer.items().insertItem(4, new ItemStack(Items.BLACK_DYE, 3), false);
        menu.requestColor(player, 0);
        tick(helper, mixer);
        helper.assertTrue(menu.getSlot(0).getMaxStackSize(new ItemStack(Items.MILK_BUCKET)) == 1 && mixer.mixing() && mixer.progress() == 1,
                "A menu stack-limit query reset an active batch");
        final ItemStack previous = menu.quickMoveStack(player, 0);
        helper.assertTrue(OBPaintCanItem.amount(previous) == 6 && !mixer.mixing() && mixer.progress() == 0
                && mixer.pigment(3) == OBPaintMixerBlockEntity.UNIT * 2, "Removing the can changed its contents or spent reserved pigment");
        helper.getLevel().destroyBlock(mixer.getBlockPos(), true);
        helper.assertItemEntityCountIs(Items.BLACK_DYE, POS, 2, 1);
        helper.assertTrue(!menu.stillValid(player), "Breaking the mixer left its menu valid");
        final ItemStack packed = drops(helper, mixer.getBlockPos()).stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(OBItems.PAINT_MIXER.get())).findFirst().orElse(ItemStack.EMPTY);
        final var packedData = net.minecraft.world.item.BlockItem.getBlockEntityData(packed);
        helper.assertTrue(packedData != null && packedData.getIntArray("Pigment")[3] == OBPaintMixerBlockEntity.UNIT * 2
                && !packedData.contains("Inventory"), "Broken mixer failed to preserve reserves separately from its inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cansKeepPartialPaintAndGiveOneBucketWhenEmpty(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final Player player = player(helper);
        final BlockPos pos = helper.absolutePos(POS);
        helper.setBlock(POS, OBBlocks.PAINT_CAN.get());
        var can = (OBPaintCanBlockEntity) level.getBlockEntity(pos);
        can.fillFrom(OBPaintCanItem.create(CORAL, 3));
        final ItemStack brush = OBPaintBrushItem.create(0xFFFFFF);
        brush.setDamageValue(15);
        player.setItemInHand(InteractionHand.MAIN_HAND, brush);
        dip(player, pos);
        helper.assertTrue(OBPaintBrushItem.color(brush) == CORAL && brush.getDamageValue() == 0 && can.amount() == 2,
                "Dipping did not recharge the brush or used the wrong amount");
        level.destroyBlock(pos, true);
        final ItemStack savedCan = drops(helper, pos).stream().map(ItemEntity::getItem).filter(stack -> stack.is(OBItems.PAINT_CAN.get()))
                .findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(OBPaintCanItem.color(savedCan) == CORAL && OBPaintCanItem.amount(savedCan) == 2, "A broken can lost its color or amount");
        helper.setBlock(POS, OBBlocks.PAINT_CAN.get());
        can = (OBPaintCanBlockEntity) level.getBlockEntity(pos);
        can.fillFrom(savedCan);
        dip(player, pos);
        dip(player, pos);
        helper.assertTrue(level.isEmptyBlock(pos), "An empty paint can stayed in the world");
        helper.assertItemEntityCountIs(Items.BUCKET, POS, 2, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void paintPreservesBlockStateAndRinsesOneFaceAtATime(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final Player player = player(helper);
        final BlockPos pos = helper.absolutePos(POS);
        final var original = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.HALF, Half.TOP).setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlockAndUpdate(pos, original);
        final ItemStack brush = OBPaintBrushItem.create(CORAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, brush);
        helper.assertTrue(use(player, pos, Direction.NORTH).consumesAction(), "Brush did not paint stairs");
        final var canvas = (OBCanvasBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(canvas.original() == original && canvas.color(Direction.NORTH) == CORAL
                && canvas.color(Direction.SOUTH) == OBCanvasBlockEntity.UNPAINTED && brush.getDamageValue() == 1,
                "Painting one face changed other faces, the original stair state or brush durability");
        helper.assertTrue(canvas.getBlockState().getCollisionShape(level, pos).bounds().equals(original.getCollisionShape(level, pos).bounds())
                && level.getFluidState(pos).isSource(), "Painting stairs changed collision or drained their water");
        final var saved = (OBCanvasBlockEntity) BlockEntity.loadStatic(pos, canvas.getBlockState(), canvas.saveWithFullMetadata());
        helper.assertTrue(saved != null && saved.original() == original && saved.color(Direction.NORTH) == CORAL,
                "Loading the canvas lost the waterlogged original or face paint");

        player.setShiftKeyDown(true);
        use(player, pos, Direction.UP);
        player.setShiftKeyDown(false);
        OBPaintBrushItem.loadPaint(brush, 0x5533AA);
        brush.setDamageValue(OBPaintBrushItem.MAX_USES - 1);
        use(player, pos, Direction.EAST);
        helper.assertTrue(OBPaintBrushItem.color(brush) == OBCanvasBlockEntity.UNPAINTED && brush.getDamageValue() == 0,
                "The final charge destroyed the brush or left paint on it");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.SQUEEGEE.get()));
        use(player, pos, Direction.NORTH);
        helper.assertTrue(canvas.color(Direction.NORTH) == OBCanvasBlockEntity.UNPAINTED && canvas.color(Direction.SOUTH) == CORAL
                && canvas.color(Direction.EAST) == 0x5533AA && level.getBlockEntity(pos) == canvas, "Cleaning one face changed another face");
        player.setShiftKeyDown(true);
        use(player, pos, Direction.UP);
        helper.assertTrue(level.getBlockState(pos) == original, "Cleaning the last paint failed to restore the exact original stairs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void brushesUseVanillaColorsAndRespectForgePlacementDenials(final GameTestHelper helper) {
        final Player player = player(helper);
        final var level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(POS);
        level.setBlockAndUpdate(pos, Blocks.WHITE_WOOL.defaultBlockState());
        final ItemStack red = OBPaintBrushItem.create(OBPaintColors.palette(DyeColor.RED));
        player.setItemInHand(InteractionHand.MAIN_HAND, red);
        helper.assertTrue(use(player, pos, Direction.NORTH).consumesAction() && level.getBlockState(pos).is(Blocks.RED_WOOL),
                "The legacy red preset did not recolor vanilla wool");
        final BlockPos elevator = pos.east(2);
        level.setBlockAndUpdate(elevator, OBBlocks.ELEVATOR_ROTATING.get().defaultBlockState()
                .setValue(OBElevatorRotatingBlock.FACING, Direction.EAST));
        use(player, elevator, Direction.UP);
        final var coloredElevator = level.getBlockState(elevator);
        helper.assertTrue(coloredElevator.is(OBBlocks.ELEVATOR_ROTATING.get()) && coloredElevator.getValue(OBElevatorBlock.COLOR) == DyeColor.RED
                && coloredElevator.getValue(OBElevatorRotatingBlock.FACING) == Direction.EAST, "Recoloring a rotating elevator lost its facing");

        final ItemStack custom = OBPaintBrushItem.create(CORAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, custom);
        final var attempts = new AtomicInteger();
        final Consumer<BlockEvent.EntityPlaceEvent> protection = event -> {
            if (event.getEntity() == player && event.getPos().equals(pos)) {
                attempts.incrementAndGet();
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(protection);
        try {
            helper.assertTrue(use(player, pos, Direction.NORTH) == InteractionResult.FAIL && attempts.get() == 1
                    && level.getBlockState(pos).is(Blocks.RED_WOOL) && custom.getDamageValue() == 0,
                    "A denied paint replacement changed the block or brush, or posted the event twice");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(protection);
        }
        helper.assertTrue(use(player, pos, Direction.NORTH).consumesAction(), "The same arbitrary paint failed outside the protection hook");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.SQUEEGEE.get()));
        MinecraftForge.EVENT_BUS.addListener(protection);
        try {
            helper.assertTrue(use(player, pos, Direction.NORTH) == InteractionResult.FAIL
                    && level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas && canvas.color(Direction.NORTH) == CORAL,
                    "Denying last-face cleanup lost the original paint");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(protection);
        }
        level.destroyBlock(pos, true);
        helper.assertItemEntityCountIs(Blocks.RED_WOOL.asItem(), POS, 2, 1);
        helper.assertItemEntityCountIs(OBItems.CANVAS.get(), POS, 2, 0);
        helper.succeed();
    }

    private static OBPaintMixerBlockEntity mixer(final GameTestHelper helper) {
        helper.setBlock(POS, OBBlocks.PAINT_MIXER.get());
        return (OBPaintMixerBlockEntity) helper.getBlockEntity(POS);
    }

    private static void tick(final GameTestHelper helper, final OBPaintMixerBlockEntity mixer) {
        OBPaintMixerBlockEntity.tick(helper.getLevel(), mixer.getBlockPos(), mixer.getBlockState(), mixer);
    }

    private static Player player(final GameTestHelper helper) {
        final Player player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PaintQA"));
        final Vec3 feet = helper.absoluteVec(new Vec3(2.5, 2, 1.5));
        player.moveTo(feet.x, feet.y, feet.z, 0, 0);
        return player;
    }

    private static java.util.List<ItemEntity> drops(final GameTestHelper helper, final BlockPos origin) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(2));
    }

    private static BlockHitResult hit(final BlockPos pos, final Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
    }

    private static void dip(final Player player, final BlockPos pos) {
        final var level = player.level();
        final var state = level.getBlockState(pos);
        state.use(level, player, InteractionHand.MAIN_HAND, hit(pos, Direction.UP));
    }

    private static InteractionResult use(final Player player, final BlockPos pos, final Direction face) {
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos, face)));
    }
}
