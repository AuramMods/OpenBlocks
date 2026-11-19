package art.arcane.openblocks.client;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.config.OBClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, value = Dist.CLIENT)
public final class OBFlimFlamLoreTooltip {

    private OBFlimFlamLoreTooltip() {}

    @SubscribeEvent
    public static void showHistory(final ItemTooltipEvent event) {
        final int mode = OBClientConfig.SPEC.isLoaded() ? OBClientConfig.SILLY_LORE_DISPLAY.get() : 1;
        final var display = event.getItemStack().getTagElement("display");
        if (mode == 0 || display == null || display.contains("Lore", Tag.TAG_LIST)) return;
        final var history = display.getList("SillyLore", Tag.TAG_STRING);
        if (history.isEmpty()) return;

        if (mode == 1 && !Screen.hasAltDown()) {
            event.getToolTip().add(Component.translatable("flimflam.open_blocks.hidden_lore").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
            return;
        }
        for (int i = 0; i < history.size(); i++) {
            event.getToolTip().add(Component.literal(history.getString(i)).withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        }
    }
}
