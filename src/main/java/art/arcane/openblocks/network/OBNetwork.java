package art.arcane.openblocks.network;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBElevatorTravel;
import art.arcane.openblocks.menu.OBRedstoneMachineMenu;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class OBNetwork {

    private static final String VERSION = "2";
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
        CHANNEL.messageBuilder(DropperSpeed.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> buf.writeVarInt(message.containerId()).writeVarInt(message.speedTenths()))
                .decoder(buf -> new DropperSpeed(buf.readVarInt(), buf.readVarInt()))
                .consumerMainThread((message, context) -> {
                    final var sender = context.get().getSender();
                    if (sender != null && sender.containerMenu instanceof OBRedstoneMachineMenu menu
                            && menu.containerId == message.containerId()) menu.setLaunchSpeed(sender, message.speedTenths());
                })
                .add();
    }

    public static void requestElevator(final boolean descend) {
        CHANNEL.sendToServer(new ElevatorAction(descend));
    }

    public static void requestDropperSpeed(final int containerId, final int speedTenths) {
        CHANNEL.sendToServer(new DropperSpeed(containerId, speedTenths));
    }

    private record ElevatorAction(boolean descend) {}

    private record DropperSpeed(int containerId, int speedTenths) {}
}
