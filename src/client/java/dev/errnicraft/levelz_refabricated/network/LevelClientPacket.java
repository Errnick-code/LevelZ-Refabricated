package dev.errnicraft.levelz_refabricated.network;

import dev.errnicraft.levelz_refabricated.screen.PlayerLevelSkillsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.PlayerSkill;
import dev.errnicraft.levelz_refabricated.level.Skill;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;
import dev.errnicraft.levelz_refabricated.network.packet.*;
import net.minecraft.world.entity.Entity;
import dev.errnicraft.levelz_refabricated.registry.EnchantmentRegistry;
import dev.errnicraft.levelz_refabricated.registry.EnchantmentZ;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class LevelClientPacket {

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(SkillSyncPacket.PACKET_ID, (payload, context) -> {
            List<Integer> skillIds = payload.skillIds();
            List<String> skillKeys = payload.skillKeys();
            List<Integer> skillMaxLevels = payload.skillMaxLevels();
            List<SkillSyncPacket.SkillAttributesRecord> skillAttributes = payload.skillAttributes();
            SkillSyncPacket.SkillBonusesRecord skillBonuses = payload.skillBonuses();

            context.client().execute(() -> {
                LevelManager levelManager = ((LevelManagerAccess) context.player()).getLevelManager();

                LevelManager.SKILLS.clear();
                for (int i = 0; i < skillIds.size(); i++) {
                    Skill skill = new Skill(skillIds.get(i), skillKeys.get(i), skillMaxLevels.get(i), skillAttributes.get(i).skillAttributes());
                    LevelManager.SKILLS.put(skillIds.get(i), skill);

                    if (!levelManager.getPlayerSkills().containsKey(skillIds.get(i))) {
                        PlayerSkill playerSkill = new PlayerSkill(skillIds.get(i), 0);
                        levelManager.getPlayerSkills().put(skillIds.get(i), playerSkill);
                    }
                }
                LevelManager.BONUSES.clear();
                for (int i = 0; i < skillBonuses.skillBonuses().size(); i++) {
                    String bonusKey = skillBonuses.skillBonuses().get(i).getKey();
                    LevelManager.BONUSES.put(bonusKey, skillBonuses.skillBonuses().get(i));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PlayerSkillSyncPacket.PACKET_ID, (payload, context) -> {
            List<Integer> playerSkillIds = payload.playerSkillIds();
            List<Integer> playerSkillLevels = payload.playerSkillLevels();
            context.client().execute(() -> {
                LevelManager levelManager = ((LevelManagerAccess) context.player()).getLevelManager();
                for (int i = 0; i < playerSkillIds.size(); i++) {
                    levelManager.setSkillLevel(playerSkillIds.get(i), playerSkillLevels.get(i));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(LevelPacket.PACKET_ID, (payload, context) -> {
            int overallLevel = payload.overallLevel();
            int skillPoints = payload.skillPoints();
            int totalLevelExperience = payload.totalLevelExperience();
            float levelProgress = payload.levelProgress();
            context.client().execute(() -> {
                LevelManager levelManager = ((LevelManagerAccess) context.player()).getLevelManager();
                levelManager.setOverallLevel(overallLevel);
                levelManager.setSkillPoints(skillPoints);
                levelManager.setTotalLevelExperience(totalLevelExperience);
                levelManager.setLevelProgress(levelProgress);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RestrictionPacket.PACKET_ID, (payload, context) -> {
            RestrictionPacket.RestrictionRecord blockRestrictions = payload.blockRestrictions();
            RestrictionPacket.RestrictionRecord craftingRestrictions = payload.craftingRestrictions();
            RestrictionPacket.RestrictionRecord entityRestrictions = payload.entityRestrictions();
            RestrictionPacket.RestrictionRecord itemRestrictions = payload.itemRestrictions();
            RestrictionPacket.RestrictionRecord potionRestrictions = payload.potionRestrictions();
            RestrictionPacket.RestrictionRecord miningRestrictions = payload.miningRestrictions();
            RestrictionPacket.RestrictionRecord enchantmentRestrictions = payload.enchantmentRestrictions();

            context.client().execute(() -> {
                LevelManager.BLOCK_RESTRICTIONS.clear();
                LevelManager.CRAFTING_RESTRICTIONS.clear();
                LevelManager.ENTITY_RESTRICTIONS.clear();
                LevelManager.ITEM_RESTRICTIONS.clear();
                LevelManager.POTION_RESTRICTIONS.clear();
                LevelManager.MINING_RESTRICTIONS.clear();
                LevelManager.ENCHANTMENT_RESTRICTIONS.clear();

                for (int i = 0; i < blockRestrictions.ids().size(); i++) {
                    LevelManager.BLOCK_RESTRICTIONS.put(blockRestrictions.ids().get(i), blockRestrictions.restrictions().get(i));
                }
                for (int i = 0; i < craftingRestrictions.ids().size(); i++) {
                    LevelManager.CRAFTING_RESTRICTIONS.put(craftingRestrictions.ids().get(i), craftingRestrictions.restrictions().get(i));
                }
                for (int i = 0; i < entityRestrictions.ids().size(); i++) {
                    LevelManager.ENTITY_RESTRICTIONS.put(entityRestrictions.ids().get(i), entityRestrictions.restrictions().get(i));
                }
                for (int i = 0; i < itemRestrictions.ids().size(); i++) {
                    LevelManager.ITEM_RESTRICTIONS.put(itemRestrictions.ids().get(i), itemRestrictions.restrictions().get(i));
                }
                for (int i = 0; i < potionRestrictions.ids().size(); i++) {
                    LevelManager.POTION_RESTRICTIONS.put(potionRestrictions.ids().get(i), potionRestrictions.restrictions().get(i));
                }
                for (int i = 0; i < miningRestrictions.ids().size(); i++) {
                    LevelManager.MINING_RESTRICTIONS.put(miningRestrictions.ids().get(i), miningRestrictions.restrictions().get(i));
                }
                for (int i = 0; i < enchantmentRestrictions.ids().size(); i++) {
                    LevelManager.ENCHANTMENT_RESTRICTIONS.put(enchantmentRestrictions.ids().get(i), enchantmentRestrictions.restrictions().get(i));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(StatPacket.PACKET_ID, (payload, context) -> {
            int id = payload.id();
            int level = payload.level();
            context.client().execute(() -> {
                LevelManager levelManager = ((LevelManagerAccess) context.player()).getLevelManager();
                levelManager.setSkillLevel(id, level);
                if (context.client().screen instanceof PlayerLevelSkillsScreen playerLevelSkillsScreen) {
                    playerLevelSkillsScreen.updateLevelButtons();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PlayerLevelSyncPacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                LevelManager.PLAYER_LEVELS.clear();
                LevelManager.PLAYER_LEVELS.putAll(payload.levelMap());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OrbPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                double x = payload.x();
                double y = payload.y();
                double z = payload.z();
                Entity entity = new LevelExperienceOrbEntity(context.client().level, x, y, z, payload.experience());
                entity.syncPacketPositionCodec(x, y, z);
                entity.setYRot(0.0F);
                entity.setXRot(0.0F);
                entity.setId(payload.entityId());
                context.client().level.addEntity(entity);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(EnchantmentZPacket.PACKET_ID, (payload, context) -> {
            Map<String, Integer> indexed = payload.indexed();
            List<Integer> keys = payload.keys();
            List<String> ids = payload.ids();
            List<Integer> levels = payload.levels();
            context.client().execute(() -> {
                EnchantmentRegistry.ENCHANTMENTS.clear();
                EnchantmentRegistry.INDEX_ENCHANTMENTS.clear();

                Registry<Enchantment> registry = context.player().level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                for (int i = 0; i < keys.size(); i++) {
                    int key = keys.get(i);
                    Holder<Enchantment> entry = registry.get(Identifier.parse(ids.get(i))).get();
                    int level = levels.get(i);
                    EnchantmentRegistry.ENCHANTMENTS.put(key, new EnchantmentZ(entry, level));
                }
                EnchantmentRegistry.INDEX_ENCHANTMENTS.putAll(indexed);
            });
        });

    }
}
