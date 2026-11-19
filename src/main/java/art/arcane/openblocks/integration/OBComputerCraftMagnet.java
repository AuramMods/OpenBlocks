package art.arcane.openblocks.integration;

import art.arcane.openblocks.OpenBlocks;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;

public final class OBComputerCraftMagnet extends AbstractTurtleUpgrade {

    public static final TurtleUpgradeSerialiser<OBComputerCraftMagnet> SERIALISER =
            TurtleUpgradeSerialiser.simpleWithCustomItem(OBComputerCraftMagnet::new);

    private OBComputerCraftMagnet(final ResourceLocation id, final ItemStack stack) {
        super(id, TurtleUpgradeType.PERIPHERAL, stack);
    }

    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener((final RegisterEvent event) -> event.register(TurtleUpgradeSerialiser.registryId(),
                OpenBlocks.id("magnet_turtle"), () -> SERIALISER));
    }

    @Override
    public IPeripheral createPeripheral(final ITurtleAccess turtle, final TurtleSide side) {
        return new OBTurtleMagnetPeripheral(turtle, side);
    }

    @Override
    public void update(final ITurtleAccess turtle, final TurtleSide side) {
        if (!turtle.getLevel().isClientSide() && turtle.getPeripheral(side) instanceof OBTurtleMagnetPeripheral peripheral) peripheral.tick();
    }
}
