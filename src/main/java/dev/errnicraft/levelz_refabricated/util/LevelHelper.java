package dev.errnicraft.levelz_refabricated.util;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.Skill;
import dev.errnicraft.levelz_refabricated.level.SkillAttribute;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class LevelHelper {

    public static void updateSkill(ServerPlayer serverPlayerEntity, Skill skill) {
        LevelManager levelManager = ((LevelManagerAccess) serverPlayerEntity).getLevelManager();
        for (SkillAttribute skillAttribute : skill.getAttributes()) {
            if (serverPlayerEntity.getAttribute(skillAttribute.getAttibute()) != null) {
                if (skillAttribute.getBaseValue() > -9999.0f) {
                    serverPlayerEntity.getAttribute(skillAttribute.getAttibute()).setBaseValue(skillAttribute.getBaseValue());
                }
                Identifier identifier = LevelZRefabricated.identifierOf(skill.getKey());
                if (serverPlayerEntity.getAttribute(skillAttribute.getAttibute()).hasModifier(identifier)) {
                    serverPlayerEntity.getAttribute(skillAttribute.getAttibute()).removeModifier(identifier);
                }
                serverPlayerEntity.getAttribute(skillAttribute.getAttibute()).addTransientModifier(new AttributeModifier(identifier, skillAttribute.getLevelValue() * levelManager.getSkillLevel(skill.getId()), skillAttribute.getOperation()));
            }
        }
        levelManager.syncFlightAbility();
    }
}
