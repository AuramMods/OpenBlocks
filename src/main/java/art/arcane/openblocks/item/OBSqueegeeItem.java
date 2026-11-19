package art.arcane.openblocks.item;

import art.arcane.openblocks.block.entity.OBCanvasBlockEntity;
import art.arcane.openblocks.registry.OBSounds;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public final class OBSqueegeeItem extends Item {

    public OBSqueegeeItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final var level = context.getLevel();
        final var pos = context.getClickedPos();
        final var player = context.getPlayer();
        final Direction face = context.getClickedFace();
        if (!(level.getBlockEntity(pos) instanceof OBCanvasBlockEntity canvas) || player == null
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, context.getItemInHand())) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (!canvas.rinse(player, face, player.isShiftKeyDown())) return InteractionResult.FAIL;
            level.playSound(null, pos, OBSounds.SQUEEGEE_USE.get(), SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
