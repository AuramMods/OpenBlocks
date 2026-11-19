package art.arcane.openblocks.command;

import art.arcane.openblocks.capability.OBCapabilities.LuckData;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBSounds;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import org.apache.commons.lang3.StringUtils;

public final class OBFlimFlamEffects {

    @FunctionalInterface
    private interface EffectAction {
        boolean execute(ServerPlayer target);
    }

    private record EffectMeta(MobEffect effect, int levelMin, int levelMax, int durationMin, int durationMax) {}
    private record Prank(String id, int cost, int weight, int maximumLuck, boolean safe, boolean silent, EffectAction action) {}

    private static final int INVISIBILITY_DURATION = 10 * 60 * 20;
    private static final int SNOWBALL_COUNT = 200;
    private static final int CREEPER_COUNT = 15;
    private static final int SEARCH_RADIUS_MOBS = 20;
    private static final int SEARCH_RADIUS_MOUNT = 40;
    private static final int PRANK_PLACEMENT_FLAGS = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static final List<String> SQUID_NAMES = List.of("Fancy Hat", "Hello there!", "Look at my hat!",
            "My hat is amazing!", "Ceci n'est pas une pipe", "???");

    private static final List<Supplier<SoundEvent>> SOUND_POOL = List.of(
            OBSounds.MOSQUITO::get, OBSounds.ALARM_CLOCK::get, OBSounds.VIBRATE::get, OBSounds.FEATURE_FART::get,
            () -> SoundEvents.TNT_PRIMED, () -> SoundEvents.GENERIC_EXPLODE, () -> SoundEvents.ELDER_GUARDIAN_CURSE,
            () -> SoundEvents.BLAZE_SHOOT, () -> SoundEvents.ENDERMAN_STARE, () -> SoundEvents.GHAST_SCREAM,
            () -> SoundEvents.GHAST_SHOOT, () -> SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, () -> SoundEvents.CREEPER_PRIMED);

    private static final List<EffectMeta> EFFECT_POOL = List.of(
            new EffectMeta(MobEffects.BLINDNESS, 1, 1, seconds(15), seconds(60)),
            new EffectMeta(MobEffects.CONFUSION, 1, 1, seconds(15), seconds(60)),
            new EffectMeta(MobEffects.DIG_SLOWDOWN, 50, 100, seconds(15), seconds(60)),
            new EffectMeta(MobEffects.JUMP, 30, 50, seconds(5), seconds(15)),
            new EffectMeta(MobEffects.MOVEMENT_SPEED, 50, 100, seconds(5), seconds(15)),
            new EffectMeta(MobEffects.MOVEMENT_SLOWDOWN, 4, 7, seconds(5), seconds(15)),
            new EffectMeta(MobEffects.LEVITATION, 1, 1, seconds(5), seconds(15)));

