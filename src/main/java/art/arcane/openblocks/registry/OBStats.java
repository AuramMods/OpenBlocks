package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class OBStats {

    public static final DeferredRegister<ResourceLocation> CUSTOM_STATS = DeferredRegister.create(Registries.CUSTOM_STAT, OpenBlocks.MODID);
    public static final RegistryObject<ResourceLocation> BRICKS_DROPPED = CUSTOM_STATS.register("bricks_dropped",
            () -> OpenBlocks.id("bricks_dropped"));

    private OBStats() {}
}
