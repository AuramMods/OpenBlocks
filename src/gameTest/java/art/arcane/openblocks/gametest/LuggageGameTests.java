package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBLuggage;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBMenuTypes;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class LuggageGameTests {

    private static final BlockPos CENTER = new BlockPos(3, 2, 3);

    @GameTest(template = "empty")
    public static void luggageCraftsOneNormalUnstackableItem(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.STICK, Items.DIAMOND, Items.STICK,
                Items.STICK, Items.CHEST, Items.STICK, Items.STICK, Items.STICK, Items.STICK);
        final var stack = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(stack.is(OBItems.LUGGAGE.get()) && stack.getCount() == 1 && stack.getMaxStackSize() == 1 && !stack.hasFoil(),
                "The Luggage recipe did not craft one ordinary case");
        grid.setItem(4, new ItemStack(Items.ENDER_CHEST));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "An ender chest replaced the recipe's wooden chest");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void luggageSpawnsOnlyOnceAndRetainsOlderStacks(final GameTestHelper helper) {
        final FakePlayer player = testPlayer(helper, new Vec3(2.5, 2, 1.5));
        final var packed = new ItemStack(OBItems.LUGGAGE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, packed);
        player.setItemInHand(InteractionHand.OFF_HAND, packed.copy());
        helper.assertTrue(use(helper, player, InteractionHand.OFF_HAND) == InteractionResult.PASS,
                "Offhand use deployed Luggage");

        helper.setBlock(CENTER.south(), Blocks.STONE);
        helper.assertTrue(use(helper, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && player.getMainHandItem() == packed,
                "Obstructed deployment spent the held item");
        helper.setBlock(CENTER.south(), Blocks.AIR);

        final Consumer<EntityJoinLevelEvent> reject = event -> {
            if (event.getEntity() instanceof OBLuggage luggage && player.getUUID().equals(luggage.getOwnerUUID())) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, reject);
        try {
            helper.assertTrue(use(helper, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && player.getMainHandItem() == packed,
                    "A rejected entity spawn spent the held item");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(reject);
        }

        helper.assertTrue(use(helper, player, InteractionHand.MAIN_HAND).consumesAction() && player.getMainHandItem().isEmpty(),
                "Survival deployment did not replace the held item with an entity");
        final var deployed = helper.getEntities(OBEntities.LUGGAGE.get(), CENTER, 2);
        helper.assertTrue(deployed.size() == 1 && player.getUUID().equals(deployed.get(0).getOwnerUUID()),
                "Deployment failed to assign the owner or created extra entities");
        deployed.get(0).discard();

        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(OBItems.LUGGAGE.get(), 2));
        helper.assertTrue(use(helper, player, InteractionHand.MAIN_HAND).consumesAction() && player.getMainHandItem().getCount() == 1,
                "Creative use of an older two-item stack did not spend exactly one Luggage");
        helper.getEntities(OBEntities.LUGGAGE.get(), CENTER, 2).forEach(OBLuggage::discard);
        helper.assertTrue(use(helper, player, InteractionHand.MAIN_HAND).consumesAction() && player.getMainHandItem().isEmpty(),
                "Creative use duplicated a portable Luggage by keeping the last item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void luggageMenusAndPickupShareOneInventory(final GameTestHelper helper) {
        final var luggage = helper.spawn(OBEntities.LUGGAGE.get(), CENTER);
        final var player = testPlayer(helper, new Vec3(3.5, 2, 4.5));
        final var secondViewer = testPlayer(helper, new Vec3(3.5, 2, 5.5));
        final ChestMenu first = luggage.createMenu(1, player.getInventory(), player);
        final ChestMenu second = luggage.createMenu(2, secondViewer.getInventory(), secondViewer);
        helper.assertTrue(first.getType() == OBMenuTypes.LUGGAGE.get() && first.getRowCount() == 3 && first.stillValid(player),
                "Normal Luggage did not have its three-row menu");

        player.getInventory().setItem(9, new ItemStack(Items.EMERALD, 5));
        first.quickMoveStack(player, 27);
        helper.assertTrue(player.getInventory().getItem(9).isEmpty() && second.getSlot(0).getItem().getCount() == 5,
                "A shift-clicked stack was not visible to both viewers");
        second.quickMoveStack(secondViewer, 0);
        helper.assertTrue(first.getSlot(0).getItem().isEmpty() && secondViewer.getInventory().getItem(8).getCount() == 5,
                "Shift-click withdrawal did not move the same server stack");

        final var tag = new ItemStack(Items.NAME_TAG).setHoverName(Component.literal("Walking Vault"));
        player.setItemInHand(InteractionHand.MAIN_HAND, tag);
        helper.assertTrue(luggage.interact(player, InteractionHand.MAIN_HAND).consumesAction() && tag.isEmpty()
                && luggage.getName().getString().equals("Walking Vault"), "The named name tag did not rename Luggage");
        first.getContainer().setItem(26, new ItemStack(Items.DIAMOND, 4));
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        player.setShiftKeyDown(true);
        luggage.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(luggage.isAlive() && first.getSlot(26).getItem().getCount() == 4, "Packing with a full inventory lost the entity or its contents");

        player.getInventory().setItem(8, ItemStack.EMPTY);
        luggage.interact(player, InteractionHand.MAIN_HAND);
        final ItemStack packed = player.getInventory().getItem(8);
        helper.assertTrue(!luggage.isAlive() && !first.stillValid(player) && !second.stillValid(secondViewer)
                && packed.is(OBItems.LUGGAGE.get()) && packed.getHoverName().getString().equals("Walking Vault"),
                "Packing did not transfer the named entity to a free slot and invalidate both menus");
        final var reloaded = OBEntities.LUGGAGE.get().create(helper.getLevel());
        reloaded.unpack(packed);
        final var reopened = reloaded.createMenu(3, player.getInventory(), player);
        helper.assertTrue(reopened.getSlot(26).getItem().is(Items.DIAMOND) && reopened.getSlot(26).getItem().getCount() == 4,
                "Redeploying the packed item lost the last slot or changed its count");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void luggageWalksToItemsAndLeavesOverflow(final GameTestHelper helper) {
        for (int x = 0; x < 7; x++) {
            for (int z = 1; z < 6; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        final var luggage = helper.spawn(OBEntities.LUGGAGE.get(), new BlockPos(1, 2, 3));
        final var player = helper.makeMockSurvivalPlayer();
        final var storage = luggage.createMenu(1, player.getInventory(), player).getContainer();
        storage.setItem(0, new ItemStack(Items.DIAMOND, 60));
        for (int slot = 1; slot < 27; slot++) storage.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        final Vec3 destination = helper.absoluteVec(new Vec3(5.5, 2.15, 3.5));
        final var dropped = new ItemEntity(helper.getLevel(), destination.x, destination.y, destination.z, new ItemStack(Items.DIAMOND, 7), 0, 0, 0);
        dropped.setDefaultPickUpDelay();
        helper.getLevel().addFreshEntity(dropped);
        helper.succeedWhen(() -> {
            helper.assertTrue(storage.getItem(0).getCount() == 64 && dropped.isAlive() && dropped.getItem().getCount() == 3,
                    "Luggage failed to reach the grounded stack, or collected beyond its normal capacity");
            helper.assertTrue(luggage.distanceToSqr(dropped) < 1, "Luggage collected the drop without walking to it");
        });
    }

    @GameTest(template = "empty")
    public static void chargedLuggageKeepsAllSlotsAndProtectsItsDrop(final GameTestHelper helper) {
        final var luggage = helper.spawn(OBEntities.LUGGAGE.get(), CENTER);
        final var player = testPlayer(helper, new Vec3(3.5, 2, 4.5));
        final ItemStack named = new ItemStack(OBItems.LUGGAGE.get()).setHoverName(Component.literal("Storm Case"));
        named.getOrCreateTag().putString("Marker", "original item tag");
        luggage.unpack(named);
        luggage.setOwnerUUID(player.getUUID());
        final ChestMenu oldMenu = luggage.createMenu(1, player.getInventory(), player);
        oldMenu.getContainer().setItem(0, new ItemStack(Items.APPLE, 3));
        luggage.thunderHit(helper.getLevel(), EntityType.LIGHTNING_BOLT.create(helper.getLevel()));
        final ChestMenu charged = luggage.createMenu(2, player.getInventory(), player);
        charged.getContainer().setItem(53, new ItemStack(Items.AMETHYST_SHARD, 8));
        oldMenu.getSlot(0).set(new ItemStack(Items.APPLE, 5));
        helper.assertTrue(luggage.isPowered() && charged.getRowCount() == 6 && oldMenu.getRowCount() == 3
                && charged.getSlot(0).getItem().getCount() == 5 && !luggage.hurt(helper.getLevel().damageSources().lava(), 50),
                "Lightning did not unlock the second three rows safely or changed its invulnerability");

        final var saved = luggage.saveWithoutId(new CompoundTag());
        luggage.discard();
        final var reloaded = OBEntities.LUGGAGE.get().create(helper.getLevel());
        reloaded.load(saved);
        helper.getLevel().addFreshEntity(reloaded);
        final var reopened = reloaded.createMenu(3, player.getInventory(), player);
        helper.assertTrue(reloaded.isPowered() && player.getUUID().equals(reloaded.getOwnerUUID()) && reopened.getRowCount() == 6
                && reopened.getSlot(53).getItem().getCount() == 8 && reloaded.getName().getString().equals("Storm Case"),
                "World serialization lost the charge, sixth row, owner, or name");
        helper.assertTrue(reloaded.getPickResult().hasFoil() && !reloaded.getPickResult().getTag().contains("Items"),
                "Creative pick block copied stored items or lost the charged appearance");

        player.setShiftKeyDown(true);
        reloaded.interact(player, InteractionHand.MAIN_HAND);
        final ItemStack packed = player.getMainHandItem();
        helper.assertTrue(packed.hasFoil() && packed.getTag().getString("Marker").equals("original item tag"),
                "Packing lost the charged appearance or unrelated item metadata");
        final Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2, 3.5));
        final var drop = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, packed.copy());
        helper.getLevel().addFreshEntity(drop);
        helper.assertTrue(drop.isInvulnerable() && drop.getAge() == -32768 && !drop.hurt(helper.getLevel().damageSources().lava(), 100),
                "The packed drop could burn or expire");
        final var restored = OBEntities.LUGGAGE.get().create(helper.getLevel());
        restored.unpack(drop.getItem());
        helper.assertTrue(restored.isPowered() && restored.createMenu(4, player.getInventory(), player).getSlot(53).getItem().getCount() == 8,
                "The packed drop lost the lightning upgrade or its last stored stack");
        helper.succeed();
    }

    private static FakePlayer testPlayer(final GameTestHelper helper, final Vec3 pos) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "LuggageGameTest"));
        final Vec3 worldPos = helper.absoluteVec(pos);
        player.moveTo(worldPos.x, worldPos.y, worldPos.z, 0, 0);
        return player;
    }

    private static InteractionResult use(final GameTestHelper helper, final FakePlayer player, final InteractionHand hand) {
        return player.gameMode.useItem(player, helper.getLevel(), player.getItemInHand(hand), hand);
    }
}