    private static final List<Function<ServerPlayer, ItemStack>> USELESS_TOOL_POOL = List.of(
            p -> new ItemStack(Items.DIAMOND_PICKAXE),
            p -> new ItemStack(Items.GOLDEN_PICKAXE),
            p -> new ItemStack(Items.IRON_PICKAXE),
            p -> new ItemStack(Items.STONE_PICKAXE),
            p -> new ItemStack(Items.WOODEN_PICKAXE),
            p -> new ItemStack(Items.DIAMOND_SHOVEL),
            p -> new ItemStack(Items.GOLDEN_SHOVEL),
            p -> new ItemStack(Items.IRON_SHOVEL),
            p -> new ItemStack(Items.STONE_SHOVEL),
            p -> new ItemStack(Items.WOODEN_SHOVEL),
            p -> new ItemStack(Items.DIAMOND_AXE),
            p -> new ItemStack(Items.GOLDEN_AXE),
            p -> new ItemStack(Items.IRON_AXE),
            p -> new ItemStack(Items.STONE_AXE),
            p -> new ItemStack(Items.WOODEN_AXE),
            p -> new ItemStack(Items.SHEARS),
            p -> new ItemStack(Items.DIAMOND_LEGGINGS),
            p -> new ItemStack(Items.GOLDEN_LEGGINGS),
            p -> new ItemStack(Items.IRON_LEGGINGS),
            p -> new ItemStack(Items.CHAINMAIL_LEGGINGS),
            p -> new ItemStack(Items.LEATHER_LEGGINGS),
            p -> new ItemStack(Items.DIAMOND_BOOTS),
            p -> new ItemStack(Items.GOLDEN_BOOTS),
            p -> new ItemStack(Items.IRON_BOOTS),
            p -> new ItemStack(Items.CHAINMAIL_BOOTS),
            p -> new ItemStack(Items.LEATHER_BOOTS),
            p -> new ItemStack(Items.DIAMOND_CHESTPLATE),
            p -> new ItemStack(Items.GOLDEN_CHESTPLATE),
            p -> new ItemStack(Items.IRON_CHESTPLATE),
            p -> new ItemStack(Items.CHAINMAIL_CHESTPLATE),
            p -> new ItemStack(Items.LEATHER_CHESTPLATE),
            p -> new ItemStack(Items.DIAMOND_HELMET),
            p -> new ItemStack(Items.GOLDEN_HELMET),
            p -> new ItemStack(Items.IRON_HELMET),
            p -> new ItemStack(Items.CHAINMAIL_HELMET),
            p -> new ItemStack(Items.LEATHER_HELMET));

    private static final Map<String, Prank> EFFECTS = new LinkedHashMap<>();

    static {
        register("inventory-shuffle", -50, 100, true, OBFlimFlamEffects::inventoryShuffle);
        register("useless-tool", -125, 50, true, OBFlimFlamEffects::uselessTool);
        register("bane", -125, 100, true, OBFlimFlamEffects::bane);
        register("epic-lore", -10, 100, true, OBFlimFlamEffects::epicLore);
        register("living-rename", -10, 100, true, OBFlimFlamEffects::livingRename);
        register("squid", -75, 50, true, OBFlimFlamEffects::squid);
        register("sheep-dye", -5, 50, true, OBFlimFlamEffects::sheepDye);
        register("invisible-mobs", -25, 10, true, OBFlimFlamEffects::invisibleMobs);
        register(new Prank("sound", -5, 150, -5, true, true, OBFlimFlamEffects::sound));
        register("snowballs", -50, 50, false, OBFlimFlamEffects::snowballs);
        register("teleport", -100, 30, false, OBFlimFlamEffects::teleport);
        register("mount", -150, 25, false, OBFlimFlamEffects::mount);
        register(new Prank("encase", -50, 50, -300, false, false, OBFlimFlamEffects::encase));
        register("creepers", -60, 50, false, OBFlimFlamEffects::creepers);
        register("disarm", -50, 50, false, OBFlimFlamEffects::disarm);
        register("effect", -75, 75, false, OBFlimFlamEffects::effect);
        register(new Prank("skyblock", -100, 150, -400, false, false, OBFlimFlamEffects::skyblock));
    }

    private OBFlimFlamEffects() {}

    public static List<String> effectNames() {
        return List.copyOf(EFFECTS.keySet());
    }

    public static boolean hasEffect(final String name) {
        return EFFECTS.containsKey(name);
    }

    public static String randomEffectName(final ServerPlayer target) {
        final List<String> names = effectNames();
        if (names.isEmpty()) return "";
        return names.get(target.getRandom().nextInt(names.size()));
    }

    public static boolean execute(final String name, final ServerPlayer target) {
        final Prank prank = EFFECTS.get(name);
        return prank != null && execute(prank, target);
    }

