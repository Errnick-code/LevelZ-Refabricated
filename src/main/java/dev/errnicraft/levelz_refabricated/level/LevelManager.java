package dev.errnicraft.levelz_refabricated.level;

import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.restriction.PlayerRestriction;
import dev.errnicraft.levelz_refabricated.registry.EnchantmentRegistry;
import dev.errnicraft.levelz_refabricated.util.LevelHelper;
import dev.errnicraft.levelz_refabricated.util.PacketHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import java.util.ArrayList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.HashMap;
import java.util.Map;

public class LevelManager {


    public static final Map<Integer, Skill> SKILLS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> BLOCK_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> CRAFTING_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> ENTITY_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> ITEM_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> POTION_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> MINING_RESTRICTIONS = new HashMap<>();
    public static final Map<Integer, PlayerRestriction> ENCHANTMENT_RESTRICTIONS = new HashMap<>();
    public static final Map<String, SkillBonus> BONUSES = new HashMap<>();
    // Client-side: levels of all online players (UUID -> overallLevel), synced via PlayerLevelSyncPacket
    public static final Map<java.util.UUID, Integer> PLAYER_LEVELS = new HashMap<>();

    private final Player playerEntity;
    private Map<Integer, PlayerSkill> playerSkills = new HashMap<>();

    // Level
    private int overallLevel;
    private int totalLevelExperience;
    private float levelProgress;
    private int skillPoints;

    // FIX: флаг «стартовые поинты уже выдавались» — сохраняется в NBT
    private boolean startPointsGiven = false;

    public LevelManager(Player playerEntity) {
        this.playerEntity = playerEntity;

        for (Skill skill : SKILLS.values()) {
            if (!this.playerSkills.containsKey(skill.getId())) {
                this.playerSkills.put(skill.getId(), new PlayerSkill(skill.getId(), 0));
            } else if (this.playerSkills.get(skill.getId()).getLevel() > skill.getMaxLevel()) {
                this.playerSkills.get(skill.getId()).setLevel(skill.getMaxLevel());
            }
        }
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }

    public void readNbt(ValueInput view) {
        this.overallLevel = view.getIntOr("Level", 1);
        this.levelProgress = view.getFloatOr("LevelProgress", 0.0f);
        this.totalLevelExperience = view.getIntOr("TotalLevelExperience", 0);
        this.skillPoints = view.getIntOr("SkillPoints", 0);
        // FIX: читаем флаг из NBT
        this.startPointsGiven = view.getBooleanOr("StartPointsGiven", false);

        ValueInput skillsView = view.childOrEmpty("Skills");
        if (skillsView != null) {
            for (String key : skillsView.keys()) {
                ValueInput skillView = skillsView.childOrEmpty(key);
                if (skillView == null) {
                    continue;
                }

                PlayerSkill skill = PlayerSkill.readDataFromView(skillView);
                if (SKILLS.containsKey(skill.getId())) {
                    playerSkills.put(skill.getId(), skill);
                }
            }
        }
    }

    public void writeNbt(ValueOutput view) {
        view.putInt("Level", this.overallLevel);
        view.putFloat("LevelProgress", this.levelProgress);
        view.putInt("TotalLevelExperience", this.totalLevelExperience);
        view.putInt("SkillPoints", this.skillPoints);
        // FIX: пишем флаг в NBT
        view.putBoolean("StartPointsGiven", this.startPointsGiven);

        ValueOutput skillsView = view.child("Skills");
        for (Map.Entry<Integer, PlayerSkill> entry : playerSkills.entrySet()) {
            ValueOutput skillView = skillsView.child("Skill" + entry.getKey());
            entry.getValue().writeDataToNbt(skillView);
        }
    }


    public Map<Integer, PlayerSkill> getPlayerSkills() {
        return playerSkills;
    }

    public void setPlayerSkills(Map<Integer, PlayerSkill> playerSkills) {
        this.playerSkills = playerSkills;
    }

    public void setOverallLevel(int overallLevel) {
        this.overallLevel = overallLevel;
    }

    public int getOverallLevel() {
        return overallLevel;
    }

    public void setTotalLevelExperience(int totalLevelExperience) {
        this.totalLevelExperience = totalLevelExperience;
    }

