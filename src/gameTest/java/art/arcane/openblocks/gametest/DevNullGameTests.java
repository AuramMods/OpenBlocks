package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.item.OBDevNullItem;
import art.arcane.openblocks.menu.OBDevNullMenu;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class DevNullGameTests {

    @GameTest(template = "empty")
    public static void devNullCraftsAndRetainsTheExactStoredStack(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.COBBLESTONE, Items.APPLE);
        final ItemStack device = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(device.is(OBItems.DEV_NULL.get()) && device.getMaxStackSize() == 1 && OBDevNullItem.getStored(device).isEmpty(),
                "Cobblestone and an apple did not craft an empty, unstackable device");

        final var sample = new ItemStack(Items.IRON_PICKAXE);
        sample.setDamageValue(31);
        sample.setHoverName(Component.literal("Saved tool"));
        OBDevNullItem.setStored(device, sample);
        final var reloaded = ItemStack.of(device.save(new CompoundTag()));
        helper.assertTrue(ItemStack.matches(OBDevNullItem.getStored(reloaded), sample), "The stored tool lost its name or damage through NBT");
        OBDevNullItem.setStored(reloaded, ItemStack.EMPTY);
        helper.assertTrue(!reloaded.hasTag() && OBDevNullItem.getStored(reloaded).isEmpty(), "Emptying the slot left stale item data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void devNullFillsMatchingDevicesThenDiscardsOnlyOverflow(final GameTestHelper helper) {
        final var player = helper.makeMockSurvivalPlayer();
        final var first = device(new ItemStack(Items.DIRT, 61));
        final var second = device(new ItemStack(Items.DIRT, 60));
        final var other = device(new ItemStack(Items.STONE, 17));
        player.getInventory().setItem(0, first);
        player.getInventory().setItem(5, other);
        player.setItemInHand(InteractionHand.OFF_HAND, second);

        final var drop = drop(helper, new ItemStack(Items.DIRT, 30));
        drop.playerTouch(player);
        helper.assertTrue(drop.isRemoved() && OBDevNullItem.getStored(first).getCount() == 64
                && OBDevNullItem.getStored(second).getCount() == 64 && OBDevNullItem.getStored(other).getCount() == 17
                && player.getInventory().countItem(Items.DIRT) == 0, "The devices lost stored items or let matching overflow reach normal inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void devNullRespectsTagsPickupDelayRecipientsAndForgeCancellation(final GameTestHelper helper) {
        final var player = helper.makeMockSurvivalPlayer();
        final var named = new ItemStack(Items.STONE, 9).setHoverName(Component.literal("Keep this"));
        final var device = device(named);
        final var empty = device(ItemStack.EMPTY);
        player.getInventory().setItem(0, device);
        player.getInventory().setItem(1, empty);

        final var plain = drop(helper, new ItemStack(Items.STONE, 2));
        plain.playerTouch(player);
        helper.assertTrue(plain.isRemoved() && OBDevNullItem.getStored(device).getCount() == 9
                && player.getInventory().countItem(Items.STONE) == 2 && OBDevNullItem.getStored(empty).isEmpty(),
                "An empty device or mismatched tags intercepted a regular pickup");

        final var reserved = drop(helper, named.copyWithCount(2));
        reserved.setPickUpDelay(20);
        reserved.playerTouch(player);
        helper.assertTrue(!reserved.isRemoved(), "The device ignored pickup delay");
        reserved.setNoPickUpDelay();
        reserved.setTarget(UUID.randomUUID());
        reserved.playerTouch(player);
        helper.assertTrue(!reserved.isRemoved(), "The device intercepted another player's reserved drop");

        final Consumer<EntityItemPickupEvent> cancel = event -> {
            if (event.getItem() == reserved) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityItemPickupEvent.class, cancel);
        try {
            reserved.setTarget(player.getUUID());
            reserved.playerTouch(player);
            helper.assertTrue(!reserved.isRemoved() && OBDevNullItem.getStored(device).getCount() == 9,
                    "A Forge-cancelled pickup modified the device");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
        }

        reserved.playerTouch(player);
        helper.assertTrue(reserved.isRemoved() && OBDevNullItem.getStored(device).getCount() == 11,
                "An eligible exact-match drop was not collected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void devNullPlacesFromItsSlotAndRollsBackCancelledPlacement(final GameTestHelper helper) {
        final var player = helper.makeMockSurvivalPlayer();
        final var device = device(new ItemStack(Items.OAK_LOG, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, device);
        final BlockPos support = new BlockPos(3, 2, 3);
        helper.setBlock(support, Blocks.STONE);
        final BlockPos absoluteSupport = helper.absolutePos(support);
        final var hit = new BlockHitResult(Vec3.atCenterOf(absoluteSupport).add(0, 0.5, 0), Direction.UP, absoluteSupport, false);
        final Consumer<BlockEvent.EntityPlaceEvent> cancel = event -> {
            if (event.getEntity() == player && event.getPos().equals(absoluteSupport.above())) event.setCanceled(true);
        };

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityPlaceEvent.class, cancel);
        try {
            helper.assertTrue(!useOn(player, hit) && OBDevNullItem.getStored(device).getCount() == 2
                    && player.getMainHandItem() == device, "A cancelled placement consumed or replaced the device or its contents");
            helper.assertBlockPresent(Blocks.AIR, support.above());
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
        }

        helper.assertTrue(useOn(player, hit) && OBDevNullItem.getStored(device).getCount() == 1
                && player.getMainHandItem() == device, "Placing a block did not spend exactly one from the slot");
        helper.assertBlockPresent(Blocks.OAK_LOG, support.above());
        helper.setBlock(support.above(), Blocks.AIR);
        helper.assertTrue(useOn(player, hit) && OBDevNullItem.getStored(device).isEmpty(), "The final placement did not empty the slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void devNullMenuKeepsSourceLockedAndRefreshesExternalPickups(final GameTestHelper helper) {
        final var player = fakePlayer(helper);
        final var inventory = player.getInventory();
        final var device = device(ItemStack.EMPTY);
        inventory.selected = 2;
        inventory.setItem(2, device);
        inventory.setItem(9, new ItemStack(Items.DIRT, 20));

        final var menu = new OBDevNullMenu(1, inventory, 2);
        player.containerMenu = menu;
        menu.clicked(30, 0, ClickType.PICKUP, player);
        menu.clicked(1, 2, ClickType.SWAP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && inventory.getItem(2) == device && inventory.getItem(9).getCount() == 20,
                "A normal click or number-key swap moved the live backing item");
        helper.assertTrue(!menu.quickMoveStack(player, 1).isEmpty() && OBDevNullItem.getStored(device).getCount() == 20
                && inventory.getItem(9).isEmpty(), "Shift-inserting a stack failed to save it");

        drop(helper, new ItemStack(Items.DIRT, 7)).playerTouch(player);
        helper.assertTrue(menu.getSlot(0).getItem().getCount() == 27 && OBDevNullItem.getStored(device).getCount() == 27,
                "World pickup did not synchronize the open menu");
        helper.assertTrue(!menu.quickMoveStack(player, 0).isEmpty() && OBDevNullItem.getStored(device).isEmpty()
                && inventory.countItem(Items.DIRT) == 27 && inventory.getItem(2) == device, "Shift-taking lost or duplicated stored items");

        final var replacement = device(new ItemStack(Items.STONE));
        inventory.setItem(2, replacement);
        menu.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(!menu.stillValid(player) && menu.getCarried().isEmpty()
                && OBDevNullItem.getStored(replacement).is(Items.STONE), "A stale menu edited a replacement device");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void devNullAdvancementComesFromFiveRealNestedDevices(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "nested_dev_null"));
        final var inventory = player.getInventory();
        final var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(OpenBlocks.id("tma2"));
        final var progress = player.getAdvancements().getOrStartProgress(advancement);
        for (int i = 0; i < 5; i++) inventory.setItem(i, device(ItemStack.EMPTY));
        helper.assertTrue(!progress.isDone(), "Separate devices counted as nesting");

        final var outer = inventory.getItem(0);
        ItemStack inner = ItemStack.EMPTY;
        for (int i = 0; i < 4; i++) inner = device(inner);
        inventory.setItem(9, inner);
        final var menu = new OBDevNullMenu(1, inventory, 0);
        helper.assertTrue(!menu.quickMoveStack(player, 1).isEmpty() && OBDevNullItem.contents(outer).depth() == 5,
                "The menu did not persist five layers");
        helper.assertTrue(progress.isDone(), "Inserting the fourth nested device failed to award Stack Overflow");

        final var newOuter = device(ItemStack.EMPTY);
        inventory.setItem(0, newOuter);
        inventory.setItem(9, outer);
        final var next = new OBDevNullMenu(2, inventory, 0);
        helper.assertTrue(next.quickMoveStack(player, 1).isEmpty() && inventory.getItem(9) == outer
                && OBDevNullItem.getStored(newOuter).isEmpty(), "A sixth layer nested or a failed insertion lost an item");
        helper.succeed();
    }

    private static FakePlayer fakePlayer(final GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "dev_null_test"));
    }

    private static ItemStack device(final ItemStack stack) {
        final var device = new ItemStack(OBItems.DEV_NULL.get());
        OBDevNullItem.setStored(device, stack);
        return device;
    }

    private static ItemEntity drop(final GameTestHelper helper, final ItemStack stack) {
        final BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        final var entity = new ItemEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, stack);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static boolean useOn(final Player player, final BlockHitResult hit) {
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }
}
