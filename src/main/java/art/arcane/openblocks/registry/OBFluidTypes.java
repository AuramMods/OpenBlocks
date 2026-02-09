package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBFluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, OpenBlocks.MODID);

    private static final ResourceLocation XP_JUICE_STILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "block/xp_juice_still");
    private static final ResourceLocation XP_JUICE_FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, "block/xp_juice_flowing");
    private static final int XP_JUICE_TINT = 0xFFFFFFFF;

    public static final RegistryObject<FluidType> XP_JUICE_TYPE = FLUID_TYPES.register("xpjuice",
            () -> new OBFluidType(
                    FluidType.Properties.create(),
                    XP_JUICE_STILL_TEXTURE,
                    XP_JUICE_FLOWING_TEXTURE,
                    XP_JUICE_TINT));

    private OBFluidTypes() {}

    private static final class OBFluidType extends FluidType {
        private final ResourceLocation stillTexture;
        private final ResourceLocation flowingTexture;
        private final int tintColor;

        private OBFluidType(
                final Properties properties,
                final ResourceLocation stillTexture,
                final ResourceLocation flowingTexture,
                final int tintColor) {
            super(properties);
            this.stillTexture = stillTexture;
            this.flowingTexture = flowingTexture;
            this.tintColor = tintColor;
        }

        @Override
        public void initializeClient(final Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return stillTexture;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return flowingTexture;
                }

                @Override
                public int getTintColor() {
                    return tintColor;
                }
            });
        }
    }
}
