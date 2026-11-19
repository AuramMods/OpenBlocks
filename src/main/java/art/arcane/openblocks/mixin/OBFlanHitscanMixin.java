package art.arcane.openblocks.mixin;

import art.arcane.openblocks.integration.OBFlanTargetHits;
import com.flansmod.common.actions.contexts.ActionGroupContext;
import com.flansmod.common.actions.contexts.GunshotContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunshotContext.class, remap = false)
public abstract class OBFlanHitscanMixin {
    @Shadow @Final public boolean IsProjectile;
    @Shadow @Final public ActionGroupContext ActionGroup;

    @Inject(method = "ProcessImpact", at = @At("HEAD"), remap = false)
    private void openBlocks$scoreHitscan(final HitResult hit, final CallbackInfo callback) {
        if (!IsProjectile && hit instanceof BlockHitResult blockHit && ActionGroup.Gun.GetLevel() instanceof ServerLevel level) {
            OBFlanTargetHits.recordHitscan(level, blockHit, ActionGroup.Gun.GetShootOrigin().positionVec3(),
                    ActionGroup.Gun.GetShooter().Entity());
        }
    }
}
