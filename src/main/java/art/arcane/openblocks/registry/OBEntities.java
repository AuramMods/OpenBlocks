package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBPlaceholderEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OpenBlocks.MODID);

    public static final RegistryObject<EntityType<OBPlaceholderEntity>> LUGGAGE = register("luggage", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> XP_ORB_NO_FLY = register("xp_orb_no_fly", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> HANG_GLIDER = register("hang_glider", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> MAGNET = register("magnet", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> MOUNTED_BLOCK = register("mounted_block", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> PLAYER_MAGNET = register("player_magnet", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> CARTOGRAPHER = register("cartographer", 64, 8);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> ITEM_PROJECTILE = register("item_projectile", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> GOLDEN_EYE = register("golden_eye", 64, 8);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> MINI_ME = register("mini_me", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> GLYPH = register("glyph", 160, Integer.MAX_VALUE);

    private OBEntities() {}

    private static RegistryObject<EntityType<OBPlaceholderEntity>> register(final String id, final int trackingRange, final int updateInterval) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.<OBPlaceholderEntity>of(OBPlaceholderEntity::new, MobCategory.MISC)
                .sized(0.98F, 0.98F)
                .clientTrackingRange(trackingRange)
                .updateInterval(updateInterval)
                .build(OpenBlocks.MODID + ":" + id));
    }
}
