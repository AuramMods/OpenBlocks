package art.arcane.openblocks.command;

import art.arcane.openblocks.registry.OBSounds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.Containers;
import net.minecraft.server.level.ServerPlayer;

public final class OBFlimFlamEffects {

    @FunctionalInterface
    private interface EffectAction {
        boolean execute(ServerPlayer target);
    }

    private record EffectMeta(MobEffect effect, int levelMin, int levelMax, int durationMin, int durationMax) {}

    private static final int LORE_MAX_LINES = 4;
    private static final int INVISIBILITY_DURATION = 10 * 60 * 20;
    private static final int SNOWBALL_COUNT = 200;
    private static final int CREEPER_COUNT = 15;
    private static final int SEARCH_RADIUS_MOBS = 20;
    private static final int SEARCH_RADIUS_MOUNT = 40;

    private static final List<String> LORE_POOL = List.of(
            "This tool is now certified for absolutely no useful tasks.",
            "Mysterious craftsmanship, questionable outcomes.",
            "Legend says this item once worked.",
            "Flim-flam approved. Engineer not available.");

    private static final List<String> ENTITY_NAME_POOL = List.of(
            "Fancy Hat",
            "Not Suspicious",
            "Totally Real",
            "Definitely Helpful",
            "Ceci n'est pas une pipe");

    private static final List<Function<ServerPlayer, SoundEvent>> SOUND_POOL = List.of(
            p -> OBSounds.MOSQUITO.get(),
            p -> OBSounds.ALARM_CLOCK.get(),
            p -> OBSounds.VIBRATE.get(),
            p -> OBSounds.FEATURE_FART.get());

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

    private static final Map<String, EffectAction> EFFECTS = new LinkedHashMap<>();

    static {
        register("inventory-shuffle", OBFlimFlamEffects::inventoryShuffle);
        register("useless-tool", OBFlimFlamEffects::uselessTool);
        register("bane", OBFlimFlamEffects::bane);
        register("epic-lore", OBFlimFlamEffects::epicLore);
        register("living-rename", OBFlimFlamEffects::livingRename);
        register("squid", OBFlimFlamEffects::squid);
        register("sheep-dye", OBFlimFlamEffects::sheepDye);
        register("invisible-mobs", OBFlimFlamEffects::invisibleMobs);
        register("sound", OBFlimFlamEffects::sound);
        register("snowballs", OBFlimFlamEffects::snowballs);
        register("teleport", OBFlimFlamEffects::teleport);
        register("mount", OBFlimFlamEffects::mount);
        register("encase", OBFlimFlamEffects::encase);
        register("creepers", OBFlimFlamEffects::creepers);
        register("disarm", OBFlimFlamEffects::disarm);
        register("effect", OBFlimFlamEffects::effect);
        register("skyblock", OBFlimFlamEffects::skyblock);
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
        final EffectAction action = EFFECTS.get(name);
        return action != null && action.execute(target);
    }

    private static void register(final String id, final EffectAction action) {
        EFFECTS.put(id, action);
    }

    private static boolean inventoryShuffle(final ServerPlayer target) {
        if (target.containerMenu != target.inventoryMenu) return false;
        Collections.shuffle(target.getInventory().items);
        target.inventoryMenu.broadcastChanges();
        return true;
    }

