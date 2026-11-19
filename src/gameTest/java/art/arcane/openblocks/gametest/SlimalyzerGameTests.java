package art.arcane.openblocks.gametest;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.item.OBSlimalyzerItem;
import art.arcane.openblocks.registry.OBItems;
import art.arcane.openblocks.registry.OBSounds;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public final class SlimalyzerGameTests {

    @GameTest(template = "empty")
    public static void slimalyzerKeepsItsLegacyRecipe(final GameTestHelper helper) {
        final var grid = ComponentRecipeGameTests.input(helper, Items.IRON_INGOT, Items.GLASS_PANE, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.SLIME_BALL, Items.IRON_INGOT, Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT);
        final var result = ComponentRecipeGameTests.craft(helper, grid);
        helper.assertTrue(result.is(OBItems.SLIMALYZER.get()) && !OBSlimalyzerItem.isActive(result),
                "The original recipe did not make one inactive Slimalyzer");
        grid.setItem(1, new ItemStack(Items.GLASS));
        helper.assertTrue(ComponentRecipeGameTests.craft(helper, grid).isEmpty(), "The recipe accepted a glass block in place of a pane");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void slimalyzerPingsOnEnteringAChunkFromInventoryAndOffhand(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final Vec3[] positions = border(level.getSeed());
        final var ordinary = positions[0];
        final var slime = positions[1];
        final var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "SlimalyzerQA"));
        final var stack = new ItemStack(OBItems.SLIMALYZER.get(), 2);
        stack.setHoverName(Component.literal("Field detector"));
        player.getInventory().setItem(7, stack);
        final var signals = new Signals();
        MinecraftForge.EVENT_BUS.register(signals);
        try {
            player.setPos(ordinary);
            player.getInventory().tick();
            helper.assertTrue(!OBSlimalyzerItem.isActive(stack) && signals.positions.isEmpty(), "An ordinary chunk activated the Slimalyzer");
            player.setPos(slime);
            for (int i = 0; i < 4; i++) player.getInventory().tick();
            helper.assertTrue(OBSlimalyzerItem.isActive(stack) && signals.positions.equals(List.of(slime)),
                    "An unselected stack must activate and ping just once on crossing the slime chunk border");
            player.setItemInHand(InteractionHand.OFF_HAND, player.getInventory().removeItemNoUpdate(7));
            player.setPos(ordinary);
            player.getInventory().tick();
            helper.assertTrue(!OBSlimalyzerItem.isActive(stack) && signals.positions.size() == 1 && stack.getCount() == 2 && stack.hasCustomHoverName(),
                    "Leaving the slime chunk must reset the signal silently while preserving the stack and its name");
            player.setPos(slime.x, -20, slime.z);
            player.getInventory().tick();
            helper.assertTrue(OBSlimalyzerItem.isActive(stack) && signals.positions.equals(List.of(slime, new Vec3(slime.x, -20, slime.z))),
                    "Re-entering the same slime chunk in the offhand must ping once below ground as well");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(signals);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void slimalyzerDroppedModelUpdatesAndSurvivesEntityReload(final GameTestHelper helper) {
        final var level = helper.getLevel();
        final Vec3[] positions = border(level.getSeed());
        final var ordinary = positions[0];
        final var slime = positions[1];
        final var dropped = new ItemEntity(level, slime.x, slime.y, slime.z, new ItemStack(OBItems.SLIMALYZER.get()));
        dropped.getEntityData().packDirty();
        final var signals = new Signals();
        MinecraftForge.EVENT_BUS.register(signals);
        try {
            helper.assertTrue(!dropped.getItem().onEntityItemUpdate(dropped) && OBSlimalyzerItem.isActive(dropped.getItem())
                    && dropped.getEntityData().isDirty(), "A dropped Slimalyzer did not synchronize the lit model or blocked normal item ticks");
            dropped.getEntityData().packDirty();
            dropped.getItem().onEntityItemUpdate(dropped);
            helper.assertTrue(!dropped.getEntityData().isDirty() && signals.positions.equals(List.of(slime)),
                    "A stationary drop resent its item data or replayed its ping");

            final var restored = new ItemEntity(EntityType.ITEM, level);
            restored.load(dropped.saveWithoutId(new CompoundTag()));
            restored.getItem().onEntityItemUpdate(restored);
            helper.assertTrue(OBSlimalyzerItem.isActive(restored.getItem()) && signals.positions.size() == 1,
                    "Reloading a lit drop reset its state or replayed the ping");
            restored.getEntityData().packDirty();
            restored.setPos(ordinary);
            restored.getItem().onEntityItemUpdate(restored);
            helper.assertTrue(!OBSlimalyzerItem.isActive(restored.getItem()) && restored.getEntityData().isDirty() && signals.positions.size() == 1,
                    "Moving the reloaded drop out of the chunk failed to synchronize the off model silently");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(signals);
        }
        helper.succeed();
    }

    private static Vec3[] border(final long seed) {
        for (int x = -256; x < 0; x++) {
            final var left = new ChunkPos(x, -3);
            final var right = new ChunkPos(x + 1, -3);
            final boolean activeLeft = WorldgenRandom.seedSlimeChunk(left.x, left.z, seed, 987234911L).nextInt(10) == 0;
            final boolean activeRight = WorldgenRandom.seedSlimeChunk(right.x, right.z, seed, 987234911L).nextInt(10) == 0;
            if (activeLeft == activeRight) continue;
            final var west = Vec3.atCenterOf(new BlockPos(left.getMaxBlockX(), 72, left.getMiddleBlockZ()));
            final var east = west.add(1, 0, 0);
            return activeLeft ? new Vec3[]{east, west} : new Vec3[]{west, east};
        }
        throw new IllegalStateException("The GameTest seed did not have a slime chunk boundary in the sample");
    }

    public static final class Signals {
        private final List<Vec3> positions = new ArrayList<>();

        @SubscribeEvent
        public void record(final PlayLevelSoundEvent.AtPosition event) {
            if (event.getSound() != null && event.getSound().value() == OBSounds.SLIMALYZER_SIGNAL.get()
                    && event.getSource() == SoundSource.PLAYERS && event.getOriginalPitch() == 1 && event.getOriginalVolume() == 1) {
                positions.add(event.getPosition());
            }
        }
    }
}
