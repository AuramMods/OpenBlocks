package art.arcane.openblocks.item;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBBigButtonBlock;
import art.arcane.openblocks.block.OBGuideBlock;
import art.arcane.openblocks.block.OBLadderBlock;
import art.arcane.openblocks.registry.OBBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.event.level.BlockEvent.BlockToolModificationEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class OBWrenchItem extends Item {

    public static final ToolAction ROTATE = ToolAction.get("wrench_rotate");

    public OBWrenchItem() {
        super(new Properties().stacksTo(1));
    }

    @Override public boolean canPerformAction(final ItemStack stack, final ToolAction action) { return action == ROTATE; }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final LevelReader level, final BlockPos pos, final Player player) {
        return true;
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final var player = context.getPlayer();
        final var level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final Direction face = context.getClickedFace();
        if (player == null || player.isSpectator() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)) {
            return InteractionResult.FAIL;
        }

        final BlockState state = level.getBlockState(pos);
        final BlockState proposed = rotatedState(level, pos, state, face, player.isShiftKeyDown());
        if (proposed == state) return InteractionResult.FAIL;
        if (level.isClientSide()) return proposed.canSurvive(level, pos) ? InteractionResult.SUCCESS : InteractionResult.FAIL;

        final var event = new BlockToolModificationEvent(state, context, ROTATE, false);
        event.setFinalState(proposed);
        if (MinecraftForge.EVENT_BUS.post(event) || level.getBlockState(pos) != state) return InteractionResult.FAIL;
        final BlockState rotated = event.getFinalState();
        if (rotated == null || rotated == state || !rotated.is(state.getBlock()) || !rotated.canSurvive(level, pos)
                || !level.setBlockAndUpdate(pos, rotated)) return InteractionResult.FAIL;

        notifyPoweredAttachment(level, pos, state, level.getBlockState(pos));
        return InteractionResult.CONSUME;
    }

    private static BlockState rotatedState(final Level level, final BlockPos pos, final BlockState state,
            final Direction face, final boolean sneaking) {
        final var block = state.getBlock();
        if ((block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof ChestBlock) && !sneaking) return state;
        if (block instanceof BedBlock || block instanceof PistonHeadBlock || block instanceof MovingPistonBlock
                || block instanceof PistonBaseBlock && state.getValue(PistonBaseBlock.EXTENDED)
                || block instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                || state.is(OBBlocks.FLAG.get()) || state.is(OBBlocks.ROPE_LADDER.get()) || state.is(OBBlocks.GRAVE.get())) return state;

        if (block instanceof OBGuideBlock) {
            final var orientation = state.getValue(OBGuideBlock.ORIENTATION);
            return state.setValue(OBGuideBlock.ORIENTATION, orientation.wrenchRotation(face));
        }

        final var blockId = ForgeRegistries.BLOCKS.getKey(block);
        final boolean openBlocks = blockId != null && blockId.getNamespace().equals(OpenBlocks.MODID) && !(block instanceof OBLadderBlock);
        if (openBlocks && state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            final Direction.Axis current = state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            final Direction.Axis next = face.getAxis().isHorizontal() ? face.getAxis() : current == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            return state.setValue(BlockStateProperties.HORIZONTAL_AXIS, next);
        }

        for (final Property<?> property : state.getProperties()) {
            if (property.getValueClass() == Direction.class && (property.getName().equals("facing") || property.getName().equals("rotation"))) {
                @SuppressWarnings("unchecked") final Property<Direction> facing = (Property<Direction>) property;
                return rotateFacing(level, pos, state, face, facing, openBlocks);
            }
        }
        return state;
    }

    private static BlockState rotateFacing(final Level level, final BlockPos pos, final BlockState state, final Direction face,
            final Property<Direction> facing, final boolean openBlocks) {
        final var allowed = facing.getPossibleValues();
        final Direction current = state.getValue(facing);
        final boolean horizontal = !allowed.contains(Direction.UP) && !allowed.contains(Direction.DOWN);
        final Direction next;
        if (openBlocks) {
            if (horizontal && face.getAxis() == Direction.Axis.Y) {
                next = face == Direction.UP ? current.getClockWise() : current.getCounterClockWise();
            } else {
                next = current == face ? face.getOpposite() : face;
            }
        } else {
            if (!allowed.contains(face)) return state;
            next = horizontal ? current.getClockWise() : current.getClockWise(face.getAxis());
        }
        if (!allowed.contains(next)) return openBlocks ? state : state.cycle(facing);
        if (next == current) return state;

        if (current.getAxis().isHorizontal() && next.getAxis().isHorizontal()) {
            final Rotation rotation = next == current.getClockWise() ? Rotation.CLOCKWISE_90
                    : next == current.getOpposite() ? Rotation.CLOCKWISE_180 : Rotation.COUNTERCLOCKWISE_90;
            final var rotated = state.rotate(level, pos, rotation);
            if (rotated.is(state.getBlock()) && rotated.getValue(facing) == next) return rotated;
        }
        return state.setValue(facing, next);
    }

    private static void notifyPoweredAttachment(final Level level, final BlockPos pos, final BlockState previous, final BlockState current) {
        final var block = previous.getBlock();
        if (!(block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof OBBigButtonBlock)
                || !current.is(block) || !previous.getValue(BlockStateProperties.POWERED)) return;
        final Direction oldSupport = supportDirection(previous);
        final Direction newSupport = supportDirection(current);
        if (oldSupport != newSupport) {
            level.updateNeighborsAt(pos.relative(oldSupport), block);
            level.updateNeighborsAt(pos.relative(newSupport), block);
        }
    }

    private static Direction supportDirection(final BlockState state) {
        if (state.getBlock() instanceof OBBigButtonBlock) return state.getValue(OBBigButtonBlock.FACING).getOpposite();
        return switch (state.getValue(BlockStateProperties.ATTACH_FACE)) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        };
    }
}
