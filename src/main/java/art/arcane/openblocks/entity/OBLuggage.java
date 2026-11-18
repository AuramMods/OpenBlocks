package art.arcane.openblocks.entity;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBEntities;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBMenuTypes;
import art.arcane.openblocks.registry.OBSounds;
import java.util.Comparator;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkHooks;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OBLuggage extends TamableAnimal implements MenuProvider, PowerableMob {

    private static final int NORMAL_SIZE = 27;
    private static final int CHARGED_SIZE = 54;
    private static final String SIZE_KEY = "size";
    private static final String ITEMS_KEY = "Items";
    private static final String CHARGED_KEY = "shiny";
    private static final String ITEM_TAG_KEY = "ItemTag";
    private static final EntityDataAccessor<Boolean> CHARGED = SynchedEntityData.defineId(OBLuggage.class, EntityDataSerializers.BOOLEAN);

    // Keep the first three rows live for viewers when a lightning strike unlocks the other three.
    private final SimpleContainer inventory = new SimpleContainer(CHARGED_SIZE) {
        @Override
        public int getContainerSize() {
            return isPowered() ? CHARGED_SIZE : NORMAL_SIZE;
        }

        @Override
        public boolean stillValid(final Player player) {
            return isAlive() && player.level() == level() && player.distanceToSqr(OBLuggage.this) < 64;
        }
    };
    private final InvWrapper itemHandler = new InvWrapper(inventory);
    private CompoundTag packedItemData = new CompoundTag();
    private int lastEatSound;

    public OBLuggage(final EntityType<? extends OBLuggage> type, final Level level) {
        super(type, level);
        setTame(true);
        setPersistenceRequired();
        setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
    }

    @SubscribeEvent
    public static void registerAttributes(final EntityAttributeCreationEvent event) {
        event.put(OBEntities.LUGGAGE.get(), createMobAttributes().build());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CHARGED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new FollowOwnerGoal(this, 0.7, 10.0F, 2.0F, false));
        goalSelector.addGoal(3, new CollectItemsGoal());
    }

    @Override
    public boolean isPowered() {
        return entityData.get(CHARGED);
    }

    public static boolean isChargedItem(final ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        return tag != null && tag.getInt(SIZE_KEY) > NORMAL_SIZE;
    }

    @Override
    public void thunderHit(final ServerLevel level, final LightningBolt bolt) {
        entityData.set(CHARGED, true);
    }

    @Override
    public boolean isInvulnerableTo(final DamageSource source) {
        return true;
    }

    @Override
    public boolean canBeLeashed(final Player player) {
        return false;
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob other) {
        return null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.825;
    }

    @Override
    protected void playStepSound(final BlockPos pos, final BlockState state) {
        playSound(OBSounds.LUGGAGE_WALK.get(), 0.3F, 0.7F + random.nextFloat() * 0.5F);
    }

    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.getItemInHand(hand).is(Items.NAME_TAG)) return InteractionResult.PASS;
        if (level() instanceof ServerLevel server) {
            if (player.isShiftKeyDown()) {
                final int slot = player.getInventory().getFreeSlot();
                if (slot < 0) return InteractionResult.CONSUME;
                player.getInventory().setItem(slot, pack());
                player.getInventory().setChanged();
                discard();
                server.sendParticles(ParticleTypes.PORTAL, getX(), getY() + getBbHeight(), getZ(), 50, 0.25, 0, 0.25, 0.3);
                playSound(SoundEvents.ITEM_PICKUP, 0.5F, 0.9F + random.nextFloat() * 0.1F);
            } else if (player instanceof ServerPlayer serverPlayer) {
                playSound(SoundEvents.CHEST_OPEN, 0.5F, 0.9F + random.nextFloat() * 0.1F);
                NetworkHooks.openScreen(serverPlayer, this, data -> data.writeBoolean(isPowered()));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public ChestMenu createMenu(final int id, final Inventory playerItems, final Player player) {
        return new ChestMenu(OBMenuTypes.LUGGAGE.get(), id, playerItems, inventory, inventory.getContainerSize() / 9);
    }

    public static ChestMenu createClientMenu(final int id, final Inventory playerItems, final FriendlyByteBuf data) {
        final int size = data.readBoolean() ? CHARGED_SIZE : NORMAL_SIZE;
        return new ChestMenu(OBMenuTypes.LUGGAGE.get(), id, playerItems, new SimpleContainer(size), size / 9);
    }

    @Override
    public ItemStack getPickResult() {
        final ItemStack picked = new ItemStack(OBItems.LUGGAGE.get());
        if (isPowered()) picked.getOrCreateTag().putInt(SIZE_KEY, CHARGED_SIZE);
        if (hasCustomName()) picked.setHoverName(getCustomName());
        return picked;
    }

    private ItemStack pack() {
        final ItemStack stack = new ItemStack(OBItems.LUGGAGE.get());
        final CompoundTag tag = packedItemData.copy();
        writeInventory(tag);
        stack.setTag(tag);
        if (hasCustomName()) stack.setHoverName(getCustomName());
        else stack.resetHoverName();
        return stack;
    }

    public void unpack(final ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        if (tag != null) {
            readInventory(tag);
            packedItemData = tag.copy();
            packedItemData.remove(ITEMS_KEY);
            packedItemData.remove(SIZE_KEY);
        }
        if (stack.hasCustomHoverName()) setCustomName(stack.getHoverName());
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(CHARGED_KEY, isPowered());
        writeInventory(tag);
        if (!packedItemData.isEmpty()) tag.put(ITEM_TAG_KEY, packedItemData.copy());
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readInventory(tag);
        packedItemData = tag.getCompound(ITEM_TAG_KEY).copy();
    }

    private void writeInventory(final CompoundTag tag) {
        final int size = inventory.getContainerSize();
        tag.putInt(SIZE_KEY, size);
        final ListTag items = new ListTag();
        for (int slot = 0; slot < size; slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            final CompoundTag entry = stack.save(new CompoundTag());
            entry.putByte("Slot", (byte) slot);
            items.add(entry);
        }
        tag.put(ITEMS_KEY, items);
    }

    private void readInventory(final CompoundTag tag) {
        entityData.set(CHARGED, tag.getBoolean(CHARGED_KEY) || tag.getInt(SIZE_KEY) > NORMAL_SIZE);
        inventory.clearContent();
        final ListTag items = tag.getList(ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            final CompoundTag entry = items.getCompound(index);
            final int slot = entry.getByte("Slot");
            if (slot >= 0 && slot < inventory.getContainerSize()) inventory.setItem(slot, ItemStack.of(entry));
        }
    }

    private boolean canCollect(final ItemEntity item) {
        if (!item.isAlive() || !item.onGround() || item.isInWater() || item.getItem().isEmpty()) return false;
        final ItemStack stack = item.getItem();
        return ItemHandlerHelper.insertItem(itemHandler, stack, true).getCount() < stack.getCount();
    }

    private final class CollectItemsGoal extends Goal {

        @Nullable private ItemEntity target;
        @Nullable private Path path;

        private CollectItemsGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!getNavigation().isDone()) return false;
            final var nearby = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(10), OBLuggage.this::canCollect);
            nearby.sort(Comparator.comparingDouble(OBLuggage.this::distanceToSqr));
            for (final ItemEntity item : nearby) {
                final boolean withinReach = distanceToSqr(item) < 1;
                final Path route = withinReach ? null : getNavigation().createPath(item, 0);
                if (!withinReach && (route == null || !route.canReach())) continue;
                target = item;
                path = route;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && canCollect(target) && (distanceToSqr(target) < 1 || !getNavigation().isDone());
        }

        @Override
        public void start() {
            if (path != null) getNavigation().moveTo(path, 0.4);
        }

        @Override
        public void stop() {
            getNavigation().stop();
            target = null;
            path = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null || !target.isAlive() || !target.onGround() || target.isInWater() || distanceToSqr(target) >= 1) return;
            final ItemStack offered = target.getItem();
            final ItemStack remaining = ItemHandlerHelper.insertItem(itemHandler, offered, false);
            if (remaining.getCount() == offered.getCount()) return;
            if (remaining.isEmpty()) target.discard();
            else target.setItem(remaining);
            if (tickCount - lastEatSound > 15) {
                playSound((offered.getUseAnimation() == UseAnim.EAT ? OBSounds.LUGGAGE_EAT_FOOD : OBSounds.LUGGAGE_EAT_ITEM).get(),
                        0.5F, 1.0F + random.nextFloat() * 0.2F);
                lastEatSound = tickCount;
            }
        }
    }
}