    public static void deliver(final ServerPlayer target, final LuckData luck) {
        final var configuredList = OBServerConfig.FLIM_FLAM_BLACKLIST.get();
        final boolean reverse = OBServerConfig.FLIM_FLAM_REVERSE_BLACKLIST.get();
        final boolean safeOnly = OBServerConfig.FLIM_FLAM_SAFE_ONLY.get();
        final List<Prank> candidates = new ArrayList<>();
        int totalWeight = 0;
        for (final Prank prank : EFFECTS.values()) {
            if (luck.getLuck() > prank.maximumLuck() || (safeOnly && !prank.safe())
                    || configuredList.contains(prank.id()) != reverse) continue;
            candidates.add(prank);
            totalWeight += prank.weight();
        }

        while (!candidates.isEmpty()) {
            int roll = target.getRandom().nextInt(totalWeight);
            for (int index = 0; index < candidates.size(); index++) {
                final Prank prank = candidates.get(index);
                roll -= prank.weight();
                if (roll >= 0) continue;
                if (execute(prank, target)) {
                    luck.addLuck(-prank.cost());
                    if (!prank.silent()) target.sendSystemMessage(Component.translatable("flimflam.open_blocks.affected"));
                    return;
                }
                totalWeight -= prank.weight();
                candidates.remove(index);
                break;
            }
        }
    }

    private static boolean execute(final Prank prank, final ServerPlayer target) {
        try {
            return prank.action().execute(target);
        } catch (final RuntimeException error) {
            LogUtils.getLogger().warn("Flim-Flam effect {} failed for {}", prank.id(), target.getScoreboardName(), error);
            return false;
        }
    }

    private static void register(final String id, final int cost, final int weight, final boolean safe, final EffectAction action) {
        register(new Prank(id, cost, weight, cost, safe, false, action));
    }

    private static void register(final Prank prank) {
        EFFECTS.put(prank.id(), prank);
    }

    private static boolean inventoryShuffle(final ServerPlayer target) {
        if (target.containerMenu != target.inventoryMenu) return false;
        Collections.shuffle(target.getInventory().items);
        target.inventoryMenu.broadcastChanges();
        return true;
    }

    private static boolean uselessTool(final ServerPlayer target) {
        final ItemStack dropped = USELESS_TOOL_POOL.get(target.getRandom().nextInt(USELESS_TOOL_POOL.size())).apply(target);
        final var enchantments = EnchantmentHelper.getAvailableEnchantmentResults(30, dropped, true);
        if (enchantments.isEmpty()) return false;
        final var selection = enchantments.get(target.getRandom().nextInt(enchantments.size()));
        dropped.enchant(selection.enchantment, target.getRandom().nextInt(selection.level) + 1);
        dropped.setDamageValue(dropped.getMaxDamage());
        final var item = new ItemEntity(target.serverLevel(), target.getX(), target.getY(), target.getZ(), dropped);
        item.setDefaultPickUpDelay();
        return target.serverLevel().addFreshEntity(item);
    }

    private static boolean bane(final ServerPlayer target) {
        for (final ItemStack stack : target.getInventory().items) {
            if (!stack.isEmpty() && stack.getMaxStackSize() == 1 && !stack.isEnchantable() && !stack.isEnchanted()) {
                stack.enchant(Enchantments.BANE_OF_ARTHROPODS, 5);
                target.inventoryMenu.broadcastChanges();
                return true;
            }
        }

        return false;
    }

    private static boolean epicLore(final ServerPlayer target) {
        final List<ItemStack> candidates = new ArrayList<>();
        for (final EquipmentSlot slot : EquipmentSlot.values()) {
            final ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty()) candidates.add(stack);
        }

        if (candidates.isEmpty()) return false;
        final ItemStack selected = candidates.get(target.getRandom().nextInt(candidates.size()));

        final String history = OBFlimFlamLore.history(target.getRandom(), target.getScoreboardName(), historyItemType(selected));
        final ListTag lore = new ListTag();
        final StringBuilder line = new StringBuilder();
        for (final String word : history.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > 30) {
                lore.add(StringTag.valueOf(line.toString()));
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lore.add(StringTag.valueOf(line.toString()));
        selected.getOrCreateTagElement("display").put("SillyLore", lore);
        target.containerMenu.broadcastChanges();
        return true;
    }

