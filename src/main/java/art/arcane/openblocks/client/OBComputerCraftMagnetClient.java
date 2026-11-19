package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.integration.OBComputerCraftMagnet;
import dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import net.minecraftforge.eventbus.api.IEventBus;

public final class OBComputerCraftMagnetClient {

    private OBComputerCraftMagnetClient() {}

    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener((final RegisterTurtleModellersEvent event) -> event.register(OBComputerCraftMagnet.SERIALISER,
                TurtleUpgradeModeller.sided(OpenBlocks.id("block/magnet_turtle_left"), OpenBlocks.id("block/magnet_turtle_right"))));
    }
}
