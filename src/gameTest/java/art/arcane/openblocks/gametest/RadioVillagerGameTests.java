package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBProfessions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class RadioVillagerGameTests {

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void unemployedVillagerClaimsAJukebox(final GameTestHelper helper) {
        final BlockPos job = new BlockPos(2, 1, 2);
        final var jukebox = Blocks.JUKEBOX.defaultBlockState();
        helper.assertTrue(PoiTypes.forState(jukebox.setValue(JukeboxBlock.HAS_RECORD, true))
                        .map(poi -> poi.is(OBProfessions.JUKEBOX) && poi.is(PoiTypeTags.ACQUIRABLE_JOB_SITE)).orElse(false),
                "A jukebox with a record must still count as the same job site");

        for (final BlockPos floor : BlockPos.betweenClosed(0, 0, 0, 5, 0, 5)) helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(job, jukebox);
        final Villager villager = helper.spawn(EntityType.VILLAGER, job.west());
        villager.refreshBrain(helper.getLevel());
        helper.succeedWhen(() -> {
            helper.assertTrue(villager.getVillagerData().getProfession() == OBProfessions.RADIO.get(),
                    "The villager did not naturally become an Audiophile at " + villager.blockPosition() + ": " + villager.getVillagerData().getProfession());
            helper.assertTrue(villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                            .map(site -> site.pos().equals(helper.absolutePos(job))).orElse(false),
                    "The Audiophile did not claim the expected jukebox");
            helper.assertTrue(villager.getOffers().size() == 1, "The new Audiophile is missing the novice offer");
            villager.discard();
            helper.setBlock(job, Blocks.AIR);
        });
    }

    @GameTest(template = "empty")
    public static void audiophileUnlocksTheLegacyTradeTiers(final GameTestHelper helper) {
        final Villager novice = audiophile(helper, 1);
        helper.assertTrue(novice.getOffers().size() == 1, "Expected one novice offer");
        final MerchantOffer buy = novice.getOffers().get(0);
        final int notes = buy.getBaseCostA().getCount();
        helper.assertTrue(buy.getBaseCostA().is(Items.NOTE_BLOCK) && notes >= 5 && notes <= 7
                        && buy.getResult().is(Items.EMERALD) && buy.getResult().getCount() == 1,
                "The novice must buy five to seven note blocks for an emerald");

        final var apprentice = audiophile(helper, 2).getOffers();
        helper.assertTrue(apprentice.size() == 1 && apprentice.get(0).getResult().is(Items.JUKEBOX)
                        && apprentice.get(0).getResult().getCount() == 1 && costsEmeralds(apprentice.get(0), 10, 15),
                "The apprentice must sell a jukebox for 10 to 15 emeralds");

        final var journeyman = audiophile(helper, 3);
        final var discs = journeyman.getOffers();
        helper.assertTrue(discs.size() == 2 && !discs.get(0).getResult().is(discs.get(1).getResult().getItem()),
                "The journeyman must stock two distinct records");
        helper.assertTrue(discs.stream().allMatch(offer -> offer.getResult().is(ItemTags.MUSIC_DISCS)
                        && offer.getResult().getCount() == 1 && costsEmeralds(offer, 3, 6)),
                "Each offer must sell a tagged music disc for three to six emeralds");

        final CompoundTag saved = journeyman.saveWithoutId(new CompoundTag());
        final var enabled = OBServerConfig.RADIO_VILLAGER_ENABLED;
        final boolean original = enabled.get();
        try {
            enabled.set(false);
            for (int tier = 1; tier <= 3; tier++) {
                helper.assertTrue(audiophile(helper, tier).getOffers().isEmpty(), "Disabled Audiophiles generated new trades");
            }
            final Villager restored = EntityType.VILLAGER.create(helper.getLevel());
            restored.load(saved);
            helper.assertTrue(restored.getVillagerData().getProfession() == OBProfessions.RADIO.get()
                            && restored.getOffers().createTag().equals(discs.createTag()),
                    "The server setting erased existing trade data");
        } finally {
            enabled.set(original);
        }
        helper.succeed();
    }

    private static Villager audiophile(final GameTestHelper helper, final int tier) {
        final Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        villager.setVillagerData(villager.getVillagerData().setProfession(OBProfessions.RADIO.get()).setLevel(tier));
        return villager;
    }

    private static boolean costsEmeralds(final MerchantOffer offer, final int min, final int max) {
        final int price = offer.getBaseCostA().getCount();
        return offer.getBaseCostA().is(Items.EMERALD) && price >= min && price <= max && offer.getCostB().isEmpty();
    }
}
