package art.arcane.openblocks.mixin;

import art.arcane.openblocks.block.OBFoldingTargetBlock;
import com.flansmod.common.projectiles.BulletEntity;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BulletEntity.class, remap = false)
public abstract class OBFlanBulletMixin {
    @Redirect(method = "OnImpact", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getHitResultOnMoveVector(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult openBlocks$scorePhysicalHit(final Entity bullet, final Predicate<Entity> canHit) {
        final HitResult hit = ProjectileUtil.getHitResultOnMoveVector(bullet, canHit);
        if (bullet.level() instanceof ServerLevel level && bullet.getDeltaMovement().lengthSqr() > 0) {
            OBFoldingTargetBlock.recordOtherProjectile(level, hit);
        }
        return hit;
    }
}
