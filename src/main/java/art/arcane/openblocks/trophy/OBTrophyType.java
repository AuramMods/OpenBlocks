package art.arcane.openblocks.trophy;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum OBTrophyType {

    WOLF("Wolf", EntityType.WOLF),
    CHICKEN("Chicken", EntityType.CHICKEN, 10000, Items.EGG),
    COW("Cow", EntityType.COW, 20000, Items.LEATHER),
    CREEPER("Creeper", EntityType.CREEPER),
    SKELETON("Skeleton", EntityType.SKELETON),
    ZOMBIFIED_PIGLIN("PigZombie", EntityType.ZOMBIFIED_PIGLIN, 20000, Items.GOLD_NUGGET),
    BAT("Bat", EntityType.BAT, 1.0, -0.3),
    ZOMBIE("Zombie", EntityType.ZOMBIE),
    WITCH("Witch", EntityType.WITCH, 0.35, 0),
    VILLAGER("Villager", EntityType.VILLAGER),
    OCELOT("Ozelot", EntityType.OCELOT),
    SHEEP("Sheep", EntityType.SHEEP),
    BLAZE("Blaze", EntityType.BLAZE),
    SILVERFISH("Silverfish", EntityType.SILVERFISH),
    SPIDER("Spider", EntityType.SPIDER),
    CAVE_SPIDER("CaveSpider", EntityType.CAVE_SPIDER),
    SLIME("Slime", EntityType.SLIME, 0.6, 0),
    GHAST("Ghast", EntityType.GHAST, 0.1, 0.3),
    ENDERMAN("Enderman", EntityType.ENDERMAN, 0.3, 0),
    MAGMA_CUBE("LavaSlime", EntityType.MAGMA_CUBE, 0.6, 0),
    SQUID("Squid", EntityType.SQUID, 0.3, 0.5),
    MOOSHROOM("MushroomCow", EntityType.MOOSHROOM),
    IRON_GOLEM("VillagerGolem", EntityType.IRON_GOLEM, 0.3, 0),
    SNOW_GOLEM("SnowMan", EntityType.SNOW_GOLEM),
    PIG("Pig", EntityType.PIG, 20000, Items.PORKCHOP),
    ENDERMITE("Endermite", EntityType.ENDERMITE),
    GUARDIAN("Guardian", EntityType.GUARDIAN),
    RABBIT("Rabbit", EntityType.RABBIT, 20000, Items.CARROT),
    POLAR_BEAR("PolarBear", EntityType.POLAR_BEAR, 20000, Items.COD),
    SHULKER("Shulker", EntityType.SHULKER),
    HORSE("EntityHorse", EntityType.HORSE, 0.35, 0, 20000, Items.WHEAT),
    SKELETON_HORSE("SkeletonHorse", EntityType.SKELETON_HORSE, 0.35, 0, 20000, Items.BONE),
    ZOMBIE_HORSE("ZombieHorse", EntityType.ZOMBIE_HORSE, 0.35, 0, 20000, Items.ROTTEN_FLESH),
    DONKEY("Donkey", EntityType.DONKEY, 0.35, 0, 20000, Items.WHEAT),
    MULE("Mule", EntityType.MULE, 0.35, 0, 20000, Items.WHEAT),
    LLAMA("Llama", EntityType.LLAMA, 0.35, 0),
    ELDER_GUARDIAN("ElderGuardian", EntityType.ELDER_GUARDIAN, 0.2, 0.3),
    WITHER_SKELETON("WitherSkeleton", EntityType.WITHER_SKELETON, 50000, Items.WITHER_SKELETON_SKULL),
    STRAY("Stray", EntityType.STRAY),
    HUSK("Husk", EntityType.HUSK, 20000, Items.FEATHER),
    ZOMBIE_VILLAGER("ZombieVillager", EntityType.ZOMBIE_VILLAGER),
    EVOKER("EvocationIllager", EntityType.EVOKER),
    VEX("Vex", EntityType.VEX),
    VINDICATOR("VindicationIllager", EntityType.VINDICATOR, 20000, Items.IRON_AXE);

    public static final OBTrophyType DEFAULT = ZOMBIFIED_PIGLIN;
    public static final String ENTITY_ID = "entity_id";
    public static final String COOLDOWN = "cooldown";

    private static final OBTrophyType[] LEGACY_ORDER = values();
    private static final Map<EntityType<?>, OBTrophyType> BY_ENTITY = new HashMap<>();
    private static final Map<ResourceLocation, OBTrophyType> BY_ID = new HashMap<>();
    private static final Map<String, OBTrophyType> BY_LEGACY_NAME = new HashMap<>();

    static {
        for (final OBTrophyType trophy : LEGACY_ORDER) {
            BY_ENTITY.put(trophy.entityType, trophy);
            BY_ID.put(trophy.id, trophy);
            BY_LEGACY_NAME.put(trophy.legacyName.toLowerCase(Locale.ROOT), trophy);
        }
        BY_ID.put(new ResourceLocation("minecraft", "zombie_pigman"), ZOMBIFIED_PIGLIN);
        BY_ID.put(new ResourceLocation("minecraft", "villager_golem"), IRON_GOLEM);
        BY_ID.put(new ResourceLocation("minecraft", "snowman"), SNOW_GOLEM);
        BY_ID.put(new ResourceLocation("minecraft", "evocation_illager"), EVOKER);
        BY_ID.put(new ResourceLocation("minecraft", "vindication_illager"), VINDICATOR);
    }

    private final String legacyName;
    private final EntityType<?> entityType;
    private final ResourceLocation id;
    private final double scale;
    private final double verticalOffset;
    private final int rewardCooldown;
    @Nullable private final Item reward;
    @Nullable private final SoundEvent ambientSound;

    OBTrophyType(final String legacyName, final EntityType<?> entityType) {
        this(legacyName, entityType, 0.4, 0);
    }

    OBTrophyType(final String legacyName, final EntityType<?> entityType, final int rewardCooldown, final Item reward) {
        this(legacyName, entityType, 0.4, 0, rewardCooldown, reward);
    }

    OBTrophyType(final String legacyName, final EntityType<?> entityType, final double scale, final double verticalOffset) {
        this(legacyName, entityType, scale, verticalOffset, 0, null);
    }

    OBTrophyType(final String legacyName, final EntityType<?> entityType, final double scale, final double verticalOffset,
            final int rewardCooldown, @Nullable final Item reward) {
        this.legacyName = legacyName;
        this.entityType = entityType;
        this.id = EntityType.getKey(entityType);
        this.scale = scale;
        this.verticalOffset = verticalOffset;
        this.rewardCooldown = rewardCooldown;
        this.reward = reward;
        ambientSound = BuiltInRegistries.SOUND_EVENT.getOptional(new ResourceLocation("minecraft", "entity." + id.getPath() + ".ambient"))
                .orElse(null);
    }

    public EntityType<?> entityType() { return entityType; }
    public ResourceLocation id() { return id; }
    public double scale() { return scale; }
    public double verticalOffset() { return verticalOffset; }
    public int rewardCooldown() { return rewardCooldown; }
    @Nullable public Item reward() { return reward; }
    @Nullable public SoundEvent ambientSound() { return ambientSound; }

    @Nullable
    public static OBTrophyType of(final EntityType<?> entity) {
        return BY_ENTITY.get(entity);
    }

    @Nullable
    public static OBTrophyType read(@Nullable final CompoundTag tag) {
        if (tag == null) return null;
        if (tag.contains(ENTITY_ID, Tag.TAG_STRING)) return BY_ID.get(ResourceLocation.tryParse(tag.getString(ENTITY_ID)));
        if (tag.contains("entity", Tag.TAG_STRING)) return BY_LEGACY_NAME.get(tag.getString("entity").toLowerCase(Locale.ROOT));
        if (tag.contains("trophyIndex", Tag.TAG_ANY_NUMERIC)) {
            final int index = tag.getInt("trophyIndex");
            return index >= 0 && index < LEGACY_ORDER.length ? LEGACY_ORDER[index] : null;
        }
        return null;
    }

    public static OBTrophyType readOrDefault(@Nullable final CompoundTag tag) {
        final OBTrophyType type = read(tag);
        return type == null ? DEFAULT : type;
    }
}
