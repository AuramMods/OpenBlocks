package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBMagnet;
import art.arcane.openblocks.entity.OBMountedBlock;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class CraneGameTests {
    private static final BlockPos SOURCE = new BlockPos(2, 2, 2);
    private static final BlockPos DESTINATION = new BlockPos(4, 2, 2);

    @GameTest(template = "empty")
    public static void craneEquipmentCraftsWithItsLegacyComponents(final GameTestHelper helper) {
        final var backpack = ComponentRecipeGameTests.input(helper, OBItems.CRANE_ENGINE.get(), OBItems.CRANE_MAGNET.get(),
                OBItems.BEAM.get(), OBItems.BEAM.get(), OBItems.LINE.get(), OBItems.LINE.get(), OBItems.LINE.get(), Items.LEATHER);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, backpack).is(OBItems.CRANE_BACKPACK.get()),
                "The legacy components could not craft the backpack");
        final var control = ComponentRecipeGameTests.input(helper, Items.IRON_INGOT, Items.GLOWSTONE_DUST, Items.IRON_INGOT,
                Items.GOLD_NUGGET, Items.REDSTONE, Items.GOLD_NUGGET, Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, control).is(OBItems.CRANE_CONTROL.get()),
                "The legacy ingots, nuggets, and dust could not craft the controller");
        final var miracleMagnet = ComponentRecipeGameTests.input(helper, Items.REDSTONE, Items.ENDER_PEARL, Items.REDSTONE,
                Items.ENDER_PEARL, OBItems.CRANE_MAGNET.get(), Items.ENDER_PEARL, Items.REDSTONE, Items.ENDER_PEARL, Items.REDSTONE);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, miracleMagnet).is(OBItems.MIRACLE_MAGNET.get()),
                "Redstone, ender pearls, and the Crane Magnet could not craft the Miracle Magnet");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craneEquipmentControlsWinchAndRespondsToSwing(final GameTestHelper helper) {
        final FakePlayer player = player(helper);
        helper.assertTrue(OBItems.CRANE_BACKPACK.get() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.CHEST
                && OBItems.CRANE_CONTROL.get().getMaxStackSize(new ItemStack(OBItems.CRANE_CONTROL.get())) == 1,
                "The backpack should occupy the chest slot and the controller should be unstackable");
        helper.assertTrue(OBMagnet.ensureFor(player) == null, "The crane spawned without its chest equipment");
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(OBItems.CRANE_BACKPACK.get()));
        final OBMagnet magnet = OBMagnet.ensureFor(player);
        helper.assertTrue(magnet != null && OBMagnet.ensureFor(player) == magnet, "The backpack did not deploy exactly one magnet");
        final ItemStack controller = new ItemStack(OBItems.CRANE_CONTROL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, controller);
        player.setShiftKeyDown(true);
        final var lowering = controller.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult();
        helper.assertTrue(lowering.consumesAction() && !lowering.shouldSwing(), "Winch use must not send the attack swing that toggles the magnet");
        for (int tick = 0; tick < 130; tick++) magnet.tick();
        helper.assertTrue(magnet.cableLength() == 10 && magnet.liftState() == 2, "Sneak-use did not lower the cable to its limit");
        player.stopUsingItem();
        player.setShiftKeyDown(false);
        controller.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 130; tick++) magnet.tick();
        helper.assertTrue(magnet.cableLength() == 0.25 && magnet.liftState() == 1, "Plain use did not winch in to the minimum");
        player.stopUsingItem();

        helper.setBlock(SOURCE, Blocks.GOLD_BLOCK);
        setMagnetAt(magnet, helper.absolutePos(SOURCE).above());
        player.swing(InteractionHand.MAIN_HAND);
        helper.assertTrue(magnet.getFirstPassenger() instanceof OBMountedBlock, "The controller's server swing did not grab a block");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        magnet.tick();
        helper.assertTrue(magnet.isRemoved() && OBMagnet.forPlayer(player) == null, "Removing the backpack left an active magnet");
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, SOURCE);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craneTransfersChestWithoutDroppingItsInventory(final GameTestHelper helper) {
        final FakePlayer player = player(helper);
        final OBMagnet magnet = equipped(player);
        fillChest(helper);
        helper.setBlock(SOURCE.below(), Blocks.STONE);
        final BlockPos source = helper.absolutePos(SOURCE);
        magnet.setPos(source.getX() + 0.5, source.getY() + 0.875, source.getZ() + 0.5);
        helper.assertTrue(magnet.toggle(player) && magnet.getFirstPassenger() instanceof OBMountedBlock, "The crane failed to pick up a single chest");
        helper.assertTrue(magnet.getY() - magnet.getFirstPassenger().getBbHeight() >= source.getY(), "The mounted chest intersected the floor at pickup");
        helper.assertBlockPresent(Blocks.AIR, SOURCE);
        helper.assertItemEntityCountIs(Items.DIAMOND, SOURCE, 2, 0);

        final Vec3 destination = Vec3.atBottomCenterOf(helper.absolutePos(DESTINATION));
        final var cargo = magnet.getFirstPassenger();
        magnet.setPos(destination.x, Math.nextDown(destination.y + cargo.getBbHeight()), destination.z);
        magnet.positionRider(cargo);
        helper.setBlock(DESTINATION.below(), Blocks.STONE);
        helper.setBlock(DESTINATION, Blocks.STONE);
        helper.assertTrue(!magnet.toggle(player) && magnet.isVehicle(), "An obstructed drop created or lost cargo");
        helper.setBlock(DESTINATION, Blocks.AIR);
        helper.assertTrue(magnet.toggle(player) && !magnet.isVehicle(), "The chest could not be delivered to an empty space");
        checkChest(helper, DESTINATION);
        helper.assertItemEntityCountIs(Items.DIAMOND, DESTINATION, 2, 0);
        magnet.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craneHonorsPickupSpawnAndPlacementCancellation(final GameTestHelper helper) {
        final FakePlayer player = player(helper);
        final OBMagnet magnet = equipped(player);
        fillChest(helper);
        final BlockPos source = helper.absolutePos(SOURCE);
        final BlockPos destination = helper.absolutePos(DESTINATION);
        setMagnetAt(magnet, source.above());
        final Consumer<BlockEvent.BreakEvent> denyPickup = event -> {
            if (event.getPlayer() == player && event.getPos().equals(source)) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, denyPickup);
        try {
            helper.assertTrue(!magnet.toggle(player) && !magnet.isVehicle(), "Forge rejected pickup, but the chest was mounted");
            checkChest(helper, SOURCE);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(denyPickup);
        }

        final Consumer<EntityJoinLevelEvent> denySpawn = event -> {
            if (event.getEntity() instanceof OBMountedBlock && event.getEntity().blockPosition().equals(source)) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, denySpawn);
        try {
            helper.assertTrue(!magnet.toggle(player) && !magnet.isVehicle(), "Rejected cargo spawn removed the chest");
            checkChest(helper, SOURCE);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(denySpawn);
        }

        helper.assertTrue(magnet.toggle(player), "The permission events blocked a later permitted pickup");
        setMagnetAt(magnet, destination.above());
        final Consumer<BlockEvent.EntityPlaceEvent> denyPlace = event -> {
            if (event.getEntity() == player && event.getPos().equals(destination)) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityPlaceEvent.class, denyPlace);
        try {
            helper.assertTrue(!magnet.toggle(player) && magnet.isVehicle(), "Canceled crane placement discarded cargo");
            helper.assertBlockPresent(Blocks.AIR, DESTINATION);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(denyPlace);
        }
        helper.assertTrue(magnet.toggle(player), "The retained cargo could not be placed after canceling once");
        checkChest(helper, DESTINATION);
        magnet.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cranePrefersEntitiesAndSavesMountedBlockPayload(final GameTestHelper helper) {
        final FakePlayer player = player(helper);
        final OBMagnet magnet = equipped(player);
        fillChest(helper);
        setMagnetAt(magnet, helper.absolutePos(SOURCE).above());
        final var pig = helper.spawn(EntityType.PIG, SOURCE);
        helper.assertTrue(magnet.toggle(player) && pig.getVehicle() == magnet, "An eligible entity was not picked before the block below it");
        checkChest(helper, SOURCE);
        helper.assertTrue(magnet.toggle(player) && !pig.isPassenger(), "Toggling again did not release the entity");
        pig.discard();
        helper.assertTrue(magnet.toggle(player), "The magnet could not pick up the chest after releasing its passenger");

        final CompoundTag data = new CompoundTag();
        helper.assertTrue(magnet.save(data), "The magnet did not serialize its mounted cargo");
        magnet.getFirstPassenger().discard();
        magnet.discard();
        final var saved = EntityType.loadEntityRecursive(data, helper.getLevel(), entity -> entity);
        helper.assertTrue(saved instanceof OBMagnet && saved.getFirstPassenger() instanceof OBMountedBlock, "Reloading lost the mounted cargo entity");
        final OBMagnet restored = (OBMagnet) saved;
        setMagnetAt(restored, helper.absolutePos(DESTINATION).above());
        helper.assertTrue(restored.toggle(player), "Restored cargo could not be released");
        checkChest(helper, DESTINATION);
        restored.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void remoteMagnetFollowsItsControllerAndHonorsSpawnCancellation(final GameTestHelper helper) {
        final FakePlayer actor = player(helper);
        final Vec3 origin = helper.absoluteVec(new Vec3(2.5, 4, 2.5));
        final var target = new AtomicReference<>(origin);
        final Consumer<EntityJoinLevelEvent> cancel = event -> {
            if (event.getEntity() instanceof OBMagnet remote && remote.position().equals(origin)) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, cancel);
        try {
            helper.assertTrue(OBMagnet.deployRemote(helper.getLevel(), actor.getGameProfile(), origin, target::get) == null,
                    "A denied remote magnet spawn was reported as a successful activation");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
        }
        final OBMagnet magnet = OBMagnet.deployRemote(helper.getLevel(), actor.getGameProfile(), origin, target::get);
        helper.assertTrue(magnet != null && magnet.getType() == OBEntities.MAGNET.get() && magnet.holder() == null,
                "Remote activation unexpectedly required a player backpack");
        target.set(origin.add(2, 1, 0));
        for (int tick = 0; tick < 20; tick++) magnet.tick();
        helper.assertTrue(magnet.position().distanceTo(target.get()) < 0.002, "The remote magnet missed its new target");
        target.set(null);
        magnet.tick();
        helper.assertTrue(magnet.isRemoved(), "The remote magnet ignored its disconnected controller");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inactiveRemoteMagnetRecoversSavedCargoUsingItsOwner(final GameTestHelper helper) {
        final FakePlayer actor = player(helper);
        fillChest(helper);
        final Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(SOURCE).above());
        final OBMagnet magnet = OBMagnet.deployRemote(helper.getLevel(), actor.getGameProfile(), origin, () -> origin);
        helper.assertTrue(magnet != null && magnet.toggle(actor), "The remote magnet did not mount the chest");
        setMagnetAt(magnet, helper.absolutePos(DESTINATION).above());
        final CompoundTag data = new CompoundTag();
        helper.assertTrue(magnet.save(data) && data.contains("MachineOwner"), "The remote magnet lost the owner's protection identity");
        magnet.getFirstPassenger().discard();
        magnet.discard();
        final var loaded = EntityType.loadEntityRecursive(data, helper.getLevel(), entity -> entity);
        helper.assertTrue(loaded instanceof OBMagnet && loaded.getFirstPassenger() instanceof OBMountedBlock,
                "The restored remote magnet lost the chest it carried");
        final BlockPos destination = helper.absolutePos(DESTINATION);
        final AtomicReference<UUID> placedBy = new AtomicReference<>();
        final Consumer<BlockEvent.EntityPlaceEvent> observe = event -> {
            if (event.getPos().equals(destination) && event.getEntity() instanceof FakePlayer player) placedBy.set(player.getUUID());
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityPlaceEvent.class, observe);
        try {
            loaded.tick();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(observe);
        }
        helper.assertTrue(loaded.isRemoved() && actor.getUUID().equals(placedBy.get()), "The unloaded controller's cargo bypassed its owner identity");
        helper.assertBlockPresent(Blocks.AIR, SOURCE);
        checkChest(helper, DESTINATION);
        helper.succeed();
    }

    private static void fillChest(final GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        final var chest = (ChestBlockEntity) helper.getBlockEntity(SOURCE);
        chest.setCustomName(Component.literal("Crane cargo"));
        chest.setItem(3, new ItemStack(Items.DIAMOND, 9));
    }

    private static void checkChest(final GameTestHelper helper, final BlockPos pos) {
        final var chest = helper.getBlockEntity(pos);
        helper.assertTrue(chest instanceof ChestBlockEntity loaded && loaded.getItem(3).is(Items.DIAMOND) && loaded.getItem(3).getCount() == 9
                && loaded.getName().getString().equals("Crane cargo") && loaded.getBlockState().getValue(ChestBlock.FACING) == Direction.EAST,
                "The chest lost its name, inventory, or facing");
    }

    private static void setMagnetAt(final OBMagnet magnet, final BlockPos pos) {
        final Vec3 center = Vec3.atBottomCenterOf(pos);
        magnet.setPos(center.x, center.y, center.z);
        if (magnet.getFirstPassenger() != null) magnet.positionRider(magnet.getFirstPassenger());
    }

    private static OBMagnet equipped(final FakePlayer player) {
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(OBItems.CRANE_BACKPACK.get()));
        return OBMagnet.ensureFor(player);
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CraneGameTest"));
        final Vec3 pos = helper.absoluteVec(new Vec3(3.5, 4, 3.5));
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }
}
