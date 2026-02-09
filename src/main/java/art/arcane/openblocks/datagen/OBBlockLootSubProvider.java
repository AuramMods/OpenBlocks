package art.arcane.openblocks.datagen;

import art.arcane.openblocks.registry.OBBlocks;
import java.util.Set;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraftforge.registries.RegistryObject;

public class OBBlockLootSubProvider extends BlockLootSubProvider {

    public OBBlockLootSubProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        OBBlocks.all().forEach(block -> dropSelf(block.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return OBBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
