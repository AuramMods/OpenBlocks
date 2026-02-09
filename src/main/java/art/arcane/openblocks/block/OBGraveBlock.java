package art.arcane.openblocks.block;

import art.arcane.openblocks.block.entity.OBGraveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OBGraveBlock extends OBShapeBlock implements EntityBlock {

    public OBGraveBlock(
            final BlockBehaviour.Properties properties,
            final VoxelShape shape,
            final VoxelShape collisionShape) {
        super(properties, shape, collisionShape);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new OBGraveBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof OBGraveBlockEntity graveBlockEntity)) return InteractionResult.PASS;

        graveBlockEntity.claimLoot(serverPlayer);
        return InteractionResult.CONSUME;
    }
}
