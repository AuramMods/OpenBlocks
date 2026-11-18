package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.block.OBElevatorBlock;
import art.arcane.openblocks.item.OBElevatorItem;
import art.arcane.openblocks.registry.OBBlocks;
import art.arcane.openblocks.registry.OBItems;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OBElevatorColors {

    private OBElevatorColors() {}

    @SubscribeEvent
    public static void registerBlocks(final RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? rgb(state.getValue(OBElevatorBlock.COLOR)) : -1,
                OBBlocks.ELEVATOR.get(), OBBlocks.ELEVATOR_ROTATING.get());
    }

    @SubscribeEvent
    public static void registerItems(final RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? rgb(OBElevatorItem.getColor(stack)) : -1,
                OBItems.ELEVATOR.get(), OBItems.ELEVATOR_ROTATING.get());
    }

    private static int rgb(final DyeColor color) {
        if (color == DyeColor.WHITE) return -1;
        final float[] rgb = color.getTextureDiffuseColors();
        return FastColor.ARGB32.color(255, Math.round(rgb[0] * 255), Math.round(rgb[1] * 255), Math.round(rgb[2] * 255));
    }
}
