package art.arcane.openblocks.registry;

import art.arcane.openblocks.OpenBlocks;
import art.arcane.openblocks.entity.OBCartographer;
import art.arcane.openblocks.entity.OBFallingExperienceOrb;
import art.arcane.openblocks.entity.OBGlyph;
import art.arcane.openblocks.entity.OBGoldenEye;
import art.arcane.openblocks.entity.OBHangGlider;
import art.arcane.openblocks.entity.OBItemProjectile;
import art.arcane.openblocks.entity.OBLuggage;
import art.arcane.openblocks.entity.OBMagnet;
import art.arcane.openblocks.entity.OBMiniMe;
import art.arcane.openblocks.entity.OBMountedBlock;
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
    public static final RegistryObject<EntityType<OBHangGlider>> HANG_GLIDER = ENTITY_TYPES.register("hang_glider",
            () -> EntityType.Builder.of(OBHangGlider::new, MobCategory.MISC).noSave().noSummon()
                    .sized(0.0F, 0.0F).clientTrackingRange(4).updateInterval(1).setShouldReceiveVelocityUpdates(false)
                    .build(OpenBlocks.id("hang_glider").toString()));
    public static final RegistryObject<EntityType<OBMagnet>> MAGNET = registerMagnet("magnet");
    public static final RegistryObject<EntityType<OBMountedBlock>> MOUNTED_BLOCK = ENTITY_TYPES.register("mounted_block",
            () -> EntityType.Builder.of(OBMountedBlock::new, MobCategory.MISC).noSummon()
                    .sized(0.925F, 0.925F).clientTrackingRange(4).updateInterval(1).build(OpenBlocks.id("mounted_block").toString()));
    public static final RegistryObject<EntityType<OBMagnet>> PLAYER_MAGNET = registerMagnet("player_magnet");
    public static final RegistryObject<EntityType<OBCartographer>> CARTOGRAPHER = ENTITY_TYPES.register("cartographer",
            () -> EntityType.Builder.of(OBCartographer::new, MobCategory.MISC).fireImmune()
                    .sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(2).build(OpenBlocks.id("cartographer").toString()));
    public static final RegistryObject<EntityType<OBItemProjectile>> ITEM_PROJECTILE = ENTITY_TYPES.register("item_projectile",
            () -> EntityType.Builder.<OBItemProjectile>of(OBItemProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build(OpenBlocks.id("item_projectile").toString()));
    public static final RegistryObject<EntityType<OBGoldenEye>> GOLDEN_EYE = ENTITY_TYPES.register("golden_eye",
            () -> EntityType.Builder.of(OBGoldenEye::new, MobCategory.MISC).sized(0.02F, 0.02F)
                    .clientTrackingRange(4).updateInterval(1).build(OpenBlocks.id("golden_eye").toString()));
    public static final RegistryObject<EntityType<OBMiniMe>> MINI_ME = ENTITY_TYPES.register("mini_me",
            () -> EntityType.Builder.of(OBMiniMe::new, MobCategory.MISC).sized(0.6F, 0.95F)
                    .clientTrackingRange(4).updateInterval(1).setShouldReceiveVelocityUpdates(true).build(OpenBlocks.id("mini_me").toString()));
    public static final RegistryObject<EntityType<OBGlyph>> GLYPH = ENTITY_TYPES.register("glyph",
            () -> EntityType.Builder.of(OBGlyph::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).setShouldReceiveVelocityUpdates(false)
                    .build(OpenBlocks.id("glyph").toString()));

    private OBEntities() {}

    private static RegistryObject<EntityType<OBMagnet>> registerMagnet(final String id) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of(OBMagnet::new, MobCategory.MISC).noSummon().fireImmune()
                .sized(OBMagnet.SIZE, OBMagnet.SIZE).clientTrackingRange(4).updateInterval(1).build(OpenBlocks.id(id).toString()));
    }
}
