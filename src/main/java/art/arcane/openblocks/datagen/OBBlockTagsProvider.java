package art.arcane.openblocks.datagen;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.registry.OBBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class OBBlockTagsProvider extends BlockTagsProvider {

    public OBBlockTagsProvider(final PackOutput output,
            final CompletableFuture<HolderLookup.Provider> lookupProvider,
            final ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, OpenBlocks.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(final HolderLookup.Provider provider) {
        OBBlocks.all().forEach(block -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block.get()));
        OBBlocks.all().forEach(block -> tag(BlockTags.NEEDS_STONE_TOOL).add(block.get()));

        tag(BlockTags.CLIMBABLE).add(
                OBBlocks.LADDER.get(),
                OBBlocks.ROPE_LADDER.get(),
                OBBlocks.SCAFFOLDING.get());
    }
}
