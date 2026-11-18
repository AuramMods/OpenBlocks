package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBRedstoneMachineBlock;
import art.arcane.openblocks.block.entity.OBRedstoneMachineBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.menu.OBRedstoneMachineMenu;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class RedstoneMachineGameTests {

    private static final BlockPos MACHINE = new BlockPos(3, 3, 3);
    private static final BlockPos TARGET = MACHINE.east();
    private static final BlockPos POWER = MACHINE.north();

    @GameTest(template = "empty")
    public static void redstoneMachinesCraftAndFollowLegacyPlacementDirections(final GameTestHelper helper) {
        assertRecipe(helper, Items.DIAMOND_PICKAXE, OBItems.BLOCK_BREAKER.get());
        assertRecipe(helper, Items.PISTON, OBItems.BLOCK_PLACER.get());
        assertRecipe(helper, Items.HOPPER, OBItems.ITEM_DROPPER.get());

        final ItemStack pickaxe = new ItemStack(Items.WOODEN_PICKAXE);
        for (final Block machine : List.of(OBBlocks.BLOCK_BREAKER.get(), OBBlocks.BLOCK_PLACER.get(), OBBlocks.ITEM_DROPPER.get())) {
            final var state = machine.defaultBlockState();
            helper.assertTrue(pickaxe.isCorrectToolForDrops(state) && state.getDestroySpeed(helper.getLevel(), helper.absolutePos(MACHINE)) == 1.0F,
                    "Mining requirements differ from the legacy machine: " + machine);
        }

        final Player player = helper.makeMockSurvivalPlayer();
        final BlockPos support = MACHINE.below();
        helper.setBlock(support, Blocks.STONE);
        final BlockPos supportPos = helper.absolutePos(support);
        final Vec3 origin = Vec3.atCenterOf(supportPos);
        final var onTop = new BlockHitResult(origin.add(0.0, 0.5, 0.0), Direction.UP, supportPos, false);
        player.moveTo(origin.x, origin.y + 1.5, origin.z + 2.5, 180.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.BLOCK_BREAKER.get()));
        helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, onTop)).consumesAction(),
                "Normal breaker item placement failed");
        helper.assertTrue(helper.getBlockState(MACHINE).getValue(OBRedstoneMachineBlock.FACING) == Direction.SOUTH,
                "The breaker did not face back toward its placer");
        helper.setBlock(MACHINE, Blocks.AIR);

        player.moveTo(origin.x, origin.y + 2.5, origin.z, 180.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.ITEM_DROPPER.get()));
        helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, onTop)).consumesAction(),
                "Normal dropper item placement failed");
        helper.assertTrue(helper.getBlockState(MACHINE).getValue(OBRedstoneMachineBlock.FACING) == Direction.DOWN,
                "The dropper did not orient its nozzle away from the player above it");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blockBreakerHarvestsAndRoutesDropsWithoutScoopingExistingItems(final GameTestHelper helper) {
        final var breaker = place(helper, OBBlocks.BLOCK_BREAKER.get(), Direction.EAST);
        final var access = items(breaker, Direction.WEST);
        helper.assertTrue(access.getSlots() == 1 && access.insertItem(0, new ItemStack(Items.COAL), false).getCount() == 1,
                "The breaker did not retain its legacy non-insertable connection");
        final BlockPos chestPos = MACHINE.west();
        helper.setBlock(chestPos, Blocks.CHEST);
        final var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
        final BlockPos target = helper.absolutePos(TARGET);
        final Vec3 center = Vec3.atCenterOf(target);
        final var existing = new ItemEntity(helper.getLevel(), center.x, center.y + 0.25, center.z,
                new ItemStack(Items.NETHER_STAR), 0.0, 0.0, 0.0);
        existing.setNoGravity(true);
        helper.getLevel().addFreshEntity(existing);
        helper.setBlock(TARGET, Blocks.IRON_ORE);
        helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            helper.assertTrue(powered(helper) && chest.getItem(0).is(Items.RAW_IRON) && chest.getItem(0).getCount() == 1,
                    "The breaker did not harvest ore with an appropriate tool and insert the drop behind it");
            helper.assertTrue(!existing.isRemoved() && chest.getItem(1).isEmpty(), "The breaker collected a pre-existing item");
            existing.discard();

            for (int slot = 0; slot < chest.getContainerSize(); slot++) chest.setItem(slot, new ItemStack(Items.STONE, 64));
            helper.setBlock(TARGET, Blocks.COBBLESTONE);
            helper.runAfterDelay(3, () -> {
                helper.assertBlockPresent(Blocks.AIR, TARGET);
                helper.assertTrue(countDrops(helper, target, Items.COBBLESTONE) == 1,
                        "The breaker lost or duplicated loot when the rear inventory was full");
                helper.setBlock(POWER, Blocks.AIR);
                helper.setBlock(TARGET, Blocks.IRON_ORE);
                helper.runAfterDelay(3, () -> {
                    helper.assertBlockPresent(Blocks.IRON_ORE, TARGET);
                    helper.assertTrue(!powered(helper), "The breaker stayed powered after signal removal");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty")
    public static void blockBreakerHonorsForgeBreakCancellationAndUnbreakableBlocks(final GameTestHelper helper) {
        place(helper, OBBlocks.BLOCK_BREAKER.get(), Direction.EAST);
        final BlockPos target = helper.absolutePos(TARGET);
        final List<Player> actors = new ArrayList<>();
        final Consumer<BlockEvent.BreakEvent> protect = event -> {
            if (event.getLevel() == helper.getLevel() && event.getPos().equals(target) && event.getPlayer() instanceof FakePlayer) {
                actors.add(event.getPlayer());
                event.setCanceled(true);
            }
        };
        helper.setBlock(TARGET, Blocks.STONE);
        // Let the machine's initial power check run before testing a new redstone edge.
        helper.runAfterDelay(2, () -> {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, protect);
            helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
            helper.runAfterDelay(3, () -> {
                MinecraftForge.EVENT_BUS.unregister(protect);
                helper.assertBlockPresent(Blocks.STONE, TARGET);
                helper.assertTrue(actors.size() == 1 && countDrops(helper, target, Items.COBBLESTONE) == 0,
                        "Expected one protected break attempt and no drops. Break attempts: " + actors.size());
                helper.setBlock(TARGET, Blocks.BEDROCK);
                helper.runAfterDelay(3, () -> {
                    helper.assertBlockPresent(Blocks.BEDROCK, TARGET);
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty")
    public static void blockBreakerThrottlesRegeneratingTargetsPerTick(final GameTestHelper helper) {
        place(helper, OBBlocks.BLOCK_BREAKER.get(), Direction.EAST);
        final var level = helper.getLevel();
        final BlockPos target = helper.absolutePos(TARGET);
        final var attempts = new HashMap<Long, Integer>();
        final List<ItemEntity> drops = new ArrayList<>();
        final Consumer<BlockEvent.BreakEvent> count = event -> {
            if (event.getLevel() == level && event.getPos().equals(target) && event.getPlayer() instanceof FakePlayer) {
                attempts.merge(level.getGameTime(), 1, Integer::sum);
            }
        };
        final Consumer<EntityJoinLevelEvent> regenerate = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof ItemEntity drop
                    && drop.blockPosition().equals(target) && drop.getItem().is(Items.COBBLESTONE)) {
                drops.add(drop);
                if (drops.size() < 6) helper.setBlock(TARGET, Blocks.COBBLESTONE);
            }
        };
        final int limit = OBServerConfig.BLOCK_BREAKER_ACTION_LIMIT.get();
        OBServerConfig.BLOCK_BREAKER_ACTION_LIMIT.set(2);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, count);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, regenerate);
        helper.setBlock(TARGET, Blocks.COBBLESTONE);
        helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(8, () -> {
            MinecraftForge.EVENT_BUS.unregister(count);
            MinecraftForge.EVENT_BUS.unregister(regenerate);
            OBServerConfig.BLOCK_BREAKER_ACTION_LIMIT.set(limit);
            helper.assertTrue(drops.size() == 6 && attempts.size() == 3 && attempts.values().stream().allMatch(value -> value == 2),
                    "A regenerating face did not honor the configured two actions per tick: " + attempts);
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void blockPlacerRespondsToInventoryChangesAndUsesNormalBlockOrientation(final GameTestHelper helper) {
        final var placer = place(helper, OBBlocks.BLOCK_PLACER.get(), Direction.EAST);
        final var capability = items(placer, Direction.UP);
        final Player player = playerNear(helper);
        final var menu = (OBRedstoneMachineMenu) placer.createMenu(1, player.getInventory(), player);
        helper.assertTrue(capability.getSlots() == 9 && menu.stillValid(player) && !menu.isDropper(),
                "The placer did not open a nine-slot machine menu");
        helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            capability.insertItem(2, new ItemStack(Items.STONE, 3), false);
            player.getInventory().setItem(9, new ItemStack(Items.PISTON));
            helper.assertTrue(menu.quickMoveStack(player, 9).is(Items.PISTON), "Shift-click did not move the piston into the placer");
            helper.runAfterDelay(3, () -> {
                helper.assertBlockPresent(Blocks.PISTON, TARGET);
                helper.assertTrue(helper.getBlockState(TARGET).getValue(PistonBaseBlock.FACING) == Direction.EAST
                                && capability.getStackInSlot(0).isEmpty() && capability.getStackInSlot(2).getCount() == 3,
                        "The placer did not use the first slot with the expected facing and count");
                helper.setBlock(TARGET, Blocks.AIR);
                helper.runAfterDelay(3, () -> {
                    helper.assertBlockPresent(Blocks.STONE, TARGET);
                    helper.assertTrue(capability.getStackInSlot(2).getCount() == 2,
                            "The placer did not replace a cleared target while still powered");
                    helper.setBlock(POWER, Blocks.AIR);
                    helper.setBlock(TARGET, Blocks.AIR);
                    helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
                    helper.setBlock(POWER, Blocks.AIR);
                    helper.runAfterDelay(3, () -> {
                        helper.assertBlockPresent(Blocks.STONE, TARGET);
                        helper.assertTrue(!powered(helper) && capability.getStackInSlot(2).getCount() == 1,
                                "A short redstone pulse did not trigger exactly one placement");
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = "empty")
    public static void blockPlacerReturnsItemsWhenForgeCancelsPlacement(final GameTestHelper helper) {
        final var placer = place(helper, OBBlocks.BLOCK_PLACER.get(), Direction.EAST);
        final BlockPos target = helper.absolutePos(TARGET);
        final List<BlockPos> positions = new ArrayList<>();
        final Consumer<BlockEvent.EntityPlaceEvent> cancel = event -> {
            if (event.getLevel() == helper.getLevel() && event.getPos().equals(target) && event.getEntity() instanceof FakePlayer) {
                positions.add(event.getPos());
                event.setCanceled(true);
            }
        };
        placer.items().setStackInSlot(3, new ItemStack(Items.GOLD_BLOCK, 2));
        helper.runAfterDelay(2, () -> {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityPlaceEvent.class, cancel);
            helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
            helper.runAfterDelay(4, () -> {
                MinecraftForge.EVENT_BUS.unregister(cancel);
                helper.assertBlockPresent(Blocks.AIR, TARGET);
                helper.assertTrue(positions.size() == 1 && placer.items().getStackInSlot(3).getCount() == 2
                                && countDrops(helper, target, Items.GOLD_BLOCK) == 0,
                        "Expected one canceled placement attempt and an unchanged inventory. Placement attempts: " + positions.size());
                helper.setBlock(POWER, Blocks.AIR);
                helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
                helper.runAfterDelay(3, () -> {
                    helper.assertBlockPresent(Blocks.GOLD_BLOCK, TARGET);
                    helper.assertTrue(placer.items().getStackInSlot(3).getCount() == 1, "The retried placement used the wrong quantity");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty")
    public static void itemDropperScalesOnlyOnRisingEdgesAndPersistsItsSettings(final GameTestHelper helper) {
        final var dropper = place(helper, OBBlocks.ITEM_DROPPER.get(), Direction.EAST);
        final var capability = dropper.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN);
        final IItemHandler access = capability.orElseThrow(IllegalStateException::new);
        access.insertItem(4, new ItemStack(Items.EMERALD, 4), false);
        final Player player = playerNear(helper);
        final var menu = (OBRedstoneMachineMenu) dropper.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu.isDropper() && menu.maxSpeedTenths() == 40 && menu.speedTenths() == 0, "The default speed controls were incorrect");
        menu.setLaunchSpeed(player, Integer.MAX_VALUE);
        helper.assertTrue(menu.speedTenths() == 40, "The server did not enforce its speed limit");
        menu.setLaunchSpeed(player, 20);
        helper.assertTrue(menu.clickMenuButton(player, OBRedstoneMachineMenu.TOGGLE_REDSTONE_SCALING), "The menu rejected scaling");

        final var level = helper.getLevel();
        final BlockPos target = helper.absolutePos(TARGET);
        final var emitted = new ArrayList<ItemEntity>();
        final var vectors = new ArrayList<Vec3>();
        final Consumer<ItemTossEvent> listen = event -> {
            if (event.getEntity().level() == level && event.getEntity().blockPosition().equals(target) && event.getPlayer() instanceof FakePlayer) {
                emitted.add(event.getEntity());
                vectors.add(event.getEntity().getDeltaMovement());
                if (emitted.size() == 2) event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ItemTossEvent.class, listen);
        try {
            helper.setBlock(POWER, Blocks.DAYLIGHT_DETECTOR.defaultBlockState().setValue(DaylightDetectorBlock.POWER, 6));
            helper.assertTrue(access.getStackInSlot(4).getCount() == 3 && emitted.size() == 1
                            && Math.abs(vectors.get(0).x - 0.8) < 1e-6 && vectors.get(0).y == 0 && vectors.get(0).z == 0,
                    "A signal strength of six did not eject one item east at 40% launch speed");
            helper.setBlock(POWER, Blocks.DAYLIGHT_DETECTOR.defaultBlockState().setValue(DaylightDetectorBlock.POWER, 15));
            helper.assertTrue(emitted.size() == 1 && access.getStackInSlot(4).getCount() == 3,
                    "A strength change while already powered ejected another item");
            helper.setBlock(POWER, Blocks.AIR);
            helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
            helper.assertTrue(emitted.size() == 2 && vectors.get(1).x == 2.0 && access.getStackInSlot(4).getCount() == 3,
                    "A canceled toss consumed inventory or ignored the signal strength");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listen);
        }

        final BlockPos pos = helper.absolutePos(MACHINE);
        final var restored = (OBRedstoneMachineBlockEntity) BlockEntity.loadStatic(pos, helper.getBlockState(MACHINE), dropper.saveWithFullMetadata());
        helper.assertTrue(restored != null && restored.scalesWithRedstone() && restored.speedTenths() == 20
                        && restored.items().getStackInSlot(4).getCount() == 3 && powered(helper), "Dropper data did not survive a save and load");
        emitted.forEach(ItemEntity::discard);
        level.destroyBlock(pos, true);
        helper.assertTrue(!capability.isPresent() && !menu.stillValid(player) && countDrops(helper, pos, Items.EMERALD) == 3,
                "Breaking the dropper lost inventory or left its capability or menu live");
        helper.succeed();
    }

    private static void assertRecipe(final GameTestHelper helper, final Item component, final Item result) {
        final var recipe = ComponentRecipeGameTests.input(helper, Items.IRON_INGOT, Items.COBBLESTONE, Items.COBBLESTONE,
                component, Items.REDSTONE, Items.COBBLESTONE, Items.IRON_INGOT, Items.COBBLESTONE, Items.COBBLESTONE);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, recipe).is(result), "The machine recipe was missing for " + result);
    }

    private static OBRedstoneMachineBlockEntity place(final GameTestHelper helper, final Block block, final Direction facing) {
        helper.setBlock(MACHINE, block.defaultBlockState().setValue(OBRedstoneMachineBlock.FACING, facing));
        return (OBRedstoneMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE));
    }

    private static boolean powered(final GameTestHelper helper) {
        return helper.getBlockState(MACHINE).getValue(OBRedstoneMachineBlock.POWERED);
    }

    private static int countDrops(final GameTestHelper helper, final BlockPos pos, final Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0)).stream()
                .filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static IItemHandler items(final BlockEntity entity, final Direction face) {
        return entity.getCapability(ForgeCapabilities.ITEM_HANDLER, face).orElseThrow(IllegalStateException::new);
    }

    private static Player playerNear(final GameTestHelper helper) {
        final var player = helper.makeMockSurvivalPlayer();
        final Vec3 position = Vec3.atCenterOf(helper.absolutePos(MACHINE)).add(0, 0, 2);
        player.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        return player;
    }
}
