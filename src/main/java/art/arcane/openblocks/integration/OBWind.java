package art.arcane.openblocks.integration;

import art.arcane.openblocks.OpenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OBWind {
    private static volatile BiConsumer<ServerLevel, CompoundTag> bridge;

    private OBWind() {}

    @SubscribeEvent
    public static void enqueue(InterModEnqueueEvent event) {
        InterModComms.sendTo("auram", "wind_api_v1",
                () -> (Consumer<BiConsumer<ServerLevel, CompoundTag>>) receiver -> bridge = receiver);
    }

    public static void setFan(ServerLevel level, BlockPos position, Vec3 direction, float strength) {
        var receiver = bridge;
        if (receiver == null) return;
        CompoundTag update = request(position, "set");
        update.putDouble("x", direction.x);
        update.putDouble("y", direction.y);
        update.putDouble("z", direction.z);
        update.putFloat("magnitude", strength);
        receiver.accept(level, update);
    }

    public static void stopFan(ServerLevel level, BlockPos position) {
        var receiver = bridge;
        if (receiver != null) receiver.accept(level, request(position, "stop"));
    }

    private static CompoundTag request(BlockPos position, String operation) {
        CompoundTag event = new CompoundTag();
        event.putString("operation", operation);
        event.putString("id", "fan");
        event.putLong("pos", position.asLong());
        return event;
    }
}
