package art.arcane.openblocks.block.entity;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class OBPlaceholderBlockEntity extends BlockEntity {

    public OBPlaceholderBlockEntity(final Supplier<? extends BlockEntityType<?>> type, final BlockPos pos, final BlockState state) {
        super(type.get(), pos, state);
    }
}
