package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.item.OBSleepingBagItem;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class SleepingBagGameTests {

    private static final BlockPos GROUND = new BlockPos(2, 1, 2);

    @GameTest(template = "empty")
    public static void sleepingBagKeepsItsLegacyRecipe(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.BLUE_CARPET, Items.BLUE_CARPET, Items.AIR,
                Items.WHITE_WOOL, Items.WHITE_WOOL, Items.WHITE_WOOL, Items.BLUE_CARPET, Items.BLUE_CARPET, Items.WHITE_WOOL);
        final var bag = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(bag.is(OBItems.SLEEPING_BAG.get()) && bag.getMaxStackSize() == 1,
                "Blue carpet and white wool must craft one wearable Sleeping Bag");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sleepingBagReturnsNamedBagAndDisplacedArmor(final GameTestHelper helper) {
        final var player = player(helper, GameType.SURVIVAL);
        final var events = new SleepEvents(player);
        final var slot = player.getInventory();
        slot.selected = 3;
        final var bag = new ItemStack(OBItems.SLEEPING_BAG.get());
        bag.setHoverName(Component.literal("Camp bag"));
        final var armor = new ItemStack(Items.DIAMOND_CHESTPLATE);
        armor.setDamageValue(37);
        player.setItemSlot(EquipmentSlot.CHEST, armor);
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        final var respawn = player.blockPosition().east(20);
        player.setRespawnPosition(helper.getLevel().dimension(), respawn, 73, true, false);
        player.sleepCounter = 100;

        MinecraftForge.EVENT_BUS.register(events);
        try {
            helper.assertTrue(rightClick(player).consumesAction() && player.isSleeping() && player.getSleepTimer() == 0,
                    "The sleeping bag did not enter normal player sleep with a fresh countdown");
            final var worn = player.getItemBySlot(EquipmentSlot.CHEST);
            helper.assertTrue(OBSleepingBagItem.hasSlept(worn) && player.getMainHandItem() == armor,
                    "The damaged chestplate was not swapped to the selected slot");
            final var location = player.getSleepingPos().orElseThrow();
            helper.assertTrue(ForgeEventFactory.fireSleepingLocationCheck(player, location)
                    && helper.getLevel().isEmptyBlock(location), "Sleeping did not persist on the unmodified ground");
            helper.assertTrue(respawn.equals(player.getRespawnPosition()) && player.getRespawnAngle() == 73 && player.isRespawnForced(),
                    "Entering the bag changed the respawn metadata");
            helper.setBlock(GROUND, Blocks.AIR);
            helper.assertTrue(!ForgeEventFactory.fireSleepingLocationCheck(player, location), "The bag kept sleeping after its support disappeared");
            wake(player);
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST) == armor && player.getMainHandItem().getCount() == 1
                    && ItemStack.isSameItemSameTags(player.getMainHandItem(), bag) && armor.getDamageValue() == 37,
                    "Waking did not return the exact bag and armor");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(events);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sleepingBagKeepsOldStackAndArmorCountsInCreative(final GameTestHelper helper) {
        final var player = player(helper, GameType.CREATIVE);
        final var events = new SleepEvents(player);
        final var armor = new ItemStack(Items.ELYTRA);
        armor.setDamageValue(13);
        player.setItemSlot(EquipmentSlot.CHEST, armor);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.SLEEPING_BAG.get(), 3));
        MinecraftForge.EVENT_BUS.register(events);
        try {
            helper.assertTrue(rightClick(player).consumesAction() && player.isSleeping(), "The legacy stack was rejected");
            final var inventory = player.getInventory();
            final var worn = player.getItemBySlot(EquipmentSlot.CHEST);
            helper.assertTrue(player.getMainHandItem().getCount() == 2 && worn.getCount() == 1
                    && inventory.getItem(1) == armor && armor.getCount() == 1 && armor.getDamageValue() == 13,
                    "Creative use duplicated either the sleeping bags or the displaced elytra");
            wake(player);
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST) == armor && inventory.getItem(1).is(OBItems.SLEEPING_BAG.get())
                    && inventory.getItem(1).getCount() == 1 && player.getMainHandItem().getCount() == 2,
                    "Waking did not conserve the original count and equipment");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(events);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sleepingBagRespectsForgeSleepDenialAndManualEquipment(final GameTestHelper helper) {
        final var player = player(helper, GameType.SURVIVAL);
        final var events = new SleepEvents(player);
        final var bag = new ItemStack(OBItems.SLEEPING_BAG.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        player.setItemInHand(InteractionHand.OFF_HAND, bag.copy());

        MinecraftForge.EVENT_BUS.register(events);
        try {
            helper.assertTrue(player.gameMode.useItem(player, player.level(), player.getOffhandItem(), InteractionHand.OFF_HAND) == InteractionResult.PASS,
                    "Using the bag in the offhand changed the player");
            events.denyTime = true;
            helper.assertTrue(rightClick(player) == InteractionResult.FAIL && player.getMainHandItem() == bag && !player.isSleeping(),
                    "A rejected time check moved the bag into the armor slot");
            events.denyTime = false;
            events.denyUse = true;
            helper.assertTrue(rightClick(player) == InteractionResult.FAIL && !player.isSleeping() && player.getMainHandItem() == bag,
                    "The sleeping bag ignored the Forge sleep event");
            events.denyUse = false;
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.CHEST, bag);
            player.getInventory().tick();
            helper.assertTrue(player.isSleeping() && OBSleepingBagItem.hasSlept(bag), "Equipping manually did not start sleep");
            wake(player);
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && player.getMainHandItem().is(OBItems.SLEEPING_BAG.get()),
                    "The manually equipped bag did not return to the inventory");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(events);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sleepingBagKeepsItemsWhenAFullInventoryDropIsDenied(final GameTestHelper helper) {
        final var player = player(helper, GameType.SURVIVAL);
        final var events = new SleepEvents(player);
        final var bag = new ItemStack(OBItems.SLEEPING_BAG.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        MinecraftForge.EVENT_BUS.register(events);
        try {
            helper.assertTrue(rightClick(player).consumesAction(), "The bag could not enter sleep");
            final var equipped = player.getItemBySlot(EquipmentSlot.CHEST);
            final var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.items.size(); slot++) inventory.setItem(slot, new ItemStack(Items.STONE, 64));
            events.denyDrop = true;
            wake(player);
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST) == equipped && OBSleepingBagItem.hasSlept(equipped),
                    "A canceled item spawn lost the bag or let it restart sleep");
            events.denyDrop = false;
            player.getInventory().tick();
            final var drops = player.level().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(1),
                    item -> item.getItem().is(OBItems.SLEEPING_BAG.get()));
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && drops.size() == 1
                    && drops.get(0).getItem().getCount() == 1 && !OBSleepingBagItem.hasSlept(drops.get(0).getItem())
                    && player.getUUID().equals(drops.get(0).target) && player.getMainHandItem().getCount() == 64,
                    "The full inventory did not keep its items and receive one reserved bag drop");
            drops.get(0).discard();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(events);
        }
        helper.succeed();
    }

    private static FakePlayer player(final GameTestHelper helper, final GameType mode) {
        helper.setBlock(GROUND, Blocks.STONE);
        final BlockPos pos = helper.absolutePos(GROUND.above());
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "SleepingBagQA"));
        player.setGameMode(mode);
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        return player;
    }

    private static InteractionResult rightClick(final FakePlayer player) {
        return player.gameMode.useItem(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    private static void wake(final FakePlayer player) {
        player.stopSleepInBed(false, true);
        player.getInventory().tick();
    }

    public static final class SleepEvents {
        private final FakePlayer player;
        private boolean denyUse;
        private boolean denyTime;
        private boolean denyDrop;

        private SleepEvents(final FakePlayer player) { this.player = player; }

        @SubscribeEvent
        public void allowTime(final SleepingTimeCheckEvent event) {
            if (event.getEntity() == player) event.setResult(denyTime ? Event.Result.DENY : Event.Result.ALLOW);
        }

        @SubscribeEvent
        public void allowSleep(final PlayerSleepInBedEvent event) {
            if (denyUse && event.getEntity() == player) event.setResult(net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM);
        }

        @SubscribeEvent
        public void allowDrop(final EntityJoinLevelEvent event) {
            if (denyDrop && event.getEntity() instanceof ItemEntity item && player.getUUID().equals(item.target)) event.setCanceled(true);
        }
    }
}
