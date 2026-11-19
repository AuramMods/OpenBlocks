package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.capability.OBCapabilities;
import art.arcane.openblocks.capability.OBCapabilities.LuckData;
import art.arcane.openblocks.command.OBFlimFlamEffects;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBEnchantments;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class FlimFlamGameTests {

    @GameTest(template = "empty")
    public static void flimFlamBookCountsOccupiedSlotsAndFitsSwordAndArmor(final GameTestHelper helper) {
        final var enchantment = OBEnchantments.FLIM_FLAM.get();
        helper.assertTrue(enchantment.getRarity() == Enchantment.Rarity.RARE && enchantment.getMinCost(1) == 41
                && enchantment.getMaxCost(4) == 81 && enchantment.canEnchant(new ItemStack(Items.IRON_HELMET))
                && enchantment.canEnchant(new ItemStack(Items.IRON_SWORD)) && !enchantment.canEnchant(new ItemStack(Items.IRON_PICKAXE)),
                "The enchantment must use the original sword and armor acquisition rules");

        final var grid = ComponentRecipeGameTests.input(helper, Items.BOOK, Items.EMERALD);
        grid.setItem(0, new ItemStack(Items.BOOK, 12));
        grid.setItem(1, new ItemStack(Items.EMERALD, 4));
        final var one = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(one.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantments(one).get(enchantment) == 1,
                "A full emerald stack in one slot pays for only one level");

        for (int slot = 2; slot <= 4; slot++) grid.setItem(slot, new ItemStack(Items.EMERALD));
        final var four = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(four.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantments(four).get(enchantment) == 4,
                "Four separately occupied emerald slots must craft level IV");
        grid.setItem(5, new ItemStack(Items.EMERALD));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "A fifth emerald slot must not match");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flimFlamArmorReflectsLuckAndTiesCancel(final GameTestHelper helper) {
        final var attacker = player(helper);
        final var defender = player(helper);
        equip(defender, EquipmentSlot.HEAD, Items.IRON_HELMET, 2);
        equip(defender, EquipmentSlot.FEET, Items.IRON_BOOTS, 2);
        hit(attacker, defender);
        final int reflected = luck(attacker).getLuck();
        helper.assertTrue(reflected >= -20 && reflected <= -1 && luck(defender).getLuck() == 0,
                "Four armor levels must reflect one D20 roll onto an unenchanted attacker");

        equip(attacker, EquipmentSlot.MAINHAND, Items.IRON_SWORD, 1);
        hit(attacker, defender);
        helper.assertTrue(luck(attacker).getLuck() == reflected && luck(defender).getLuck() == 0,
                "Equal attack and defense must spare both players");
        equip(attacker, EquipmentSlot.MAINHAND, Items.IRON_SWORD, 4);
        hit(attacker, defender);
        helper.assertTrue(luck(attacker).getLuck() == reflected && luck(defender).getLuck() >= -60 && luck(defender).getLuck() <= -3,
                "A three-level attack advantage must subtract three D20 rolls from the defender");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forcedFlimFlamPaysLuckAndRespectsCooldownAndHistorySlots(final GameTestHelper helper) {
        final var attacker = player(helper);
        final var defender = player(helper);
        equip(attacker, EquipmentSlot.MAINHAND, Items.IRON_SWORD, 2);
        final var held = new ItemStack(Items.IRON_PICKAXE);
        final var ordinary = new ListTag();
        ordinary.add(StringTag.valueOf("{\"text\":\"Ordinary lore\"}"));
        held.getOrCreateTagElement("display").put("Lore", ordinary);
        defender.setItemSlot(EquipmentSlot.MAINHAND, held);
        final var storage = new ItemStack(Items.DIAMOND);
        defender.getInventory().setItem(20, storage);
        final var luck = luck(defender);
        luck.addLuck(-40);
        final var prior = filter(List.of("epic-lore"), true, true);
        try {
            hit(attacker, defender);
            final int afterHit = luck.getLuck();
            helper.assertTrue(luck.isForceNext() && afterHit <= -42, "Combat below the original threshold must schedule a forced prank");
            tick(defender);
            final var display = held.getTagElement("display");
            final var history = display.getList("SillyLore", Tag.TAG_STRING);
            helper.assertTrue(!history.isEmpty() && display.get("Lore").equals(ordinary) && storage.getTag() == null,
                    "The original history grammar must only touch equipped items and preserve ordinary lore");
            helper.assertTrue(luck.getLuck() == afterHit + 10 && !luck.isForceNext() && luck.getCooldown() == 300,
                    "A successful history effect must credit ten luck and start fifteen seconds of cooldown");
            tick(defender);
            helper.assertTrue(luck.getCooldown() == 299 && luck.getLuck() == afterHit + 10 && display.get("SillyLore").equals(history),
                    "An ordinary tick in the cooldown must not immediately deliver another prank");
        } finally {
            prior.restore();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void automaticFlimFlamHonorsAllFiltersWhileNamedCommandsRemainExplicit(final GameTestHelper helper) {
        final var target = player(helper);
        final var saddle = new ItemStack(Items.SADDLE);
        target.getInventory().setItem(0, saddle);
        final var luck = luck(target);
        luck.addLuck(-126);
        final var prior = filter(List.of("bane"), false, false);
        try {
            OBServerConfig.FLIM_FLAM_REVERSE_BLACKLIST.set(true);
            OBServerConfig.FLIM_FLAM_BLACKLIST.set(List.of("disarm"));
            OBServerConfig.FLIM_FLAM_SAFE_ONLY.set(true);
            luck.setForceNext(true);
            tick(target);
            helper.assertTrue(!luck.isForceNext() && luck.getLuck() == -126 && saddle.getCount() == 1,
                    "Safe-only must block an unsafe effect even if it is explicitly allowlisted");

            OBServerConfig.FLIM_FLAM_BLACKLIST.set(OBFlimFlamEffects.effectNames());
            OBServerConfig.FLIM_FLAM_REVERSE_BLACKLIST.set(false);
            OBServerConfig.FLIM_FLAM_SAFE_ONLY.set(false);
            luck.setForceNext(true);
            tick(target);
            helper.assertTrue(luck.getLuck() == -126 && !saddle.isEnchanted(), "The blacklist must suppress otherwise eligible effects");

            OBServerConfig.FLIM_FLAM_BLACKLIST.set(List.of("bane"));
            OBServerConfig.FLIM_FLAM_REVERSE_BLACKLIST.set(true);
            luck.addLuck(121);
            luck.setForceNext(true);
            tick(target);
            helper.assertTrue(luck.getLuck() == -5 && !saddle.isEnchanted(), "The minimum debt must apply even to an allowlisted effect");
            helper.assertTrue(OBFlimFlamEffects.execute("bane", target) && saddle.isEnchanted() && luck.getLuck() == -5,
                    "Explicit admin effects must preserve the original ability to bypass automatic filters without charging luck");
            helper.assertTrue(OBFlimFlamEffects.effectNames().size() == 17, "The original effect names must all remain available");
        } finally {
            prior.restore();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disarmNeverLosesBlockedTossesOrTouchesTheOffhand(final GameTestHelper helper) {
        final var target = player(helper);
        final var selected = new ItemStack(Items.EMERALD, 12);
        final var offhand = new ItemStack(Items.DIAMOND, 2);
        target.getInventory().setItem(0, selected);
        target.setItemSlot(EquipmentSlot.OFFHAND, offhand);
        final var guard = new TossGuard(target);
        MinecraftForge.EVENT_BUS.register(guard);
        try {
            for (int attempt = 0; attempt < 40 && !guard.seen; attempt++) OBFlimFlamEffects.execute("disarm", target);
            helper.assertTrue(guard.seen && selected.getCount() == 12 && offhand.getCount() == 2,
                    "A Forge-denied toss must leave its source intact, and the offhand must stay out of disarm");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(guard);
        }
        helper.succeed();
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "FlimFlamQA"));
        player.setGameMode(GameType.SURVIVAL);
        final Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static void equip(final FakePlayer player, final EquipmentSlot slot, final Item item, final int level) {
        final var stack = new ItemStack(item);
        stack.enchant(OBEnchantments.FLIM_FLAM.get(), level);
        player.setItemSlot(slot, stack);
    }

    private static LuckData luck(final FakePlayer player) {
        return player.getCapability(OBCapabilities.LUCK).orElseThrow(IllegalStateException::new);
    }

    private static void hit(final FakePlayer attacker, final FakePlayer defender) {
        MinecraftForge.EVENT_BUS.post(new LivingHurtEvent(defender, defender.damageSources().playerAttack(attacker), 2.0F));
    }

    private static void tick(final FakePlayer player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
    }

    private static Filters filter(final List<String> ids, final boolean reverse, final boolean safe) {
        final var prior = new Filters(OBServerConfig.FLIM_FLAM_BLACKLIST.get(),
                OBServerConfig.FLIM_FLAM_REVERSE_BLACKLIST.get(), OBServerConfig.FLIM_FLAM_SAFE_ONLY.get());
        OBServerConfig.FLIM_FLAM_BLACKLIST.set(ids);
        OBServerConfig.FLIM_FLAM_REVERSE_BLACKLIST.set(reverse);
        OBServerConfig.FLIM_FLAM_SAFE_ONLY.set(safe);
        return prior;
    }

    private record Filters(List<? extends String> ids, boolean reverse, boolean safe) {
        void restore() {
            OBServerConfig.FLIM_FLAM_BLACKLIST.set(ids);
            OBServerConfig.FLIM_FLAM_REVERSE_BLACKLIST.set(reverse);
            OBServerConfig.FLIM_FLAM_SAFE_ONLY.set(safe);
        }
    }

    private static final class TossGuard {
        private final FakePlayer player;
        private boolean seen;

        TossGuard(final FakePlayer player) { this.player = player; }

        @SubscribeEvent
        public void onToss(final ItemTossEvent event) {
            if (event.getPlayer() != player) return;
            seen = true;
            event.setCanceled(true);
        }
    }
}
