package art.arcane.openblocks.world;

import art.arcane.openblocks.config.OBServerConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public final class OBTechnicolorLoot extends LootModifier {

    public static final Codec<OBTechnicolorLoot> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ResourceLocation.CODEC.fieldOf("loot_table").forGetter(modifier -> modifier.table))
            .apply(instance, OBTechnicolorLoot::new));

    private final ResourceLocation table;

    private OBTechnicolorLoot(final LootItemCondition[] conditions, final ResourceLocation table) {
        super(conditions);
        this.table = table;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected ObjectArrayList<ItemStack> doApply(final ObjectArrayList<ItemStack> loot, final LootContext context) {
        if (OBServerConfig.SPEC.isLoaded() && !OBServerConfig.TECHNICOLOR_LOOT.get()) return loot;

        // This is an additional pool in the requesting chest, using the chest's own RNG and luck.
        context.getResolver().getLootTable(table).getRandomItemsRaw(context, loot::add);
        return loot;
    }

    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
