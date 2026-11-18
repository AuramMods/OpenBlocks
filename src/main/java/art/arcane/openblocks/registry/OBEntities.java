package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBFallingExperienceOrb;
import art.arcane.openblocks.entity.OBItemProjectile;
import art.arcane.openblocks.entity.OBLuggage;
import art.arcane.openblocks.entity.OBPlaceholderEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OBEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OpenBlocks.MODID);

    public static final RegistryObject<EntityType<OBLuggage>> LUGGAGE = ENTITY_TYPES.register("luggage",
            () -> EntityType.Builder.of(OBLuggage::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build(OpenBlocks.id("luggage").toString()));
    public static final RegistryObject<EntityType<OBFallingExperienceOrb>> XP_ORB_NO_FLY = ENTITY_TYPES.register("xp_orb_no_fly",
            () -> EntityType.Builder.<OBFallingExperienceOrb>of(OBFallingExperienceOrb::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(5).build(OpenBlocks.id("xp_orb_no_fly").toString()));
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> HANG_GLIDER = register("hang_glider", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> MAGNET = register("magnet", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> MOUNTED_BLOCK = register("mounted_block", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> PLAYER_MAGNET = register("player_magnet", 64, 1);
    public static final RegistryObject<EntityType<OBPlaceholderEntity>> CARTOGRAPHER = register("cartographer", 64, 8);
    public static final RegistryObject<EntityType<OBItemProjectile>> ITEM_PROJECTILE = ENTITY_TYPES.register("item_projectile",
            () -> EntityType.Builder.<OBItemProjectile>of(OBItemProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build(OpenBlocks.id("item_projectile").toString()));
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
