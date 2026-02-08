package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBFluids {

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, OpenBlocks.MODID);
    public static final DeferredRegister<Block> FLUID_BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OpenBlocks.MODID);

    public static final RegistryObject<FlowingFluid> XP_JUICE = FLUIDS.register("xpjuice", () -> new ForgeFlowingFluid.Source(FluidProps.XP_JUICE));
    public static final RegistryObject<FlowingFluid> FLOWING_XP_JUICE = FLUIDS.register("flowing_xpjuice", () -> new ForgeFlowingFluid.Flowing(FluidProps.XP_JUICE));
    public static final RegistryObject<LiquidBlock> XP_JUICE_BLOCK = FLUID_BLOCKS.register("xpjuice",
            () -> new LiquidBlock(XP_JUICE, BlockBehaviour.Properties.copy(Blocks.WATER)));

    private OBFluids() {}

    private static final class FluidProps {
        private static final ForgeFlowingFluid.Properties XP_JUICE = new ForgeFlowingFluid.Properties(
                OBFluidTypes.XP_JUICE_TYPE,
                OBFluids.XP_JUICE,
                OBFluids.FLOWING_XP_JUICE)
                        .bucket(OBItems.XP_BUCKET)
                        .block(OBFluids.XP_JUICE_BLOCK);
    }
}
