package art.arcane.openblocks.block.entity;

import art.arcane.openblocks.registry.OBBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OBSkyBlockEntity extends BlockEntity {

    public OBSkyBlockEntity(final BlockPos pos, final BlockState state) {
        super(OBBlockEntities.SKY.get(), pos, state);
    }
}
