package dev.errnicraft.levelz_refabricated.init;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;

public class EntityInit {

    public static final boolean isRedstoneBitsLoaded = FabricLoader.getInstance().isModLoaded("redstonebits");

    public static final EntityType<LevelExperienceOrbEntity> LEVEL_EXPERIENCE_ORB = EntityType.Builder.<LevelExperienceOrbEntity>of(LevelExperienceOrbEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F).build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "level_experience_orb")));

    public static void init() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, LevelZRefabricated.identifierOf("level_experience_orb"), LEVEL_EXPERIENCE_ORB);
    }

}
