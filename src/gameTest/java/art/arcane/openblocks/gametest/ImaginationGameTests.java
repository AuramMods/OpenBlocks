package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBImaginaryBlock.Shape;
import art.arcane.openblocks.block.entity.OBImaginaryBlockEntity;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.item.OBImaginaryItem;
import art.arcane.openblocks.item.OBImaginaryItem.Mode;
import art.arcane.openblocks.item.OBImaginationGlasses;
import art.arcane.openblocks.item.OBPaintColors;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class ImaginationGameTests {

    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final int RED = 0xFF0000;
    private static final int BLUE = 0x0000FF;

    @GameTest(template = "empty")
    public static void originalRecipesCraftPencilsAndSixteenChargedCrayons(final GameTestHelper helper) {
        for (final var fuel : new net.minecraft.world.item.Item[]{Items.COAL, Items.CHARCOAL}) {
            final var grid = ComponentRecipeGameTests.input(helper, fuel, Items.STICK, Items.ENDER_EYE, Items.SLIME_BALL);
            final var output = ComponentRecipeGameTests.craft(helper, grid);
            helper.assertTrue(output.is(OBItems.IMAGINARY.get()) && OBImaginaryItem.color(output) == null
                    && OBImaginaryItem.uses(output) == 10 && output.getMaxStackSize() == 1, "Pencil recipe lost its charges");
        }
        for (final DyeColor dye : DyeColor.values()) {
            final var grid = ComponentRecipeGameTests.input(helper, DyeItem.byColor(dye), Items.PAPER, Items.ENDER_EYE, Items.SLIME_BALL);
            final var output = ComponentRecipeGameTests.craft(helper, grid);
            final var color = OBImaginaryItem.color(output);
            helper.assertTrue(output.is(OBItems.IMAGINARY.get()) && color != null && color == OBPaintColors.palette(dye)
                    && OBImaginaryItem.uses(output) == 10, "Incorrect crayon color or charges for " + dye.getName());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crayonMixingMergingAndGlassesConserveUses(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var grid = ComponentRecipeGameTests.input(helper);
        grid.setItem(0, OBImaginaryItem.create(RED, Mode.STAIRS, 4.5F));
        grid.setItem(1, OBImaginaryItem.create(RED, Mode.STAIRS, 2.5F));
        final var merged = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(OBImaginaryItem.uses(merged) == 7 && OBImaginaryItem.color(merged) == RED
                && OBImaginaryItem.mode(merged) == Mode.BLOCK, "Merge failed to conserve charges and reset placement mode");
        var remainder = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(remainder.stream().allMatch(ItemStack::isEmpty), "Merge returned its consumed crayons");

        grid.setItem(2, OBImaginaryItem.create(BLUE, Mode.PANEL, 1));
        final var mixed = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(OBImaginaryItem.color(mixed) == 0xAA0055 && OBImaginaryItem.mode(mixed) == Mode.STAIRS
                && Math.abs(OBImaginaryItem.uses(mixed) - 2.7F) < 0.0001F, "Mix failed to preserve weighted placement mode or average RGB");
        remainder = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(OBImaginaryItem.uses(remainder.get(0)) == 3.5F && OBImaginaryItem.uses(remainder.get(1)) == 1.5F
                && remainder.get(2).isEmpty(), "Mix did not spend exactly one charge per crayon");

        grid.setItem(1, new ItemStack(Items.PAPER));
        grid.setItem(2, ItemStack.EMPTY);
        var glasses = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(glasses.is(OBItems.CRAYON_GLASSES.get()) && OBImaginationGlasses.color(glasses) == RED,
                "Colored crayons must make matching Crayon Glasses");
        grid.setItem(0, OBImaginaryItem.create(null, Mode.BLOCK, 1));
        glasses = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(glasses.is(OBItems.PENCIL_GLASSES.get()), "Pencils must make Pencil Glasses");
        remainder = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(remainder.get(0).isEmpty(), "The final crafting charge did not consume the pencil");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void glassesSeparateVisibilitySelectionAndCollision(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(POS);
        helper.setBlock(POS, OBBlocks.IMAGINARY.get());
        final var image = (OBImaginaryBlockEntity) level.getBlockEntity(pos);
        final var state = level.getBlockState(pos);
        final var first = player(helper);
        final var second = player(helper);

        helper.assertTrue(!image.visibleTo(first) && !image.selectableBy(first) && image.solidFor(first),
                "Ordinary pencils are invisible and unselectable but still solid");
        first.setItemSlot(EquipmentSlot.HEAD, new ItemStack(OBItems.PENCIL_GLASSES.get()));
        helper.assertTrue(image.visibleTo(first) && !state.getShape(level, pos, CollisionContext.of(first)).isEmpty(),
                "Pencil Glasses did not reveal and select the pencil");

        image.setDesign(RED, Shape.STAIRS, false);
        first.setItemSlot(EquipmentSlot.HEAD, OBImaginationGlasses.createCrayonGlasses(RED));
        second.setItemSlot(EquipmentSlot.HEAD, OBImaginationGlasses.createCrayonGlasses(BLUE));
        helper.assertTrue(image.visibleTo(first) && image.selectableBy(first) && image.solidFor(first)
                && !image.visibleTo(second) && state.getCollisionShape(level, pos, CollisionContext.of(second)).isEmpty(),
                "Exact RGB did not determine visibility, selection and collision independently for two players");
        helper.assertTrue(state.getCollisionShape(level, pos, CollisionContext.of(first)).toAabbs().size() == 2
                && state.getCollisionShape(level, pos, CollisionContext.of(EntityType.PIG.create(level))).isEmpty(),
                "Crayon stairs lost their two steps or collided with non-players");

        second.setItemSlot(EquipmentSlot.HEAD, new ItemStack(OBItems.TECHNICOLOR_GLASSES.get()));
        helper.assertTrue(image.visibleTo(second) && !image.selectableBy(second) && !image.solidFor(second),
                "Technicolor Glasses should reveal ordinary crayons without making them solid or selectable");
        image.setDesign(RED, Shape.PANEL, true);
        helper.assertTrue(!image.visibleTo(first) && !image.solidFor(first) && image.selectableBy(second) && image.solidFor(second),
                "Inverted crayons did not reverse the match");
        second.setItemSlot(EquipmentSlot.HEAD, new ItemStack(OBItems.SERIOUS_GLASSES.get()));
        helper.assertTrue(image.visibleTo(second) && image.selectableBy(second) && image.solidFor(second),
                "Serious Glasses must reveal and select every imaginary shape");
        first.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(image.visibleTo(first), "Spectators could not see inverted drawings of their own glasses color");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void imaginaryPlacementPaysFractionalCostsOnlyOnSuccess(final GameTestHelper helper) {
        final var level = helper.getLevel();
        helper.setBlock(POS.below(), Blocks.STONE);
        helper.setBlock(POS.east().below(), Blocks.STONE);
        final BlockPos pos = helper.absolutePos(POS);
        final var player = player(helper);
        final var crayon = OBImaginaryItem.create(RED, Mode.INVERTED_STAIRS, 1.75F);
        player.setItemInHand(InteractionHand.MAIN_HAND, crayon);

        final Consumer<BlockEvent.EntityPlaceEvent> deny = event -> {
            if (event.getEntity() == player && event.getPos().equals(pos)) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(deny);
        try {
            helper.assertTrue(place(player, pos) == InteractionResult.FAIL && level.isEmptyBlock(pos)
                    && OBImaginaryItem.uses(crayon) == 1.75F, "A denied placement consumed the crayon or left a block");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        helper.assertTrue(place(player, pos).consumesAction() && crayon.getCount() == 1 && OBImaginaryItem.uses(crayon) == 0.5F,
                "An inverted stair should spend one and a quarter charges");
        final var block = (OBImaginaryBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(block.color() == RED && block.inverted() && block.shape() == Shape.STAIRS,
                "Placement did not transfer the crayon design");
        helper.assertTrue(place(player, pos.east()) == InteractionResult.FAIL && level.isEmptyBlock(pos.east()),
                "A crayon with insufficient charges placed another stair");
        player.setItemInHand(InteractionHand.MAIN_HAND, OBImaginaryItem.create(null, Mode.PANEL, 0.5F));
        helper.assertTrue(place(player, pos.east()).consumesAction() && player.getMainHandItem().isEmpty(),
                "A full panel did not consume its last half-charge");
        level.destroyBlock(pos, true);
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).isEmpty(), "A broken drawing dropped a tool");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void imaginaryDataReloadsAndReadsOlderPanelTags(final GameTestHelper helper) {
        helper.setBlock(POS, OBBlocks.IMAGINARY.get());
        final var pos = helper.absolutePos(POS);
        final var block = (OBImaginaryBlockEntity) helper.getLevel().getBlockEntity(pos);
        block.setDesign(0xABCDEF, Shape.HALF_PANEL, true);
        final var state = block.getBlockState();
        final var loaded = (OBImaginaryBlockEntity) BlockEntity.loadStatic(pos, state, block.saveWithFullMetadata());
        helper.assertTrue(loaded != null && loaded.color() == 0xABCDEF && loaded.shape() == Shape.HALF_PANEL && loaded.inverted(),
                "Reload did not preserve shape, color and inversion");
        final CompoundTag old = new CompoundTag();
        old.putByte("Type", (byte) 1);
        old.putFloat("PanelHeight", 0.55F);
        loaded.load(old);
        helper.assertTrue(loaded.color() == null && loaded.shape() == Shape.HALF_PANEL && !loaded.inverted(),
                "Older panel tags did not load as a half-height pencil");
        final var player = player(helper);
        final var picked = state.getCloneItemStack(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false),
                helper.getLevel(), pos, player);
        helper.assertTrue(OBImaginaryItem.color(picked) == 0xABCDEF && OBImaginaryItem.mode(picked) == Mode.INVERTED_HALF_PANEL
                && OBImaginaryItem.uses(picked) == OBImaginaryItem.defaultUses(), "Pick block failed to restore the crayon's design");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void technicolorGlassesAppearInBothOriginalChestTables(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final var original = OBServerConfig.TECHNICOLOR_LOOT.get();
        final var loot = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(POS)))
                .create(LootContextParamSets.CHEST);
        try {
            final var tables = level.getServer().getLootData();
            for (final String chest : new String[]{"abandoned_mineshaft", "simple_dungeon"}) {
                final var table = tables.getLootTable(new ResourceLocation("minecraft", "chests/" + chest));
                OBServerConfig.TECHNICOLOR_LOOT.set(true);
                long seed = 1;
                for (; seed <= 64; seed++) {
                    if (table.getRandomItems(loot, seed).stream().anyMatch(stack -> stack.is(OBItems.TECHNICOLOR_GLASSES.get()))) break;
                }
                helper.assertTrue(seed <= 64, "No Technicolor Glasses were added to " + chest);
                OBServerConfig.TECHNICOLOR_LOOT.set(false);
                final var withoutGlasses = table.getRandomItems(loot, seed);
                helper.assertTrue(!withoutGlasses.isEmpty()
                        && withoutGlasses.stream().noneMatch(stack -> stack.is(OBItems.TECHNICOLOR_GLASSES.get())),
                        "The loot setting failed to disable the glasses or removed vanilla loot");
            }
        } finally {
            OBServerConfig.TECHNICOLOR_LOOT.set(original);
        }
        helper.succeed();
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ImaginationQA"));
        final Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(POS.north(2)));
        player.moveTo(position.x, position.y, position.z, 0, 0);
        return player;
    }

    private static InteractionResult place(final FakePlayer player, final BlockPos pos) {
        final BlockPos ground = pos.below();
        final var hit = new BlockHitResult(Vec3.atCenterOf(ground).relative(Direction.UP, 0.5), Direction.UP, ground, false);
        return player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }
}
