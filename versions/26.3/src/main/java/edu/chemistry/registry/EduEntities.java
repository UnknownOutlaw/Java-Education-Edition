package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.entity.BalloonEntity;
import edu.chemistry.entity.IceBombEntity;
import edu.chemistry.entity.UnderwaterPrimedTnt;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class EduEntities {
    public static EntityType<BalloonEntity> BALLOON;
    public static EntityType<IceBombEntity> ICE_BOMB;
    public static EntityType<UnderwaterPrimedTnt> UNDERWATER_TNT;
    public static EntityType<edu.chemistry.entity.NpcEntity> NPC;
    public static EntityType<edu.chemistry.entity.TripodCameraEntity> TRIPOD_CAMERA;
    public static EntityType<edu.chemistry.agent.AgentEntity> AGENT;

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, EduMod.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void init() {
        BALLOON = register("balloon", EntityType.Builder.<BalloonEntity>of(BalloonEntity::new, MobCategory.MISC)
            .sized(0.4F, 0.4F).clientTrackingRange(10).updateInterval(2).noLootTable());
        ICE_BOMB = register("ice_bomb", EntityType.Builder.<IceBombEntity>of(IceBombEntity::new, MobCategory.MISC)
            .noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));
        UNDERWATER_TNT = register("underwater_tnt", EntityType.Builder.<UnderwaterPrimedTnt>of(UnderwaterPrimedTnt::new, MobCategory.MISC)
            .noLootTable().fireImmune().sized(0.98F, 0.98F).eyeHeight(0.15F).clientTrackingRange(10).updateInterval(10));
        NPC = register("npc", EntityType.Builder.<edu.chemistry.entity.NpcEntity>of(edu.chemistry.entity.NpcEntity::new, MobCategory.MISC)
            .noLootTable().fireImmune().sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(10).updateInterval(3));
        TRIPOD_CAMERA = register("tripod_camera", EntityType.Builder.<edu.chemistry.entity.TripodCameraEntity>of(edu.chemistry.entity.TripodCameraEntity::new, MobCategory.MISC)
            .noLootTable().fireImmune().sized(0.75F, 1.8F).eyeHeight(1.56F).clientTrackingRange(10).updateInterval(3));
        // Education's Agent: 0.6 wide and 0.93 high, never burns, sent to clients often so its steps look smooth
        AGENT = register("agent", EntityType.Builder.<edu.chemistry.agent.AgentEntity>of(edu.chemistry.agent.AgentEntity::new, MobCategory.MISC)
            .noLootTable().fireImmune().sized(0.6F, 0.93F).eyeHeight(0.75F).clientTrackingRange(10).updateInterval(1));
    }

    private EduEntities() {
    }
}
