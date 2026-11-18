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
        final var ladder = OBBlocks.LADDER.get();
        final var ropeLadder = OBBlocks.ROPE_LADDER.get();
        final var scaffolding = OBBlocks.SCAFFOLDING.get();
        OBBlocks.all().forEach(entry -> {
            final var block = entry.get();
            if (block == ladder || block == ropeLadder) {
                tag(BlockTags.MINEABLE_WITH_AXE).add(block);
            } else if (block != scaffolding) {
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
                tag(BlockTags.NEEDS_STONE_TOOL).add(block);
            }
        });

        tag(BlockTags.CLIMBABLE).add(ropeLadder);
    }
}
