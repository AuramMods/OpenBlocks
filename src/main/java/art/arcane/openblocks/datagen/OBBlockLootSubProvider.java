package art.arcane.openblocks.datagen;

import art.arcane.openblocks.block.OBElevatorBlock;
import art.arcane.openblocks.block.OBRopeLadderBlock;
import art.arcane.openblocks.registry.OBBlocks;
import java.util.Set;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.registries.RegistryObject;

public class OBBlockLootSubProvider extends BlockLootSubProvider {

    public OBBlockLootSubProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        OBBlocks.all().forEach(entry -> {
            final Block block = entry.get();
            if (block instanceof OBElevatorBlock) {
                final var isWhite = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(OBElevatorBlock.COLOR, DyeColor.WHITE));
                add(block, createSingleItemTable(block).apply(CopyBlockState.copyState(block).copy(OBElevatorBlock.COLOR)
                        .when(isWhite.invert())));
            } else if (block instanceof OBRopeLadderBlock) {
                final var recoverable = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(OBRopeLadderBlock.RECOVERABLE, true));
                add(block, LootTable.lootTable().withPool(applyExplosionCondition(block,
                        LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(block).when(recoverable)))));
            } else {
                dropSelf(block);
            }
        });
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return OBBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
