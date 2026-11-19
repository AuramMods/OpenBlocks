package art.arcane.openblocks.item;

import art.arcane.openblocks.config.OBServerConfig;
import art.arcane.openblocks.player.OBExperience;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.Event;

public final class OBCursorItem extends Item {

    public OBCursorItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (player == null || player.isSpectator() || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            final CompoundTag tag = context.getItemInHand().getOrCreateTag();
            tag.putInt("X", pos.getX());
            tag.putInt("Y", pos.getY());
            tag.putInt("Z", pos.getZ());
            tag.putString("dimension", level.dimension().location().toString());
            tag.putInt("side", context.getClickedFace().get3DDataValue());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) activate(level, player, stack.getTag());
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void activate(final Level level, final Player player, final CompoundTag tag) {
        if (tag == null || !tag.contains("X", Tag.TAG_ANY_NUMERIC) || !tag.contains("Y", Tag.TAG_ANY_NUMERIC)
                || !tag.contains("Z", Tag.TAG_ANY_NUMERIC) || !level.dimension().location().equals(dimension(tag))) return;

        final BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        final double distanceSquared = player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
        if (distanceSquared >= OBServerConfig.CURSOR_DISTANCE_SQUARED.get() || !level.hasChunkAt(pos)
                || !level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)) return;

        final var state = level.getBlockState(pos);
        if (state.isAir() || !state.getBlock().isEnabled(level.enabledFeatures())) return;
        final int cost = player.isCreative() ? 0 : (int) Math.max(0, distanceSquared - 10);
        if (cost > OBExperience.points(player)) return;

        final int side = tag.contains("side", Tag.TAG_ANY_NUMERIC) ? tag.getInt("side") : -1;
        final Direction face = side >= 0 && side < Direction.values().length ? Direction.from3DDataValue(side) : Direction.UP;
        final var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
        final var event = ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY || level.getBlockState(pos) != state) return;

        state.use(level, player, InteractionHand.MAIN_HAND, hit);
        if (cost > 0) player.giveExperiencePoints(-cost);
    }

    private static ResourceLocation dimension(final CompoundTag tag) {
        if (tag.contains("dimension", Tag.TAG_STRING)) return ResourceLocation.tryParse(tag.getString("dimension"));
        if (!tag.contains("dimension", Tag.TAG_ANY_NUMERIC)) return null;
        return switch (tag.getInt("dimension")) {
            case -1 -> Level.NETHER.location();
            case 0 -> Level.OVERWORLD.location();
            case 1 -> Level.END.location();
            default -> null;
        };
    }
}