    private static boolean uselessTool(final ServerPlayer target) {
        if (USELESS_TOOL_POOL.isEmpty()) return false;
        final ItemStack dropped = USELESS_TOOL_POOL.get(target.getRandom().nextInt(USELESS_TOOL_POOL.size())).apply(target);
        final ItemStack enchanted = EnchantmentHelper.enchantItem(target.getRandom(), dropped, 30, true);
        if (enchanted.isEmpty() || enchanted.getMaxDamage() <= 1) return false;

        enchanted.setDamageValue(enchanted.getMaxDamage() - 1);
        Containers.dropItemStack(target.level(), target.getX(), target.getY(), target.getZ(), enchanted);
        return true;
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
        for (final ItemStack stack : target.getInventory().items) {
            if (!stack.isEmpty()) candidates.add(stack);
        }

        for (final ItemStack stack : target.getInventory().armor) {
            if (!stack.isEmpty()) candidates.add(stack);
        }

        for (final ItemStack stack : target.getInventory().offhand) {
            if (!stack.isEmpty()) candidates.add(stack);
        }

        if (candidates.isEmpty()) return false;
        final ItemStack selected = candidates.get(target.getRandom().nextInt(candidates.size()));

        final CompoundTag display = selected.getOrCreateTagElement("display");
        final ListTag lore = display.contains("Lore", Tag.TAG_LIST) ? display.getList("Lore", Tag.TAG_STRING) : new ListTag();
        if (lore.size() >= LORE_MAX_LINES) return false;

        final String line = LORE_POOL.get(target.getRandom().nextInt(LORE_POOL.size()));
        final Component loreComponent = Component.literal(line).withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC);
        lore.add(StringTag.valueOf(Component.Serializer.toJson(loreComponent)));
        display.put("Lore", lore);
        target.inventoryMenu.broadcastChanges();
        return true;
    }

    private static boolean livingRename(final ServerPlayer target) {
        final AABB around = target.getBoundingBox().inflate(SEARCH_RADIUS_MOBS);
        final List<LivingEntity> living = target.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                around,
                entity -> entity != target && !(entity instanceof Player) && entity.isAlive() && !entity.hasCustomName());
        if (living.isEmpty()) return false;

        Collections.shuffle(living);
        final LivingEntity selected = living.get(0);
        final String name = ENTITY_NAME_POOL.get(target.getRandom().nextInt(ENTITY_NAME_POOL.size()));
        selected.setCustomName(Component.literal(name));
        return true;
    }

    private static boolean squid(final ServerPlayer target) {
        if (target.isPassenger()) return false;

        final Squid squid = EntityType.SQUID.create(target.level());
        if (squid == null) return false;
        squid.moveTo(target.getX(), target.getBoundingBox().minY, target.getZ(), target.getYRot(), target.getXRot());
        squid.setCustomName(Component.literal(ENTITY_NAME_POOL.get(target.getRandom().nextInt(ENTITY_NAME_POOL.size()))));
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
        final List<LivingEntity> mobs = target.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                around,
                entity -> entity != target && !(entity instanceof Player) && entity.isAlive());
        if (mobs.isEmpty()) return false;

        boolean changed = false;
        for (final LivingEntity entity : mobs) {
            if (target.getRandom().nextFloat() < 0.3F) {
                entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_DURATION, 1));
                changed = true;
            }
        }
        return changed;
    }

    private static boolean sound(final ServerPlayer target) {
        if (SOUND_POOL.isEmpty()) return false;
        final SoundEvent sound = SOUND_POOL.get(target.getRandom().nextInt(SOUND_POOL.size())).apply(target);
        target.level().playSound(null, target.blockPosition(), sound, SoundSource.MASTER, 1.0F, 1.0F);
        return true;
    }

    private static boolean snowballs(final ServerPlayer target) {
        for (int i = 0; i < SNOWBALL_COUNT; i++) {
            final Snowball snowball = new Snowball(target.level(), target);
            snowball.setPos(target.getX(), target.getY() + 4.0, target.getZ());
            snowball.setDeltaMovement(
                    target.getRandom().nextGaussian() * 0.05,
                    1.0,
                    target.getRandom().nextGaussian() * 0.05);
            target.serverLevel().addFreshEntity(snowball);
        }

        return true;
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
        final List<LivingEntity> mobs = target.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                around,
                entity -> entity != target && !(entity instanceof Player) && !(entity instanceof Creeper) && !(entity instanceof Squid));
        if (mobs.isEmpty()) return false;

        final LivingEntity selected = mobs.get(target.getRandom().nextInt(mobs.size()));
        return target.startRiding(selected, true);
    }

    private static boolean encase(final ServerPlayer target) {
        final int playerX = Mth.floor(target.getX());
        final int playerY = Mth.floor(target.getBoundingBox().minY) - 1;
        final int playerZ = Mth.floor(target.getZ());
        boolean changed = false;

        for (int y = playerY; y <= playerY + 3; y++) {
            for (int x = playerX - 1; x <= playerX + 1; x++) {
                for (int z = playerZ - 1; z <= playerZ + 1; z++) {
                    final boolean isGap = y < playerY + 3 && x == playerX && z == playerZ;
                    if (isGap) continue;

                    final BlockPos pos = new BlockPos(x, y, z);
                    if (target.level().isEmptyBlock(pos)) {
                        changed |= target.level().setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                    }
                }
            }
        }

        final BlockPos torchPos = new BlockPos(playerX, playerY + 2, playerZ);
        if (target.level().isEmptyBlock(torchPos) && Blocks.TORCH.defaultBlockState().canSurvive(target.level(), torchPos)) {
            changed |= target.level().setBlock(torchPos, Blocks.TORCH.defaultBlockState(), 3);
        }

        return changed;
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
            creeper.setNoAi(true);
            creeper.setCustomName(Component.literal("Dummy Creeper"));
            spawned |= target.serverLevel().addFreshEntity(creeper);
        }

        return spawned;
    }

    private static boolean disarm(final ServerPlayer target) {
        boolean dropped = false;
        dropped |= tryDropFromList(target, target.getInventory().items, target.getInventory().selected);
        for (int i = 0; i < target.getInventory().armor.size(); i++) {
            dropped |= tryDropFromList(target, target.getInventory().armor, i);
        }
        dropped |= tryDropFromList(target, target.getInventory().offhand, 0);
        target.inventoryMenu.broadcastChanges();
        return dropped;
    }

    private static boolean effect(final ServerPlayer target) {
        final List<EffectMeta> effects = new ArrayList<>(EFFECT_POOL);
        if (effects.size() < 2) return false;
        Collections.shuffle(effects);

        for (int i = 0; i < 2; i++) {
            final EffectMeta selected = effects.get(i);
            final int duration = selected.durationMin + target.getRandom().nextInt(selected.durationMax - selected.durationMin + 1);
            final int level = selected.levelMin + target.getRandom().nextInt(selected.levelMax - selected.levelMin + 1);
            target.addEffect(new MobEffectInstance(selected.effect, duration, level));
        }

        return true;
    }

    private static boolean skyblock(final ServerPlayer target) {
        final var level = target.serverLevel();
        if (level.dimensionType().ultraWarm()) return false;

        final int maxY = level.getMaxBuildHeight() - 6;
        final int trapY = Mth.clamp(target.blockPosition().getY() + 150, level.getMinBuildHeight() + 5, maxY);
        final BlockPos center = new BlockPos(target.blockPosition().getX(), trapY, target.blockPosition().getZ());
        final BlockPos[] blocks = new BlockPos[] {
                center.below(),
                center.east(),
                center.north(),
                center.south(),
                center.west()
        };

        for (final BlockPos pos : blocks) {
            if (!level.isEmptyBlock(pos)) return false;
        }

        for (final BlockPos pos : blocks) {
            if (!level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3)) return false;
        }

        target.teleportTo(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D);
        return true;
    }

    private static boolean tryDropFromList(final ServerPlayer target, final List<ItemStack> list, final int slot) {
        if (slot < 0 || slot >= list.size()) return false;
        final ItemStack stack = list.get(slot);
        if (stack.isEmpty() || target.getRandom().nextFloat() > 0.5F) return false;

        final ItemStack dropped = stack.split(1);
        if (dropped.isEmpty()) return false;

        Containers.dropItemStack(target.level(), target.getX(), target.getY(), target.getZ(), dropped);
        return true;
    }

    private static int seconds(final int seconds) {
        return seconds * 20;
    }
}