    private static String historyItemType(final ItemStack stack) {
        final var item = stack.getItem();
        if (item instanceof ArmorItem armor) return armor.getType().getName();
        if (item instanceof PickaxeItem) return "pickaxe";
        if (item instanceof AxeItem) return "axe";
        if (item instanceof ShovelItem) return "shovel";
        if (item instanceof ShearsItem) return "shears";
        if (item instanceof BlockItem) return "block";
        if (item instanceof BucketItem) return "bucket";
        return "gizmo";
    }

    private static boolean livingRename(final ServerPlayer target) {
        final AABB around = target.getBoundingBox().inflate(SEARCH_RADIUS_MOBS);
        final List<Mob> living = target.serverLevel().getEntitiesOfClass(Mob.class, around,
                entity -> entity.isAlive() && !entity.hasCustomName());
        if (living.isEmpty()) return false;
        final Mob selected = living.get(target.getRandom().nextInt(living.size()));
        selected.setCustomName(Component.literal(StringUtils.abbreviate(OBFlimFlamLore.name(target.getRandom()), 64)));
        return true;
    }

    private static boolean squid(final ServerPlayer target) {
        if (target.isVehicle()) return false;

        final Squid squid = EntityType.SQUID.create(target.level());
        if (squid == null) return false;
        squid.moveTo(target.getX(), target.getBoundingBox().minY, target.getZ(), target.getYRot(), target.getXRot());
        squid.setCustomName(Component.literal(SQUID_NAMES.get(target.getRandom().nextInt(SQUID_NAMES.size()))));
        return target.serverLevel().addFreshEntity(squid);
    }

    private static boolean sheepDye(final ServerPlayer target) {
        final AABB around = target.getBoundingBox().inflate(SEARCH_RADIUS_MOBS);
        final List<Sheep> sheep = target.serverLevel().getEntitiesOfClass(Sheep.class, around);
        if (sheep.isEmpty()) return false;

        final Sheep selected = sheep.get(target.getRandom().nextInt(sheep.size()));
        final DyeColor[] colors = DyeColor.values();
        selected.setColor(colors[target.getRandom().nextInt(colors.length)]);
        return true;
    }

