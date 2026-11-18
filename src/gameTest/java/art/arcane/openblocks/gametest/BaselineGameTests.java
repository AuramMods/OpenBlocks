package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.api.OBGraveDropsEvent;
import art.arcane.openblocks.block.OBElevatorRotatingBlock;
import art.arcane.openblocks.registry.OBBlocks;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class BaselineGameTests {

    @GameTest(template = "empty")
    public static void canvasRecipeLoadsAndUsesOakFence(GameTestHelper helper) {
        final var recipe = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "legacy/canvas_0"));
        helper.assertTrue(recipe.orElse(null) instanceof ShapedRecipe, "Canvas recipe did not load");
        final var center = recipe.orElseThrow().getIngredients().get(4);
        helper.assertTrue(center.test(new ItemStack(Items.OAK_FENCE)), "Canvas recipe rejects its original oak fence");
        helper.assertTrue(!center.test(new ItemStack(Items.SPRUCE_FENCE)), "Canvas recipe accepts a different fence");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeTagsDistinguishMaterialsAndColors(GameTestHelper helper) {
        final var iron = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "legacy_ore_dict/ingot_iron"));
        final var red = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "legacy_ore_dict/dye_red"));
        helper.assertTrue(new ItemStack(Items.IRON_INGOT).is(iron), "Iron ingredient rejects iron");
        helper.assertTrue(!new ItemStack(Items.GOLD_INGOT).is(iron), "Iron ingredient accepts gold");
        helper.assertTrue(new ItemStack(Items.RED_DYE).is(red), "Red dye ingredient rejects red");
        helper.assertTrue(!new ItemStack(Items.BLUE_DYE).is(red), "Red dye ingredient accepts blue");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mixedElevatorsUseDestinationRotation(GameTestHelper helper) {
        final BlockPos lower = new BlockPos(3, 1, 3);
        final BlockPos upper = lower.above(4);
        helper.setBlock(lower, OBBlocks.ELEVATOR.get());
        helper.setBlock(upper, OBBlocks.ELEVATOR_ROTATING.get().defaultBlockState()
                .setValue(OBElevatorRotatingBlock.FACING, Direction.EAST));
        final Player player = helper.makeMockSurvivalPlayer();
        final Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(lower.above()));
        player.moveTo(start.x, start.y, start.z, 33.0F, 0.0F);

        helper.useBlock(lower, player);
        helper.assertTrue(player.blockPosition().equals(helper.absolutePos(upper.above())), "Did not reach upper elevator");
        helper.assertTrue(player.getYRot() == 90.0F, "Rotating destination did not set yaw");

        player.setShiftKeyDown(true);
        helper.useBlock(upper, player);
        helper.assertTrue(player.blockPosition().equals(helper.absolutePos(lower.above())), "Did not return to ordinary elevator");
        helper.assertTrue(player.getYRot() == 90.0F, "Ordinary destination changed yaw");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spongePreservesWaterloggedBlocks(GameTestHelper helper) {
        final BlockPos sponge = new BlockPos(3, 2, 3);
        final BlockPos fence = sponge.east();
        final BlockPos water = sponge.west();
        helper.setBlock(fence, Blocks.OAK_FENCE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        helper.setBlock(water, Blocks.WATER);
        helper.setBlock(sponge, OBBlocks.SPONGE.get());

        helper.assertBlockPresent(Blocks.OAK_FENCE, fence);
        helper.assertTrue(helper.getBlockState(fence).getFluidState().isEmpty(), "Fence is still waterlogged");
        helper.assertBlockPresent(Blocks.AIR, water);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unplacedGraveReturnsStoredDropsAndHonorsDelete(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "grave_test")) {
            @Override
            public void sendSystemMessage(Component message) {}
        };
        final Vec3 pos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 30, 3)));
        player.setPos(pos.x, pos.y, pos.z);
        final ItemEntity store = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(Items.DIAMOND));
        final ItemEntity drop = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(Items.GOLD_INGOT));
        final ItemEntity delete = new ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(Items.ROTTEN_FLESH));
        final var drops = new ArrayList<>(List.of(store, drop, delete));
        final Consumer<OBGraveDropsEvent> configureDrops = event -> {
            if (event.getPlayer() != player) return;
            for (final var entry : event.getDrops()) {
                if (entry.getItem() == drop) entry.setAction(OBGraveDropsEvent.Action.DROP);
                if (entry.getItem() == delete) entry.setAction(OBGraveDropsEvent.Action.DELETE);
            }
        };

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OBGraveDropsEvent.class, configureDrops);
        try {
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(player, level.damageSources().generic(), drops, 0, false));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(configureDrops);
        }

        helper.assertTrue(drops.size() == 2 && drops.contains(store) && drops.contains(drop),
                "Grave placement failure did not return both recoverable drops while honoring DELETE");
        helper.succeed();
    }
}
