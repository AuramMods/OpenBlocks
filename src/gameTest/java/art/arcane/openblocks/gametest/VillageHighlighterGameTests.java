package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBVillageHighlighterBlock;
import art.arcane.openblocks.block.entity.OBVillageHighlighterBlockEntity;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class VillageHighlighterGameTests {

    @GameTest(template = "empty")
    public static void highlighterCraftsWithEmeraldPlanksAndCobblestone(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.OAK_PLANKS, Items.BIRCH_PLANKS, Items.SPRUCE_PLANKS,
                Items.OAK_PLANKS, Items.EMERALD, Items.BIRCH_PLANKS, Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE);
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).is(OBItems.VILLAGE_HIGHLIGHTER.get()), "The legacy recipe did not craft the highlighter");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 110)
    public static void highlighterReportsBedsAndSyncsThePoweredVillageSurvey(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2)).above(128);
        final BlockPos claimed = pos.north(3);
        final BlockPos spare = pos.south(3);
        final BlockPos lamp = pos.east();
        final BlockPos input = pos.west();
        level.setBlockAndUpdate(pos, OBBlocks.VILLAGE_HIGHLIGHTER.get().defaultBlockState());
        level.setBlockAndUpdate(lamp, Blocks.REDSTONE_LAMP.defaultBlockState());
        placeBed(helper, claimed);
        placeBed(helper, spare);
        final var highlighter = (OBVillageHighlighterBlockEntity) level.getBlockEntity(pos);

        helper.runAtTickTime(12, () -> {
            helper.assertTrue(!level.isVillage(pos) && highlighter.signal() == 0, "Vacant beds by themselves created a village");
            claim(helper, claimed);
        });
        helper.runAtTickTime(24, () -> {
            helper.assertTrue(level.isVillage(pos) && highlighter.signal() == 15
                    && level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "The spare bed did not power the lamp");
            helper.assertTrue(!highlighter.getBlockState().getValue(OBVillageHighlighterBlock.POWERED) && highlighter.survey().boundary().isEmpty(),
                    "The highlighter mistook its own signal for overlay power");
            final var adult = EntityType.VILLAGER.create(level);
            adult.setNoAi(true);
            adult.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
            adult.setNoGravity(true);
            level.addFreshEntity(adult);
            level.setBlockAndUpdate(input, Blocks.REDSTONE_BLOCK.defaultBlockState());
        });
        helper.runAtTickTime(37, () -> {
            final var actual = highlighter.survey();
            helper.assertTrue(highlighter.getBlockState().getValue(OBVillageHighlighterBlock.POWERED) && !actual.boundary().isEmpty(),
                    "Providing power did not expose the village boundary");
            helper.assertTrue(actual.golemOrigins().contains(pos.above()), "The survey omitted the adult villager's golem search origin");
            for (final var face : actual.boundary()) {
                helper.assertTrue(level.isVillage(face.section()), "The survey marked an area outside the vanilla village");
                for (final Direction side : Direction.values()) {
                    final SectionPos adjacent = SectionPos.of(face.section().x() + side.getStepX(), face.section().y() + side.getStepY(),
                            face.section().z() + side.getStepZ());
                    helper.assertTrue(face.exposed(side) != level.isVillage(adjacent), "The surveyed boundary differed from vanilla's village test");
                }
            }
            final var mirror = new OBVillageHighlighterBlockEntity(pos, highlighter.getBlockState());
            mirror.handleUpdateTag(highlighter.getUpdateTag());
            helper.assertTrue(mirror.survey().equals(actual), "The update tag lost survey or redstone information");
            mirror.load(highlighter.saveWithoutMetadata());
            helper.assertTrue(mirror.survey().boundary().isEmpty() && mirror.signal() == 0, "Transient survey data was saved with the block");
            claim(helper, spare);
        });
        helper.runAtTickTime(54, () -> {
            helper.assertTrue(highlighter.signal() == 0 && !level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT),
                    "Claiming the last spare bed did not turn off the output");
            level.getPoiManager().release(spare);
            level.removeBlock(input, false);
        });
        helper.runAtTickTime(70, () -> {
            helper.assertTrue(highlighter.signal() == 15 && highlighter.survey().boundary().isEmpty()
                    && !highlighter.getBlockState().getValue(OBVillageHighlighterBlock.POWERED), "Unpowering the overlay changed the independent bed signal");
            level.getPoiManager().release(claimed);
        });
        helper.runAtTickTime(87, () -> {
            helper.assertTrue(highlighter.signal() == 0 && !level.isVillage(pos), "Releasing the final claimed bed did not clear the village");
            level.getEntities(EntityType.VILLAGER, new AABB(pos).inflate(2), villager -> true).forEach(villager -> villager.discard());
            level.removeBlock(pos, false);
            level.removeBlock(lamp, false);
            removeBed(helper, claimed);
            removeBed(helper, spare);
            helper.succeed();
        });
    }

    private static void claim(final GameTestHelper helper, final BlockPos head) {
        final var found = helper.getLevel().getPoiManager().take(type -> type.is(PoiTypes.HOME), (type, pos) -> pos.equals(head), head, 0);
        helper.assertTrue(found.isPresent(), "The expected bed was not available to claim");
    }

    private static void placeBed(final GameTestHelper helper, final BlockPos head) {
        final var state = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        helper.getLevel().setBlock(head, state.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(head.south(), state.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
    }

    private static void removeBed(final GameTestHelper helper, final BlockPos head) {
        helper.getLevel().removeBlock(head, false);
        helper.getLevel().removeBlock(head.south(), false);
    }
}
