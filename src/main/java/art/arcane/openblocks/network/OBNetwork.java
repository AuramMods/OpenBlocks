package art.arcane.openblocks.network;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBElevatorTravel;
import art.arcane.openblocks.block.entity.OBDrawingTableBlockEntity;
import art.arcane.openblocks.block.entity.OBProjectorBlockEntity;
import art.arcane.openblocks.client.OBProjectorRendering;
import art.arcane.openblocks.menu.OBDrawingTableMenu;
import art.arcane.openblocks.menu.OBPaintMixerMenu;
import art.arcane.openblocks.menu.OBRedstoneMachineMenu;
import art.arcane.openblocks.player.OBBrickActions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class OBNetwork {

    private static final String VERSION = "6";
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
        CHANNEL.messageBuilder(BooAction.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> {})
                .decoder(buf -> new BooAction())
                .consumerMainThread((message, context) -> {
                    final var sender = context.get().getSender();
                    if (sender != null) OBBrickActions.boo(sender);
                })
                .add();
        CHANNEL.messageBuilder(ProjectionRequest.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> buf.writeBlockPos(message.pos()).writeVarInt(message.map()))
                .decoder(buf -> new ProjectionRequest(buf.readBlockPos(), buf.readVarInt()))
                .consumerMainThread((message, context) -> {
                    final var sender = context.get().getSender();
                    if (sender != null && sender.level().hasChunkAt(message.pos())
                            && sender.level().getBlockEntity(message.pos()) instanceof OBProjectorBlockEntity projector) {
                        projector.sendProjection(sender, message.map());
                    }
                })
                .add();
        CHANNEL.messageBuilder(Projection.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buf) -> buf.writeResourceLocation(message.dimension()).writeBlockPos(message.pos())
                        .writeVarInt(message.map()).writeNbt(message.data()))
                .decoder(buf -> new Projection(buf.readResourceLocation(), buf.readBlockPos(), buf.readVarInt(), buf.readNbt()))
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> OBProjectorRendering.receive(message.dimension(), message.pos(), message.map(), message.data())))
                .add();
        CHANNEL.messageBuilder(MixPaint.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> buf.writeVarInt(message.containerId()).writeInt(message.rgb()))
                .decoder(buf -> new MixPaint(buf.readVarInt(), buf.readInt()))
                .consumerMainThread((message, context) -> {
                    final var sender = context.get().getSender();
                    if (sender != null && sender.containerMenu instanceof OBPaintMixerMenu menu
                            && menu.containerId == message.containerId()) menu.requestColor(sender, message.rgb());
                })
                .add();
        CHANNEL.messageBuilder(PrintGlyphs.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> buf.writeVarInt(message.containerId()).writeUtf(message.text(), OBDrawingTableBlockEntity.MAX_PRINT_LENGTH))
                .decoder(buf -> new PrintGlyphs(buf.readVarInt(), buf.readUtf(OBDrawingTableBlockEntity.MAX_PRINT_LENGTH)))
                .consumerMainThread((message, context) -> {
                    final var sender = context.get().getSender();
                    if (sender != null && sender.containerMenu instanceof OBDrawingTableMenu menu
                            && menu.containerId == message.containerId()) menu.print(sender, message.text());
                })
                .add();
    }

    public static void requestGlyphPrint(final int containerId, final String text) {
        CHANNEL.sendToServer(new PrintGlyphs(containerId, text));
    }

    public static void requestPaintMix(final int containerId, final int rgb) {
        CHANNEL.sendToServer(new MixPaint(containerId, rgb));
    }

    public static void requestElevator(final boolean descend) {
        CHANNEL.sendToServer(new ElevatorAction(descend));
    }

    public static void requestDropperSpeed(final int containerId, final int speedTenths) {
        CHANNEL.sendToServer(new DropperSpeed(containerId, speedTenths));
    }

    public static void requestBoo() {
        CHANNEL.sendToServer(new BooAction());
    }

    public static void requestProjection(final BlockPos pos, final int map) {
        CHANNEL.sendToServer(new ProjectionRequest(pos, map));
    }

    public static void sendProjection(final ServerPlayer player, final BlockPos pos, final int map, final CompoundTag data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Projection(player.level().dimension().location(), pos, map, data));
    }

    private record PrintGlyphs(int containerId, String text) {}

    private record MixPaint(int containerId, int rgb) {}

    private record ElevatorAction(boolean descend) {}

    private record DropperSpeed(int containerId, int speedTenths) {}

    private record BooAction() {}

    private record ProjectionRequest(BlockPos pos, int map) {}

    private record Projection(ResourceLocation dimension, BlockPos pos, int map, CompoundTag data) {}
}
