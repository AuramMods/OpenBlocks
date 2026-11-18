package art.arcane.openblocks.item;

import art.arcane.openblocks.block.entity.OBTankBlockEntity;
import art.arcane.openblocks.client.OBTankRenderer;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public final class OBTankItem extends BlockItem {

    public OBTankItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new OBTankRenderer.ItemRenderer();
                return renderer;
            }
        });
    }

    public static FluidStack contents(final ItemStack stack) {
        final CompoundTag tag = stack.getTagElement(OBTankBlockEntity.FLUID_TAG);
        return tag == null ? FluidStack.EMPTY : FluidStack.loadFluidStackFromNBT(tag);
    }

    public static void setContents(final ItemStack stack, final FluidStack fluid) {
        if (fluid.isEmpty()) {
            stack.removeTagKey(OBTankBlockEntity.FLUID_TAG);
        } else {
            stack.addTagElement(OBTankBlockEntity.FLUID_TAG, fluid.writeToNBT(new CompoundTag()));
        }
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip,
            final TooltipFlag flags) {
        final FluidStack contents = contents(stack);
        if (contents.isEmpty()) return;
        final int percent = Math.max(1, Math.round(100.0F * contents.getAmount() / Math.max(OBTankBlockEntity.capacity(), contents.getAmount())));
        tooltip.add(Component.translatable("tooltip.open_blocks.tank.contents", contents.getDisplayName(), contents.getAmount(), percent));
    }

    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable final CompoundTag tag) {
        return new ICapabilityProvider() {
            private final LazyOptional<IFluidHandlerItem> handler = LazyOptional.of(() -> new PortableTank(stack));

            @Override
            public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction side) {
                return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(capability, handler);
            }
        };
    }

    private record PortableTank(ItemStack stack) implements IFluidHandlerItem {

        @Override
        public ItemStack getContainer() {
            return stack;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(final int index) {
            if (index != 0 || stack.isEmpty()) return FluidStack.EMPTY;
            final FluidStack fluid = contents(stack);
            return new FluidStack(fluid, totalAmount(fluid.getAmount()));
        }

        @Override
        public int getTankCapacity(final int index) {
            return index == 0 ? totalAmount(Math.max(OBTankBlockEntity.capacity(), contents(stack).getAmount())) : 0;
        }

        @Override
        public boolean isFluidValid(final int index, final FluidStack fluid) {
            return index == 0;
        }

        @Override
        public int fill(final FluidStack fluid, final FluidAction action) {
            if (stack.isEmpty() || fluid.isEmpty()) return 0;
            final FluidTank tank = readTank();
            final int inserted = tank.fill(new FluidStack(fluid, fluid.getAmount() / stack.getCount()), action);
            if (inserted > 0 && action.execute()) setContents(stack, tank.getFluid());
            return inserted * stack.getCount();
        }

        @Override
        public FluidStack drain(final FluidStack fluid, final FluidAction action) {
            if (fluid.isEmpty() || !fluid.isFluidEqual(contents(stack))) return FluidStack.EMPTY;
            return drain(fluid.getAmount(), action);
        }

        @Override
        public FluidStack drain(final int amount, final FluidAction action) {
            if (stack.isEmpty() || amount <= 0) return FluidStack.EMPTY;
            final FluidTank tank = readTank();
            final FluidStack extracted = tank.drain(amount / stack.getCount(), action);
            if (!extracted.isEmpty() && action.execute()) setContents(stack, tank.getFluid());
            return new FluidStack(extracted, extracted.getAmount() * stack.getCount());
        }

        private FluidTank readTank() {
            final FluidStack fluid = contents(stack);
            final var tank = new FluidTank(Math.max(OBTankBlockEntity.capacity(), fluid.getAmount()));
            tank.setFluid(fluid);
            return tank;
        }

        private int totalAmount(final int amount) {
            return (int) Math.min(Integer.MAX_VALUE, (long) amount * stack.getCount());
        }
    }
}
