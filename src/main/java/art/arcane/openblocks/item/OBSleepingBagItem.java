package art.arcane.openblocks.item;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.client.OBSleepingBagRendering;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Player.BedSleepingProblem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID)
public final class OBSleepingBagItem extends ArmorItem {

    private static final String SLEEPING = "Sleeping";
    private static final String RETURN_SLOT = "Slot";

    public OBSleepingBagItem() {
        super(ArmorMaterials.IRON, Type.CHESTPLATE, new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player user, final InteractionHand hand) {
        final ItemStack held = user.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || user.isSleeping()) return InteractionResultHolder.pass(held);
        if (!(user instanceof ServerPlayer player)) return InteractionResultHolder.success(held);
        final BlockPos pos = player.blockPosition();
        if (!maySleep(player, pos)) return InteractionResultHolder.fail(held);

        final Inventory inventory = player.getInventory();
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        final int selected = inventory.selected;
        final int returnSlot;
        final ItemStack bag = held.copyWithCount(1);
        if (held.getCount() == 1) {
            returnSlot = selected;
            inventory.setItem(selected, chest);
        } else {
            // Older versions of this port allowed stacks of sleeping bags.
            returnSlot = inventory.getFreeSlot();
            if (returnSlot < 0 && !chest.isEmpty()) {
                player.displayClientMessage(Component.translatable("item.open_blocks.sleeping_bag.no_room"), true);
                return InteractionResultHolder.fail(held);
            }
            if (returnSlot >= 0) inventory.setItem(returnSlot, chest);
            final ItemStack remaining = held.copyWithCount(held.getCount() - 1);
            clearSleepTags(remaining);
            inventory.setItem(selected, remaining);
        }
        bag.getOrCreateTag().putInt(RETURN_SLOT, returnSlot);
        player.setItemSlot(EquipmentSlot.CHEST, bag);
        startSleep(player, pos, bag);
        player.inventoryMenu.broadcastChanges();

        // The swap is already applied. Returning the original stack through vanilla's unchanged-stack path
        // prevents creative use from restoring its count or damage onto the displaced armor.
        return InteractionResultHolder.consume(held);
    }

    @Override
    public void inventoryTick(final ItemStack bag, final Level level, final Entity wearer, final int slot, final boolean selected) {
        if (!(wearer instanceof ServerPlayer player) || !player.isAlive() || player.isSleeping()
                || player.getItemBySlot(EquipmentSlot.CHEST) != bag) return;
        final BlockPos pos = player.blockPosition();
        if (hasSlept(bag)) {
            returnBag(player, bag);
        } else if (maySleep(player, pos)) {
            startSleep(player, pos, bag);
        } else {
            // Remember the failed attempt if the inventory and drop destination are both unavailable.
            bag.getOrCreateTag().putBoolean(SLEEPING, true);
            returnBag(player, bag);
        }
    }

    private static boolean maySleep(final ServerPlayer player, final BlockPos pos) {
        if (!player.isAlive() || player.isSpectator()) return false;
        final Level level = player.level();
        final Optional<BlockPos> location = Optional.of(pos);
        final Vec3 center = Vec3.atBottomCenterOf(pos);
        BedSleepingProblem problem = ForgeEventFactory.onPlayerSleepInBed(player, location);
        if (problem == null) {
            if (!level.dimensionType().natural()) {
                problem = BedSleepingProblem.NOT_POSSIBLE_HERE;
            } else if (!hasSleepingGround(player, pos)) {
                player.displayClientMessage(Component.translatable("item.open_blocks.sleeping_bag.no_ground"), true);
                return false;
            } else if (!ForgeEventFactory.fireSleepingTimeCheck(player, location)) {
                problem = BedSleepingProblem.NOT_POSSIBLE_NOW;
            } else if (!player.isCreative() && !level.getEntitiesOfClass(Monster.class,
                    new AABB(center, center).inflate(8, 5, 8),
                    monster -> monster.isPreventingPlayerRest(player)).isEmpty()) {
                problem = BedSleepingProblem.NOT_SAFE;
            }
        }
        if (problem == null) return true;
        if (problem.getMessage() != null) player.displayClientMessage(problem.getMessage(), true);
        return false;
    }

