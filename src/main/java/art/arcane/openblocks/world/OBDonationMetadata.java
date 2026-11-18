package art.arcane.openblocks.world;

import art.arcane.openblocks.OpenBlocks;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = OpenBlocks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OBDonationMetadata {

    public record Entry(String name, List<String> authors, @Nullable URI donation, @Nullable URI homepage) { }

    private static final Map<String, URI> DONATION_LINKS = new HashMap<>();

    private OBDonationMetadata() { }

    @SubscribeEvent
    public static void processMessages(final InterModProcessEvent event) {
        event.getIMCStream("donateUrl"::equalsIgnoreCase).forEach(message -> {
            final Object value = message.messageSupplier().get();
            if (value instanceof String link) registerLink(message.senderModId(), link);
        });
    }

    public static void registerLink(final String modId, final String url) {
        final URI link = webLink(url);
        if (link != null) DONATION_LINKS.put(modId, link);
    }

    @Nullable
    public static Entry identify(final ItemStack stack) {
        if (stack.isEmpty()) return null;
        final var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || id.getNamespace().equals("minecraft")) return null;
        return ModList.get().getModContainerById(id.getNamespace()).map(mod -> entry(mod.getModInfo())).orElse(null);
    }

    private static Entry entry(final IModInfo mod) {
        final Object authorValue = mod.getConfig().getConfigElement("authors").orElse(null);
        final List<String> authors;
        if (authorValue instanceof String list) {
            authors = Arrays.stream(list.split(",")).map(String::strip).filter(author -> !author.isEmpty()).toList();
        } else if (authorValue instanceof List<?> list) {
            authors = list.stream().filter(String.class::isInstance).map(String.class::cast).map(String::strip)
                    .filter(author -> !author.isEmpty()).toList();
        } else authors = List.of();

        URI donation = DONATION_LINKS.get(mod.getModId());
        if (donation == null) donation = webLink(mod.getConfig().getConfigElement("donateUrl").orElse(null));
        if (donation == null) donation = webLink(mod.getConfig().getConfigElement("donationURL").orElse(null));
        final URI homepage = webLink(mod.getConfig().getConfigElement("displayURL").orElse(null));
        return new Entry(mod.getDisplayName(), authors, donation, homepage);
    }

    @Nullable
    private static URI webLink(@Nullable final Object value) {
        if (!(value instanceof String url) || url.isBlank()) return null;
        try {
            final URI uri = new URI(url.strip());
            if (uri.getHost() != null && uri.getUserInfo() == null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) return uri;
        } catch (URISyntaxException ignored) { }
        return null;
    }
}
