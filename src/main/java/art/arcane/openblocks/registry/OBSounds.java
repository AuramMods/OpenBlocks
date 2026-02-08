package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, OpenBlocks.MODID);

    public static final RegistryObject<SoundEvent> ELEVATOR_ACTIVATE = register("elevator.activate");
    public static final RegistryObject<SoundEvent> GRAVE_ROB = register("grave.rob");
    public static final RegistryObject<SoundEvent> CRAYON_PLACE = register("crayon.place");
    public static final RegistryObject<SoundEvent> LUGGAGE_WALK = register("luggage.walk");
    public static final RegistryObject<SoundEvent> LUGGAGE_EAT_FOOD = register("luggage.eat.food");
    public static final RegistryObject<SoundEvent> LUGGAGE_EAT_ITEM = register("luggage.eat.item");
    public static final RegistryObject<SoundEvent> PEDOMETER_USE = register("pedometer.use");
    public static final RegistryObject<SoundEvent> SLIMALYZER_SIGNAL = register("slimalyzer.signal");
    public static final RegistryObject<SoundEvent> SQUEEGEE_USE = register("squeegee.use");
    public static final RegistryObject<SoundEvent> FEATURE_FART = register("best.feature.ever.fart");
    public static final RegistryObject<SoundEvent> MOSQUITO = register("annoying.mosquito");
    public static final RegistryObject<SoundEvent> ALARM_CLOCK = register("annoying.alarmclock");
    public static final RegistryObject<SoundEvent> VIBRATE = register("annoying.vibrate");
    public static final RegistryObject<SoundEvent> BEARTRAP_OPEN = register("beartrap.open");
    public static final RegistryObject<SoundEvent> BEARTRAP_CLOSE = register("beartrap.close");
    public static final RegistryObject<SoundEvent> CANNON_ACTIVATE = register("cannon.activate");
    public static final RegistryObject<SoundEvent> TARGET_OPEN = register("target.open");
    public static final RegistryObject<SoundEvent> TARGET_CLOSE = register("target.close");
    public static final RegistryObject<SoundEvent> BOTTLER_SIGNAL = register("bottler.signal");

    private OBSounds() {}

    private static RegistryObject<SoundEvent> register(final String id) {
        final ResourceLocation location = ResourceLocation.fromNamespaceAndPath(OpenBlocks.MODID, id);
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(location));
    }
}
