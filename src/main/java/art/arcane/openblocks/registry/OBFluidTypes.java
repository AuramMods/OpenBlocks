package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBFluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, OpenBlocks.MODID);

    public static final RegistryObject<FluidType> XP_JUICE_TYPE = FLUID_TYPES.register("xpjuice",
            () -> new OBFluidType(FluidType.Properties.create()));

    private OBFluidTypes() {}

    private static final class OBFluidType extends FluidType {
        private OBFluidType(final Properties properties) {
            super(properties);
        }
    }
}