    public int getTotalLevelExperience() {
        return totalLevelExperience;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setLevelProgress(float levelProgress) {
        this.levelProgress = levelProgress;
    }

    public float getLevelProgress() {
        return levelProgress;
    }

    // FIX: геттер/сеттер для флага стартовых поинтов
    public boolean isStartPointsGiven() {
        return startPointsGiven;
    }

    public void setStartPointsGiven(boolean startPointsGiven) {
        this.startPointsGiven = startPointsGiven;
    }

    public void setSkillLevel(int skillId, int level) {
        this.playerSkills.get(skillId).setLevel(level);
    }

    // Na sua classe de utilitários ou LevelManager
    public static final ThreadLocal<ServerPlayer> CURRENT_ATTACKER = new ThreadLocal<>();

    // Na sua classe de utilitários ou LevelManager
    public static final ThreadLocal<ServerPlayer> CURRENT_MINER = new ThreadLocal<>();


    public int getSkillLevel(int skillId) {
        // Maybe add a containsKey check here
        return this.playerSkills.get(skillId).getLevel();
    }

    public void addExperienceLevels(int levels) {
        this.overallLevel += levels;
        this.skillPoints += ConfigInit.CONFIG.pointsPerLevel;
        if (this.overallLevel < 0) {
            this.overallLevel = 0;
            this.levelProgress = 0.0F;
            this.totalLevelExperience = 0;
        }
    }

    public boolean isMaxLevel() {
        if (ConfigInit.CONFIG.overallMaxLevel > 0) {
            return this.overallLevel >= ConfigInit.CONFIG.overallMaxLevel;
        } else {
            int maxLevel = 0;
            for (Skill skill : SKILLS.values()) {
                maxLevel += skill.getMaxLevel();
            }
            return this.overallLevel >= maxLevel;
        }
    }

    public boolean hasAvailableLevel() {
        return this.skillPoints > 0;
    }


    public int getNextLevelExperience() {
        if (isMaxLevel()) {
            return 0;
        }
        int experienceCost = (int) (ConfigInit.CONFIG.xpBaseCost + ConfigInit.CONFIG.xpCostMultiplicator * Math.pow(this.overallLevel, ConfigInit.CONFIG.xpExponent));
        if (ConfigInit.CONFIG.xpMaxCost != 0) {
            return experienceCost >= ConfigInit.CONFIG.xpMaxCost ? ConfigInit.CONFIG.xpMaxCost : experienceCost;
        } else {
            return experienceCost;
        }
    }
    // block
    public boolean hasRequiredBlockLevel(Block block) {
        int itemId = BuiltInRegistries.BLOCK.getId(block);
        if (BLOCK_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = BLOCK_RESTRICTIONS.get(itemId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredBlockLevel(Block block) {
        int itemId = BuiltInRegistries.BLOCK.getId(block);
        if (BLOCK_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = BLOCK_RESTRICTIONS.get(itemId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }
    // crafting
    public boolean hasRequiredCraftingLevel(Item item) {
        int itemId = BuiltInRegistries.ITEM.getId(item);
        if (CRAFTING_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = CRAFTING_RESTRICTIONS.get(itemId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredCraftingLevel(Item item) {
        int itemId = BuiltInRegistries.ITEM.getId(item);
        if (CRAFTING_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = CRAFTING_RESTRICTIONS.get(itemId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    // entity
    public boolean hasRequiredEntityLevel(EntityType<?> entityType) {
        int entityId = BuiltInRegistries.ENTITY_TYPE.getId(entityType);
        if (ENTITY_RESTRICTIONS.containsKey(entityId)) {
            PlayerRestriction playerRestriction = ENTITY_RESTRICTIONS.get(entityId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredEntityLevel(EntityType<?> entityType) {
        int entityId = BuiltInRegistries.ENTITY_TYPE.getId(entityType);
        if (ENTITY_RESTRICTIONS.containsKey(entityId)) {
            PlayerRestriction playerRestriction = ENTITY_RESTRICTIONS.get(entityId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    // item
    public boolean hasRequiredItemLevel(Item item) {
        int itemId = BuiltInRegistries.ITEM.getId(item);
        if (ITEM_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = ITEM_RESTRICTIONS.get(itemId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }
    public boolean hasRequiredPotionLevel(ItemStack stack) {
        var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents != null && contents.potion().isPresent()) {
            int potionRawId = net.minecraft.core.registries.BuiltInRegistries.POTION.getId(contents.potion().get().value());

            if (POTION_RESTRICTIONS.containsKey(potionRawId)) {
                PlayerRestriction res = POTION_RESTRICTIONS.get(potionRawId);
                for (var entry : res.getSkillLevelRestrictions().entrySet()) {
                    if (this.getSkillLevel(entry.getKey()) < entry.getValue()) return false;
                }
            }
        }
        return true;
    }


    public Map<Integer, Integer> getRequiredItemLevel(Item item) {
        int itemId = BuiltInRegistries.ITEM.getId(item);
        if (ITEM_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = ITEM_RESTRICTIONS.get(itemId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    // mining
    public boolean hasRequiredMiningLevel(Block block) {
        int itemId = BuiltInRegistries.BLOCK.getId(block);
        if (MINING_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = MINING_RESTRICTIONS.get(itemId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredMiningLevel(Block block) {
        int itemId = BuiltInRegistries.BLOCK.getId(block);
        if (MINING_RESTRICTIONS.containsKey(itemId)) {
            PlayerRestriction playerRestriction = MINING_RESTRICTIONS.get(itemId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    // enchantment
    public boolean hasRequiredEnchantmentLevel(Holder<Enchantment> enchantment, int level) {
        int enchantmentId = EnchantmentRegistry.getId(enchantment, level);
        if (ENCHANTMENT_RESTRICTIONS.containsKey(enchantmentId)) {
            PlayerRestriction playerRestriction = ENCHANTMENT_RESTRICTIONS.get(enchantmentId);
            for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                if (this.getSkillLevel(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, Integer> getRequiredEnchantmentLevel(Holder<Enchantment> enchantment, int level) {
        int enchantmentId = EnchantmentRegistry.getId(enchantment, level);
        if (ENCHANTMENT_RESTRICTIONS.containsKey(enchantmentId)) {
            PlayerRestriction playerRestriction = ENCHANTMENT_RESTRICTIONS.get(enchantmentId);
            return playerRestriction.getSkillLevelRestrictions();
        }
        return Map.of(0, 0);
    }

    public boolean resetSkill(int skillId) {
        int level = this.getSkillLevel(skillId);
        if (level > 0) {
            this.setSkillPoints(this.getSkillPoints() + level);
            this.setSkillLevel(skillId, 0);
            PacketHelper.updatePlayerSkills((ServerPlayer) this.playerEntity, null);
            LevelHelper.updateSkill((ServerPlayer) this.playerEntity, SKILLS.get(skillId));
            // FIX: синхронизировать HP и атрибуты клиенту после сброса скилла
            ((ServerPlayer) this.playerEntity).setHealth(((ServerPlayer) this.playerEntity).getHealth());
            ((ServerPlayer) this.playerEntity).connection.send(new ClientboundUpdateAttributesPacket(
                    ((ServerPlayer) this.playerEntity).getId(),
                    new ArrayList<>(((ServerPlayer) this.playerEntity).getAttributes().getSyncableAttributes())
            ));
            PacketHelper.updateLevels((ServerPlayer) this.playerEntity);
            this.syncFlightAbility();
            return true;
        } else {
            return false;
        }
    }

    public void syncFlightAbility() {
        if (this.playerEntity instanceof ServerPlayer player) {
            if (player.isCreative() || player.isSpectator()) return;

            boolean hasMaestria = this.hasAllSkillsMaxed();
            boolean isAllowFlying = player.getAbilities().mayfly;

            if (hasMaestria && !isAllowFlying) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
                player.displayClientMessage(Component.translatable("skill.mastery.flight_enabled").withStyle(ChatFormatting.GOLD), true);
            } else if (!hasMaestria && isAllowFlying) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.displayClientMessage(Component.translatable("skill.mastery.flight_disabled").withStyle(ChatFormatting.RED), true);
            }
        }
    }

    public boolean hasAllSkillsMaxed() {
        for (Skill skill : SKILLS.values()) {
            int playerLevel = this.getSkillLevel(skill.getId());
            if (playerLevel < skill.getMaxLevel()) {
                return false;
            }
        }
        return true;
    }


}
