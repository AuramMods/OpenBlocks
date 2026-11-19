package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OBProfessions {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, OpenBlocks.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, OpenBlocks.MODID);
    public static final ResourceKey<PoiType> JUKEBOX = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, OpenBlocks.id("jukebox"));

    static {
        POI_TYPES.register(JUKEBOX.location().getPath(),
                () -> new PoiType(Set.copyOf(Blocks.JUKEBOX.getStateDefinition().getPossibleStates()), 1, 1));
    }

    public static final RegistryObject<VillagerProfession> RADIO = PROFESSIONS.register("radio",
            () -> new VillagerProfession("radio", poi -> poi.is(JUKEBOX), poi -> poi.is(JUKEBOX),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.NOTE_BLOCK_HARP.value()));

    private OBProfessions() {}

    @SubscribeEvent
    public static void onVillagerTrades(final VillagerTradesEvent event) {
        if (event.getType() != RADIO.get()) return;
        event.getTrades().get(1).add((trader, random) -> offer(Items.NOTE_BLOCK, random.nextIntBetweenInclusive(5, 7), Items.EMERALD, 16, 2));
        event.getTrades().get(2).add((trader, random) -> offer(Items.EMERALD, random.nextIntBetweenInclusive(10, 15), Items.JUKEBOX, 12, 10));
        final VillagerTrades.ItemListing disc = OBProfessions::musicDiscOffer;
        event.getTrades().get(3).addAll(List.of(disc, disc));
    }

    private static MerchantOffer musicDiscOffer(final Entity trader, final RandomSource random) {
        if (!(trader instanceof Villager villager) || !OBServerConfig.RADIO_VILLAGER_ENABLED.get()) return null;
        final List<Item> available = ForgeRegistries.ITEMS.tags().getTag(ItemTags.MUSIC_DISCS).stream()
                .filter(item -> villager.getOffers().stream().noneMatch(offer -> offer.getResult().is(item)))
                .toList();
        if (available.isEmpty()) return null;
        final Item disc = available.get(random.nextInt(available.size()));
        return new MerchantOffer(new ItemStack(Items.EMERALD, random.nextIntBetweenInclusive(3, 6)),
                new ItemStack(disc), 12, 10, 0.05F);
    }

    private static MerchantOffer offer(final Item priceItem, final int price, final Item result, final int uses, final int xp) {
        if (!OBServerConfig.RADIO_VILLAGER_ENABLED.get()) return null;
        return new MerchantOffer(new ItemStack(priceItem, price), new ItemStack(result), uses, xp, 0.05F);
    }
}
