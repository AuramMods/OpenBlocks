package art.arcane.openblocks.integration;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class OBOptionalMixins implements IMixinConfigPlugin {
    @Override
    public List<String> getMixins() {
        return LoadingModList.get().getModFileById("flansmod") != null
                ? List.of("OBFlanBulletMixin", "OBFlanHitscanMixin")
                : List.of();
    }

    @Override public void onLoad(final String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) { return true; }
    @Override public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {}
    @Override public void preApply(final String targetClassName, final ClassNode target, final String mixinName, final IMixinInfo info) {}
    @Override public void postApply(final String targetClassName, final ClassNode target, final String mixinName, final IMixinInfo info) {}
}
