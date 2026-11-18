package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.network.OBNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT)
public final class OBElevatorControls {

    private static Input lastInput;
    private static boolean wasJumping;
    private static boolean wasSneaking;

    private OBElevatorControls() {}

    @SubscribeEvent
    public static void onMovementInput(final MovementInputUpdateEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final var player = minecraft.player;
        if (player == null || player != event.getEntity()) return;

        final Input input = event.getInput();
        final boolean jump = input.jumping && (lastInput != input || !wasJumping);
        final boolean sneak = input.shiftKeyDown && (lastInput != input || !wasSneaking);
        lastInput = input;
        wasJumping = input.jumping;
        wasSneaking = input.shiftKeyDown;
        if ((!jump && !sneak) || minecraft.screen != null || !player.onGround() || player.isPassenger()
                || player.isSpectator() || player.getAbilities().flying || player.isSleeping()) return;

        OBNetwork.requestElevator(sneak);
    }
}