    private static boolean invisibleMobs(final ServerPlayer target) {
        final AABB around = target.getBoundingBox().inflate(SEARCH_RADIUS_MOBS);
        final List<Mob> mobs = target.serverLevel().getEntitiesOfClass(Mob.class, around, Mob::isAlive);
        if (mobs.isEmpty()) return false;

        for (final Mob entity : mobs) {
            if (target.getRandom().nextFloat() < 0.3F) {
                entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_DURATION, 1));
            }
        }
        return true;
    }

    private static boolean sound(final ServerPlayer target) {
        final SoundEvent sound = SOUND_POOL.get(target.getRandom().nextInt(SOUND_POOL.size())).get();
        target.playNotifySound(sound, SoundSource.MASTER, 1.0F, 1.0F);
        return true;
    }

    private static boolean snowballs(final ServerPlayer target) {
        final ServerLevel level = target.serverLevel();
        boolean spawned = false;
        for (int i = 0; i < SNOWBALL_COUNT; i++) {
            final Snowball snowball = new Snowball(level, target.getX(), target.getY() + 4.0, target.getZ());
            snowball.setDeltaMovement(
                    target.getRandom().nextGaussian() * 0.05,
                    1.0,
                    target.getRandom().nextGaussian() * 0.05);
            spawned |= level.addFreshEntity(snowball);
        }

        return spawned;
    }

    private static boolean teleport(final ServerPlayer target) {
        final ThrownEnderpearl pearl = new ThrownEnderpearl(target.level(), target);
        pearl.setPos(target.getX(), target.getY() + 1.0, target.getZ());
        pearl.setDeltaMovement(
                target.getRandom().nextGaussian(),
                0.5,
                target.getRandom().nextGaussian());
        return target.serverLevel().addFreshEntity(pearl);
    }

    private static boolean mount(final ServerPlayer target) {
        final AABB around = target.getBoundingBox().inflate(SEARCH_RADIUS_MOUNT);
        final List<Mob> mobs = target.serverLevel().getEntitiesOfClass(Mob.class, around,
                entity -> entity.isAlive() && !(entity instanceof Creeper) && !(entity instanceof Squid));
        if (mobs.isEmpty()) return false;

        final Mob selected = mobs.get(target.getRandom().nextInt(mobs.size()));
        return target.startRiding(selected);
    }

    private static boolean encase(final ServerPlayer target) {
        final ServerLevel level = target.serverLevel();
        final BlockPos floor = target.blockPosition().below();
        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (final BlockPos pos : BlockPos.betweenClosed(floor.offset(-1, 0, -1), floor.offset(1, 3, 1))) {
            final boolean gap = pos.getX() == floor.getX() && pos.getZ() == floor.getZ() && pos.getY() < floor.getY() + 3;
            if (!gap && level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.isEmptyBlock(pos)) {
                blocks.put(pos.immutable(), Blocks.DIRT.defaultBlockState());
            }
        }

        final BlockPos torchPos = floor.above(2);
        final BlockState torch = Blocks.TORCH.defaultBlockState();
        if (level.isEmptyBlock(torchPos) && torch.canSurvive(level, torchPos)) {
            blocks.put(torchPos, torch);
        }

        return placePrankBlocks(target, blocks);
    }

    private static boolean creepers(final ServerPlayer target) {
        boolean spawned = false;
        for (int i = 0; i < CREEPER_COUNT; i++) {
            final Creeper creeper = EntityType.CREEPER.create(target.level());
            if (creeper == null) continue;
            final double x = target.getX() + 20.0 * (target.getRandom().nextDouble() - 0.5);
            final double y = target.getY() + 5.0 * (1.0 + target.getRandom().nextDouble());
            final double z = target.getZ() + 20.0 * (target.getRandom().nextDouble() - 0.5);
            creeper.moveTo(x, y, z, target.getYRot(), target.getXRot());
            final CompoundTag state = new CompoundTag();
            creeper.addAdditionalSaveData(state);
            state.putBoolean("powered", true);
            state.putByte("ExplosionRadius", (byte) 0);
            creeper.readAdditionalSaveData(state);
            spawned |= target.serverLevel().addFreshEntity(creeper);
        }

        return spawned;
    }

    private static boolean disarm(final ServerPlayer target) {
        boolean dropped = false;
        for (int i = 0; i < target.getInventory().armor.size(); i++) {
            dropped |= tryDropFromList(target, target.getInventory().armor, i);
        }
        dropped |= tryDropFromList(target, target.getInventory().items, target.getInventory().selected);
        target.containerMenu.broadcastChanges();
        return dropped;
    }

    private static boolean effect(final ServerPlayer target) {
        final int first = target.getRandom().nextInt(EFFECT_POOL.size());
        final int second = target.getRandom().nextInt(EFFECT_POOL.size() - 1);
        final boolean applied = applyEffect(target, EFFECT_POOL.get(first));
        return applyEffect(target, EFFECT_POOL.get(second >= first ? second + 1 : second)) || applied;
    }

    private static boolean applyEffect(final ServerPlayer target, final EffectMeta selected) {
        final int duration = selected.durationMin + target.getRandom().nextInt(selected.durationMax - selected.durationMin + 1);
        final int level = selected.levelMin + target.getRandom().nextInt(selected.levelMax - selected.levelMin + 1);
        return target.addEffect(new MobEffectInstance(selected.effect, duration, level));
    }

    private static boolean skyblock(final ServerPlayer target) {
        final ServerLevel level = target.serverLevel();
        if (level.dimension() == Level.NETHER || level.dimensionType().ultraWarm()) return false;

        final BlockPos origin = target.blockPosition();
        final int trapY = Math.min(origin.getY() + 150, Math.min(250, level.getMaxBuildHeight() - 3));
        final BlockPos center = origin.atY(trapY);
        final BlockPos feet = center.above();
        if (!level.isEmptyBlock(center) || !level.isEmptyBlock(feet) || !level.isEmptyBlock(feet.above())) return false;

        final Vec3 destination = Vec3.atBottomCenterOf(feet);
        final var teleport = new EntityTeleportEvent(target, destination.x, destination.y, destination.z);
        if (MinecraftForge.EVENT_BUS.post(teleport) || !teleport.getTarget().equals(destination)) return false;

        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        final BlockState ice = Blocks.ICE.defaultBlockState();
        blocks.put(center.below(), ice);
        for (final Direction side : Direction.Plane.HORIZONTAL) {
            blocks.put(center.relative(side), ice);
        }
        if (!placePrankBlocks(target, blocks)) return false;

        target.stopRiding();
        target.teleportTo(destination.x, destination.y, destination.z);
        return true;
    }

    private static boolean placePrankBlocks(final ServerPlayer target, final Map<BlockPos, BlockState> blocks) {
        if (blocks.isEmpty()) return false;
        final ServerLevel level = target.serverLevel();
        final List<BlockSnapshot> placed = new ArrayList<>(blocks.size());
        boolean accepted = false;
        try {
            for (final var entry : blocks.entrySet()) {
                final BlockPos pos = entry.getKey();
                if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                        || !level.mayInteract(target, pos) || !level.isEmptyBlock(pos)) return false;
                final var snapshot = BlockSnapshot.create(level.dimension(), level, pos, PRANK_PLACEMENT_FLAGS);
                if (!level.setBlock(pos, entry.getValue(), PRANK_PLACEMENT_FLAGS)) return false;
                placed.add(snapshot);
            }
            final boolean denied = placed.size() == 1
                    ? ForgeEventFactory.onBlockPlace(target, placed.get(0), Direction.UP)
                    : ForgeEventFactory.onMultiBlockPlace(target, List.copyOf(placed), Direction.UP);
            if (denied || placed.stream().anyMatch(snapshot -> snapshot.getCurrentBlock() != blocks.get(snapshot.getPos()))) return false;
            accepted = true;
        } finally {
            if (!accepted) {
                for (int i = placed.size() - 1; i >= 0; i--) placed.get(i).restore(true, false);
            }
        }

        for (final BlockSnapshot snapshot : placed) {
            final BlockPos pos = snapshot.getPos();
            final BlockState state = blocks.get(pos);
            level.markAndNotifyBlock(pos, level.getChunkAt(pos), snapshot.getReplacedBlock(), state, Block.UPDATE_ALL, Block.UPDATE_LIMIT);
        }
        return true;
    }

    private static boolean tryDropFromList(final ServerPlayer target, final List<ItemStack> list, final int slot) {
        if (slot < 0 || slot >= list.size()) return false;
        final ItemStack stack = list.get(slot);
        if (stack.isEmpty() || target.getRandom().nextFloat() > 0.5F) return false;

        final var dropped = new ItemEntity(target.serverLevel(), target.getX(), target.getEyeY() - 0.3, target.getZ(), stack.copyWithCount(1));
        final float speed = target.getRandom().nextFloat() * 0.5F;
        final float angle = target.getRandom().nextFloat() * Mth.TWO_PI;
        dropped.setDeltaMovement(-Mth.sin(angle) * speed, 0.2, Mth.cos(angle) * speed);
        dropped.setPickUpDelay(40);
        dropped.setThrower(target.getUUID());
        if (MinecraftForge.EVENT_BUS.post(new ItemTossEvent(dropped, target)) || list.get(slot) != stack || stack.isEmpty()
                || !target.serverLevel().addFreshEntity(dropped)) return false;
        if (list.get(slot) != stack || stack.isEmpty()) {
            dropped.discard();
            return false;
        }
        stack.shrink(1);
        return true;
    }

    private static int seconds(final int seconds) {
        return seconds * 20;
    }
}
