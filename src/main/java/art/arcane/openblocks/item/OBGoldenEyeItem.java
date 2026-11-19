package art.arcane.openblocks.item;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.registry.OBEntities;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class OBGoldenEyeItem extends Item {

    public static final int MAX_CHARGES = 100;
    public static final TagKey<Structure> TARGETS = TagKey.create(Registries.STRUCTURE, OpenBlocks.id("golden_eye_targets"));
    private static final int SEARCH_RADIUS = 100;

    public OBGoldenEyeItem() {
        super(new Properties().durability(MAX_CHARGES));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);

        if (player.isShiftKeyDown()) {
            learnStructure(server, player, stack);
            return InteractionResultHolder.consume(stack);
        }
        if (stack.getDamageValue() >= MAX_CHARGES) {
            player.displayClientMessage(Component.translatable("item.open_blocks.golden_eye.depleted"), true);
            return InteractionResultHolder.fail(stack);
        }
        final var tag = stack.getTag();
        final BlockPos target = target(tag);
        if (target == null) {
            player.displayClientMessage(Component.translatable("item.open_blocks.golden_eye.unlocked"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (tag.contains("Dimension", Tag.TAG_STRING) && !tag.getString("Dimension").equals(level.dimension().location().toString())) {
            player.displayClientMessage(Component.translatable("item.open_blocks.golden_eye.other_dimension"), true);
            return InteractionResultHolder.fail(stack);
        }

        final var projectile = OBEntities.GOLDEN_EYE.get().create(server);
        if (projectile == null) return InteractionResultHolder.fail(stack);
        final var flyingStack = stack.copyWithCount(1);
        flyingStack.setDamageValue(stack.getDamageValue() + 1);
        projectile.launch(player, target, flyingStack);
        if (!server.addFreshEntity(projectile)) return InteractionResultHolder.fail(stack);

        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL,
                0.5F, 0.4F / (server.random.nextFloat() * 0.4F + 0.8F));
        if (stack.getCount() == 1) return InteractionResultHolder.consume(ItemStack.EMPTY);
        if (player.getAbilities().instabuild) {
            // Earlier builds could stack Eyes. Update that stack directly so Creative does not restore the thrown count.
            player.setItemInHand(hand, stack.copyWithCount(stack.getCount() - 1));
        } else {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static void learnStructure(final ServerLevel level, final Player player, final ItemStack stack) {
        final var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        final var targets = structures.getTag(TARGETS).orElse(null);
        final var found = targets == null || !level.structureManager().shouldGenerateStructures() ? null
                : level.getChunkSource().getGenerator().findNearestMapStructure(level, targets, player.blockPosition(), SEARCH_RADIUS, false);
        if (found == null) {
            player.displayClientMessage(Component.translatable("item.open_blocks.golden_eye.no_structures"), true);
            return;
        }

        final var pos = found.getFirst();
        final ResourceLocation id = structures.getKey(found.getSecond().value());
        final var tag = stack.getOrCreateTag();
        tag.putString("Structure", id.toString());
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putString("Dimension", level.dimension().location().toString());
        player.displayClientMessage(Component.translatable("item.open_blocks.golden_eye.learned", structureName(id.toString())), true);
        if (OBServerConfig.GOLDEN_EYE_DEBUG.get()) {
            player.sendSystemMessage(Component.translatable("item.open_blocks.golden_eye.debug", id.toString(),
                    pos.getX(), pos.getY(), pos.getZ()));
        }
    }

    @Nullable
    private static BlockPos target(@Nullable final CompoundTag tag) {
        if (tag == null || !tag.contains("X", Tag.TAG_ANY_NUMERIC) || !tag.contains("Y", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("Z", Tag.TAG_ANY_NUMERIC)) return null;
        return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
    }

    private static Component structureName(final String name) {
        final int colon = name.indexOf(':');
        final String key = colon < 0 ? "legacy." + name.toLowerCase(Locale.ROOT) : name.replace(':', '.');
        final String label = name.substring(colon + 1).replace('_', ' ').replace('/', ' ');
        return Component.translatableWithFallback("item.open_blocks.golden_eye.structure." + key, label);
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> lines, final TooltipFlag flag) {
        lines.add(Component.translatable("item.open_blocks.golden_eye.charges",
                Math.max(0, MAX_CHARGES - stack.getDamageValue()), MAX_CHARGES));
        final var tag = stack.getTag();
        if (tag != null && tag.contains("Structure", Tag.TAG_STRING)) {
            lines.add(Component.translatable("item.open_blocks.golden_eye.locked", structureName(tag.getString("Structure"))));
        }
    }
}
