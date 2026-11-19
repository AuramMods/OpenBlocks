package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.entity.OBPaintCanBlockEntity;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBMissingMappings;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryManager;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class RegistryMigrationGameTests {

    @GameTest(template = "empty")
    public static void registryRemapsUseRegisteredItemsAndKeepUnknownIdsMissing(final GameTestHelper helper) {
        final var stagingManager = new RegistryManager("STAGING");
        stagingManager.getRegistry(Registries.BLOCK.location(), RegistryManager.ACTIVE);
        final IForgeRegistry<Item> staging = stagingManager.getRegistry(Registries.ITEM.location(), RegistryManager.ACTIVE);
        final var hopper = new RecordedItemMapping(staging, "openblocks:vacuumhopper");
        final var paintCan = new RecordedItemMapping(staging, "open_blocks:paintcan");
        final var control = new RecordedItemMapping(staging, "openblocks:cranecontrol");
        final var unknown = new RecordedItemMapping(staging, "openblocks:unregistered_item");
        final var foreign = new RecordedItemMapping(staging, "example:vacuumhopper");
        OBMissingMappings.onMissingMappings(new MissingMappingsEvent(Registries.ITEM, staging,
                List.of(hopper, paintCan, control, unknown, foreign)));

        helper.assertTrue(hopper.target == OBItems.VACUUM_HOPPER.get() && paintCan.target == OBItems.PAINT_CAN.get()
                && control.target == OBItems.CRANE_CONTROL.get(), "Saved item names did not remap to the active registry objects");
        helper.assertTrue(unknown.target == null && foreign.target == null, "An unsupported item ID was assigned a replacement");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aliasedBlockEntityIdLoadsItsOriginalData(final GameTestHelper helper) {
        final var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, OBBlocks.PAINT_CAN.get());
        final var location = helper.absolutePos(pos);
        final var chunk = helper.getLevel().getChunkAt(location);
        final var paint = new CompoundTag();
        paint.putString("id", "OpenBlocks:paintCan");
        paint.putInt("color", 0x12A4D8);
        paint.putInt("amount", 17);
        final var unknown = new CompoundTag();
        unknown.putString("id", "openblocks:unregistered_block_entity");
        final var entries = new ListTag();
        entries.add(paint);
        entries.add(unknown);
        final var saved = new CompoundTag();
        saved.put("block_entities", entries);
        OBMissingMappings.onChunkDataLoad(new ChunkDataEvent.Load(chunk, saved, ChunkStatus.ChunkType.LEVELCHUNK));

        final var restored = BlockEntity.loadStatic(location, helper.getBlockState(pos), paint);
        helper.assertTrue(restored instanceof OBPaintCanBlockEntity can && can.color() == 0x12A4D8 && can.amount() == 17,
                "The renamed type did not load its existing paint data");
        helper.assertTrue(unknown.getString("id").equals("openblocks:unregistered_block_entity"), "An unsupported type was rewritten");
        helper.succeed();
    }

    private static final class RecordedItemMapping extends MissingMappingsEvent.Mapping<Item> {
        private Item target;

        private RecordedItemMapping(final IForgeRegistry<Item> staging, final String id) {
            super(staging, ForgeRegistries.ITEMS, ResourceLocation.parse(id), 0);
        }

        @Override
        public void remap(final Item replacement) {
            super.remap(replacement);
            target = replacement;
        }
    }
}
