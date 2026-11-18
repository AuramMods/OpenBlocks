package art.arcane.openblocks.network;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBElevatorTravel;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class OBNetwork {

    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(OpenBlocks.id("main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    private OBNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ElevatorAction.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> buf.writeBoolean(message.descend()))
                .decoder(buf -> new ElevatorAction(buf.readBoolean()))
                .consumerMainThread((message, context) -> {
                    final var sender = context.get().getSender();
                    if (sender != null) OBElevatorTravel.use(sender, message.descend());
                })
                .add();
    }

    public static void requestElevator(final boolean descend) {
        CHANNEL.sendToServer(new ElevatorAction(descend));
    }

    private record ElevatorAction(boolean descend) {}
}
