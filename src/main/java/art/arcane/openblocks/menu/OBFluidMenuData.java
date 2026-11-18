package art.arcane.openblocks.menu;

import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerData;
import net.minecraftforge.fluids.FluidStack;

final class OBFluidMenuData {

    private static final int VOLUME = 0;
    private static final int CAPACITY = 2;
    private static final int FLUID = 4;
    static final int VALUES = 6;

    private OBFluidMenuData() {}

    static ContainerData of(final Supplier<FluidStack> contents, final IntSupplier capacity, final int extraCount,
            final IntUnaryOperator extra) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                return switch (index) {
                    case VOLUME, VOLUME + 1 -> word(contents.get().getAmount(), index - VOLUME);
                    case CAPACITY, CAPACITY + 1 -> word(capacity.getAsInt(), index - CAPACITY);
                    case FLUID, FLUID + 1 -> word(BuiltInRegistries.FLUID.getId(contents.get().getFluid()), index - FLUID);
                    default -> extra.applyAsInt(index - VALUES);
                };
            }

            @Override
            public void set(final int index, final int value) {}

            @Override
            public int getCount() {
                return VALUES + extraCount;
            }
        };
    }

    static int capacity(final ContainerData data) {
        return readInt(data, CAPACITY);
    }

    static FluidStack fluid(final ContainerData data) {
        final int amount = readInt(data, VOLUME);
        final var fluid = BuiltInRegistries.FLUID.byId(readInt(data, FLUID));
        return fluid == null || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    // The vanilla menu protocol carries signed 16-bit values.
    private static int word(final int value, final int index) {
        return (short) (value >>> (index * Short.SIZE));
    }

    private static int readInt(final ContainerData data, final int first) {
        return (data.get(first) & 0xffff) | (data.get(first + 1) << Short.SIZE);
    }
}
