package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.client.OBSoundIcons;
import art.arcane.openblocks.item.OBSonicGlassesItem;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class SonicGlassesGameTests {

    @GameTest(template = "empty")
    public static void sonicGlassesKeepTheLegacyRecipeAndEquipNormally(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.IRON_INGOT, Items.IRON_HELMET, Items.IRON_INGOT,
                Items.OBSIDIAN, Items.STICK, Items.OBSIDIAN);
        final var glasses = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(glasses.is(OBItems.SONIC_GLASSES.get()) && glasses.getItem() instanceof OBSonicGlassesItem armor
                && armor.getMaterial() == ArmorMaterials.IRON && armor.getEquipmentSlot() == EquipmentSlot.HEAD
                && glasses.getMaxStackSize() == 1, "The original recipe must make one iron-tier wearable pair of glasses");
        grid.setItem(1, new ItemStack(Items.GOLDEN_HELMET));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "The recipe accepted a golden helmet");

        glasses.setHoverName(Component.literal("Sonics"));
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "SonicGlassesQA"));
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, glasses);
        final var result = player.gameMode.useItem(player, player.level(), glasses, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction() && player.getMainHandItem().isEmpty()
                && player.getItemBySlot(EquipmentSlot.HEAD).is(OBItems.SONIC_GLASSES.get())
                && player.getItemBySlot(EquipmentSlot.HEAD).getHoverName().getString().equals("Sonics"),
                "The normal right-click path did not equip the named glasses exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sonicGlassesMapModernAndLegacySoundNamesToOriginalIcons(final GameTestHelper helper) {
        final var friendly = icon("minecraft:entity.cow.ambient");
        final var hostile = icon("minecraft:entity.zombie.ambient");
        final var zombieEgg = SpawnEggItem.byId(EntityType.ZOMBIE);
        helper.assertTrue(friendly.layers().get(0).sprite().equals(OpenBlocks.id("item/sound_mob_friendly"))
                && hostile.layers().get(0).sprite().equals(OpenBlocks.id("item/sound_mob_hostile"))
                && hostile.layers().get(0).color() == zombieEgg.getColor(0) && hostile.layers().get(1).color() == zombieEgg.getColor(1),
                "Mob markers did not use the original symbols and both spawn egg colors");
        helper.assertTrue(icon("minecraft:entity.iron_golem.hurt").equals(icon("minecraft:entity.irongolem.hurt"))
                && icon("minecraft:entity.iron_golem.hurt").layers().get(0).sprite().equals(OpenBlocks.id("item/sound_mob_hurt")),
                "The golem's renamed sound lost its hurt marker");

        final var stone = icon("minecraft:block.stone.break");
        helper.assertTrue(stone.layers().get(0).sprite().equals(new ResourceLocation("minecraft", "block/stone"))
                && stone.layers().get(1).sprite().equals(new ResourceLocation("minecraft", "block/destroy_stage_5")),
                "A block break did not retain the material and action layers");
        helper.assertTrue(icon("minecraft:block.note_block.pling").equals(icon("minecraft:block.note.pling"))
                && icon("minecraft:block.note_block.pling").layers().get(0).color() == 0xFF00FF,
                "Flattened note block sounds lost their instrument color");
        helper.assertTrue(icon("minecraft:music_disc.cat").equals(icon("minecraft:record.cat"))
                && icon("open_blocks:luggage.eat.item").layers().get(0).sprite().equals(new ResourceLocation("minecraft", "item/apple"))
                && icon("qa:unmapped").layers().get(0).sprite().equals(OpenBlocks.id("item/sound_generic")),
                "Disc aliases, OpenBlocks sounds, or the modded fallback were lost");
        helper.succeed();
    }

    private static OBSoundIcons.Icon icon(final String sound) {
        return OBSoundIcons.forSound(new ResourceLocation(sound));
    }
}
