package art.arcane.openblocks.datagen;

import java.util.List;
import java.util.Set;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class OBLootTableProvider extends LootTableProvider {

    public OBLootTableProvider(final PackOutput output) {
        super(output, Set.of(), List.of(new SubProviderEntry(OBBlockLootSubProvider::new, LootContextParamSets.BLOCK)));
    }
}