    private static boolean hasSleepingGround(final Player player, final BlockPos pos) {
        final Level level = player.level();
        final BlockPos below = pos.below();
        final CollisionContext context = CollisionContext.of(player);
        if (!level.getFluidState(pos).isEmpty() || !level.getBlockState(pos).getCollisionShape(level, pos, context).isEmpty()) return false;
        final var support = level.getBlockState(below).getCollisionShape(level, below, context);
        if (support.isEmpty()) return false;
        final AABB bounds = support.bounds();
        return bounds.getXsize() >= 0.5 && bounds.getYsize() >= 0.5 && bounds.getZsize() >= 0.5;
    }

    private static void startSleep(final ServerPlayer player, final BlockPos pos, final ItemStack bag) {
        bag.getOrCreateTag().putBoolean(SLEEPING, true);
        player.sleepCounter = 0;
        player.startSleeping(pos);
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5);
        player.awardStat(Stats.SLEEP_IN_BED);
        CriteriaTriggers.SLEPT_IN_BED.trigger(player);
        if (!player.serverLevel().canSleepThroughNights()) {
            player.displayClientMessage(Component.translatable("sleep.not_possible"), true);
        }
        player.serverLevel().updateSleepingPlayerList();
    }

    private static void returnBag(final ServerPlayer player, final ItemStack equipped) {
        final Inventory inventory = player.getInventory();
        final var tag = equipped.getTag();
        final int preferred = tag != null && tag.contains(RETURN_SLOT, Tag.TAG_ANY_NUMERIC) ? tag.getInt(RETURN_SLOT) : -1;
        final ItemStack armor = preferred >= 0 && preferred < inventory.items.size() ? inventory.getItem(preferred) : ItemStack.EMPTY;
        final boolean restoreArmor = !armor.isEmpty() && !(armor.getItem() instanceof OBSleepingBagItem)
                && armor.canEquip(EquipmentSlot.CHEST, player);
        final int destination = preferred >= 0 && preferred < inventory.items.size() && (armor.isEmpty() || restoreArmor)
                ? preferred : inventory.getFreeSlot();
        final ItemStack bag = equipped.copy();
        clearSleepTags(bag);
        if (destination >= 0) {
            inventory.setItem(destination, bag);
        } else {
            final ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), bag);
            drop.setTarget(player.getUUID());
            drop.setDefaultPickUpDelay();
            if (!player.level().addFreshEntity(drop)) return;
        }
        player.setItemSlot(EquipmentSlot.CHEST, restoreArmor ? armor : ItemStack.EMPTY);
        player.inventoryMenu.broadcastChanges();
    }

    private static void clearSleepTags(final ItemStack bag) {
        bag.removeTagKey(SLEEPING);
        bag.removeTagKey(RETURN_SLOT);
    }

    public static boolean hasSlept(final ItemStack stack) {
        return stack.getItem() instanceof OBSleepingBagItem && stack.hasTag() && stack.getTag().getBoolean(SLEEPING);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void checkSleepingLocation(final SleepingLocationCheckEvent event) {
        if (event.getResult() == Event.Result.DEFAULT && event.getEntity() instanceof Player player
                && hasSlept(player.getItemBySlot(EquipmentSlot.CHEST))) {
            event.setResult(hasSleepingGround(player, event.getSleepingLocation()) ? Event.Result.ALLOW : Event.Result.DENY);
        }
    }

    @Override
    public String getArmorTexture(final ItemStack stack, final Entity entity, final EquipmentSlot slot, final String type) {
        return OpenBlocks.id("textures/model/sleeping_bag.png").toString();
    }

    @Override
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(OBSleepingBagRendering.extension());
    }
}
