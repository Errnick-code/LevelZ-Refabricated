package dev.errnicraft.levelz_refabricated.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.entity.ai.attributes.Attributes;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.init.CriteriaInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.PlayerSkill;
import dev.errnicraft.levelz_refabricated.level.Skill;
import dev.errnicraft.levelz_refabricated.network.packet.*;
import dev.errnicraft.levelz_refabricated.util.LevelHelper;
import dev.errnicraft.levelz_refabricated.util.PacketHelper;
import java.util.ArrayList;
import java.util.List;

public class LevelServerPacket {

    public static void init() {
        PayloadTypeRegistry.playS2C().register(OrbPacket.TYPE, OrbPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerLevelSyncPacket.PACKET_ID, PlayerLevelSyncPacket.PACKET_CODEC);

        PayloadTypeRegistry.playS2C().register(SkillSyncPacket.PACKET_ID, SkillSyncPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerSkillSyncPacket.PACKET_ID, PlayerSkillSyncPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(LevelPacket.PACKET_ID, LevelPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(RestrictionPacket.PACKET_ID, RestrictionPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(EnchantmentZPacket.PACKET_ID, EnchantmentZPacket.PACKET_CODEC);

        PayloadTypeRegistry.playS2C().register(StatPacket.PACKET_ID, StatPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(StatPacket.PACKET_ID, StatPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(AttributeSyncPacket.PACKET_ID, AttributeSyncPacket.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(StatPacket.PACKET_ID, (payload, context) -> {
            int id = payload.id();
            int level = payload.level();

            context.server().execute(() -> {
                LevelManager levelManager = ((LevelManagerAccess) context.player()).getLevelManager();
                if (levelManager.getSkillPoints() - level >= 0) {

                    Skill skill = LevelManager.SKILLS.get(id);
                    PlayerSkill playerSkill = levelManager.getPlayerSkills().get(id);

                    if (!ConfigInit.CONFIG.allowHigherSkillLevel && playerSkill.getLevel() >= skill.getMaxLevel()) {
                        return;
                    }
                    if (ConfigInit.CONFIG.allowHigherSkillLevel) {
                        if (playerSkill.getLevel() >= skill.getMaxLevel()) {
                            for (Skill skillCheck : LevelManager.SKILLS.values()) {
                                if (skillCheck.getMaxLevel() > levelManager.getSkillLevel(skillCheck.getId())) {
                                    return;
                                }
                            }
                        }
                    }

                    for (int i = 1; i <= level; i++) {
                        CriteriaInit.SKILL_UP.trigger(context.player(), skill.getKey(), playerSkill.getLevel() + level);
                    }

                    levelManager.setSkillLevel(id, playerSkill.getLevel() + level);
                    levelManager.setSkillPoints(levelManager.getSkillPoints() - level);

                    // Применяем атрибутные модификаторы скилла на сервере
                    LevelHelper.updateSkill(context.player(), skill);

                    // FIX: если скилл изменил MAX_HEALTH, нужно переклампить и переслать текущий HP.
                    // onAttributeUpdated() на сервере уже клампит getHealth() до нового maxHealth,
                    // но EntityData (здоровье на клиенте) обновляется только через setHealth().
                    context.player().setHealth(context.player().getHealth());

                    // Синхронизируем ВСЕ изменённые атрибуты клиенту.
                    context.player().connection.send(new ClientboundUpdateAttributesPacket(
                            context.player().getId(),
                            new ArrayList<>(context.player().getAttributes().getSyncableAttributes())
                    ));

                    PacketHelper.updateLevels(context.player());

                    ServerPlayNetworking.send(context.player(), new StatPacket(id, levelManager.getSkillLevel(id)));
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AttributeSyncPacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                context.player().connection.send(new ClientboundUpdateAttributesPacket(
                        context.player().getId(),
                        List.of(context.player().getAttribute(Attributes.ATTACK_DAMAGE))
                ));
            });
        });
    }
}
