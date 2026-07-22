package dev.errnicraft.levelz_refabricated.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "levelz_refabricated")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class LevelZRefabricatedConfig implements ConfigData {

    // Level settings
    @ConfigEntry.Category("level_settings")
    @ConfigEntry.Gui.RequiresRestart
    @Comment("Maximum level: 0 = disabled")
    public int overallMaxLevel = 220;
    @ConfigEntry.Category("level_settings")
    @Comment("In combination with overallMaxLevel, only when all skills maxed")
    public boolean allowHigherSkillLevel = false;
    @ConfigEntry.Category("level_settings")
    @ConfigEntry.Gui.RequiresRestart
    public int startPoints = 1;
    @ConfigEntry.Category("level_settings")
    public int pointsPerLevel = 1;

    @ConfigEntry.Category("level_settings")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    @Comment("Retain % of levels and skill points, 0 = hard mode")
    public float levelRetainPercentage = 100;
    @ConfigEntry.Category("level_settings")
    @Comment("Use legacy death penalty system (multiply each skill level by retain%). New system deducts % from XP progress and skill points on level loss.")
    public boolean legacyDeathPenalty = false;
    @ConfigEntry.Category("level_settings")
    public boolean disableMobFarms = true;
    @ConfigEntry.Category("level_settings")
    @Comment("Amount of allowed mob kills in a chunk before XP/loot stops dropping")
    public int mobKillCount = 100;
    @ConfigEntry.Category("level_settings")
    @Comment("Seconds after the last kill before the kill series is considered over and the decay timer starts. E.g. 30 means: if no kill for 30s, decay begins.")
    public int mobKillSeriesSeconds = 30;
    @ConfigEntry.Category("level_settings")
    @Comment("Seconds without a kill in a chunk before the kill counter starts recovering. Set to 0 to disable recovery.")
    public int mobKillDecaySeconds = 300;
    @ConfigEntry.Category("level_settings")
    @Comment("How many kills are recovered per decay cycle (every mobKillDecaySeconds). E.g. 10 means 10 kills are forgiven every 5 minutes.")
    public int mobKillDecayAmount = 10;
    @ConfigEntry.Category("level_settings")
    @Comment("Strange potion resets all stats instead of one")
    public boolean opStrangePotion = true;

    // Rare Candy settings
    @ConfigEntry.Category("level_settings")
    @Comment("If true, Rare Candy gives experience points instead of 1 skill point")
    public boolean rareCandyGiveExp = false;
    @ConfigEntry.Category("level_settings")
    @Comment("Amount of experience given by Rare Candy when rareCandyGiveExp is enabled")
    public int rareCandyExpAmount = 100;
    @ConfigEntry.Category("level_settings")
    @Comment("Pattern for randomized Rare Candy exp amount. Use % as a wildcard digit (0-9). Example: '9%%%' gives 9000-9999. Leave empty to use rareCandyExpAmount instead.")
    public String rareCandyExpPattern = "";

    @ConfigEntry.Category("level_settings")
    @Comment("Enable a server-side cooldown limiting how many Rare Candies a player can eat in a time window. This is per-player, not per-item-stack, and persists through relogs/restarts.")
    public boolean rareCandyCooldownEnabled = false;
    @ConfigEntry.Category("level_settings")
    @Comment("How many Rare Candies a player can eat before the cooldown kicks in. Default: 3")
    public int rareCandyCooldownMaxUses = 3;
    @ConfigEntry.Category("level_settings")
    @Comment("Cooldown duration in seconds once the use limit is reached, before a fresh batch of uses is granted. Default: 300 (5 minutes)")
    public int rareCandyCooldownSeconds = 300;

    // Experience Vial settings
    @ConfigEntry.Category("level_settings")
    @Comment("Maximum XP a single Experience Vial can hold")
    public int vialMaxCapacity = 1000;

    @ConfigEntry.Category("level_settings")
    @Comment("Amount of XP taken from player and put into vial per Shift+RMB use")
    public int vialFillAmount = 100;

    @ConfigEntry.Category("level_settings")
    @Comment("Minimum player level required to fill an Experience Vial (player XP won't go below this level)")
    public int vialMinLevelToFill = 1;

    @ConfigEntry.Category("level_settings")
    @Comment("Chance (0-100%) for FILLED Experience Vials to appear in chest loot tables")
    public float vialLootChance = 15f;

    @ConfigEntry.Category("level_settings")
    @Comment("Chance (0-100%) for EMPTY Experience Vials to appear in chest loot tables")
    public float vialLootEmptyChanceLoot = 5f;

    @ConfigEntry.Category("level_settings")
    @Comment("Chance (0-100%) for FILLED Experience Vials to appear as fishing loot")
    public float vialFishingChance = 5f;

    @ConfigEntry.Category("level_settings")
    @Comment("Chance (0-100%) for EMPTY Experience Vials to appear as fishing loot")
    public float vialFishingEmptyChance = 2f;

    @ConfigEntry.Category("level_settings")
    @Comment("Minimum random XP stored in vials found in loot (0 = empty vial possible)")
    public int vialLootMinXp = 0;

    @ConfigEntry.Category("level_settings")
    @Comment("Maximum random XP stored in vials found in loot")
    public int vialLootMaxXp = 500;

    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("level_settings")
    @Comment("restrict hand usage when item not unlocked")
    public boolean lockedHandUsage = false;

    // Skill bonuses
    @Comment("Bonus id: bowDamage")
    public float bowDamageBonus = 0.1F;

    @Comment("Bonus id: bowDoubleDamageChance")
    public float bowDoubleDamageChanceBonus = 0.025F;

    @Comment("Bonus id: crossbowDamage")
    public float crossbowDamageBonus = 0.1F;

    @Comment("Bonus id: crossbowDoubleDamageChance")
    public float crossbowDoubleDamageChanceBonus = 0.025F;

    @Comment("Bonus id: itemDamageChance")
    public float itemDamageChanceBonus = 0.025F;

    @Comment("Bonus id: potionEffectChance")
    public float potionEffectChanceBonus = 0.03F;

    @Comment("Bonus id: potionEffectChance. Default Max lv. Amplifier = 3")
    public float potionEffectAmplifier = 0.1F;

    @Comment("Bonus id: twinBreedChance")
    public float twinBreedChanceBonus = 0.2F;

    @Comment("Bonus id: fallDamageReduction")
    public float fallDamageReductionBonus = 0.2F;

    @Comment("Bonus id: deathGraceChance")
    public float deathGraceChanceBonus = 0.02F;

    @Comment("Bonus id: tntStrength")
    public float tntStrengthBonus = 1F;

    @Comment("Bonus id: priceDiscount")
    public float priceDiscountBonus = 0.02F;

    @Comment("Bonus id: tradeXp")
    public float tradeXpBonus = 0.2F;

    @Comment("Bonus id: miningDropChance")
    public float miningDropChanceBonus = 0.03F;

    @Comment("Bonus id: plantDropChance")
    public float plantDropChanceBonus = 0.01F;

    @Comment("Bonus id: anvilXpDiscount")
    public float anvilXpDiscountBonus = 0.01F;

    @Comment("Bonus id: anvilXpChance")
    public float anvilXpChanceBonus = 0.01F;

    @Comment("Bonus id: healthRegen")
    public float healthRegenBonus = 0.05F;

    @Comment("Bonus id: healthAbsorption")
    public float healthAbsorptionBonus = 0.2F;

    @Comment("Bonus id: exhaustionReduction")
    public float exhaustionReductionBonus = 0.02F;

    @Comment("Bonus id: knockbackAttackChance")
    public float meleeKnockbackAttackChanceBonus = 0.025F;

    @Comment("Bonus id: criticalAttackChance")
    public float meleeCriticalAttackChanceBonus = 0.025F;

    @Comment("Bonus id: meleeCriticalAttackDamage")
    public float meleeCriticalAttackDamageBonus = 0.2F;

    @Comment("Bonus id: meleeDoubleAttackDamageChance")
    public float meleeDoubleAttackDamageChanceBonus = 0.2F;

    @Comment("Bonus id: foodIncreasion")
    public float foodIncreasionBonus = 0.1F;

    @Comment("Bonus id: foodIncreasion")
    public float foodSaturationChanceBonus = 0.025F;

    @Comment("Bonus id: damageReflection")
    public float damageReflectionBonus = 0.025F;

    @Comment("Bonus id: damageReflectionChance")
    public float damageReflectionChanceBonus = 0.025F;

    @Comment("Bonus id: evadingDamageChance")
    public float evadingDamageChanceBonus = 0.02F;

    // Experience settings
    @ConfigEntry.Category("experience_settings")
    @Comment("XP equation: lvl^exponent * multiplicator + base")
    public float xpCostMultiplicator = 6.2188F;
    @ConfigEntry.Category("experience_settings")
    public int xpExponent = 2;
    @ConfigEntry.Category("experience_settings")
    public int xpBaseCost = 80;
    @ConfigEntry.Category("experience_settings")
    @Comment("0 = no experience cap")
    public int xpMaxCost = 0;
    @ConfigEntry.Category("experience_settings")
    public boolean resetCurrentXp = true;
    @ConfigEntry.Category("experience_settings")
    public boolean dropXPbasedOnLvl = false;
    @ConfigEntry.Category("experience_settings")
    @Comment("0.01 = 1% more xp per lvl")
    public float basedOnMultiplier = 0.01F;
    @ConfigEntry.Category("experience_settings")
    public float breedingXPMultiplier = 1.0F;
    @ConfigEntry.Category("experience_settings")
    public float bottleXPMultiplier = 1.0F;
    @ConfigEntry.Category("experience_settings")
    public float dragonXPMultiplier = 0.5F;
    @ConfigEntry.Category("experience_settings")
    public float fishingXPMultiplier = 0.8F;
    @ConfigEntry.Category("experience_settings")
    public float furnaceXPMultiplier = 0.1F;
    @ConfigEntry.Category("experience_settings")
    public float oreXPMultiplier = 1.0F;
    @ConfigEntry.Category("experience_settings")
    public float tradingXPMultiplier = 0.3F;
    @ConfigEntry.Category("experience_settings")
    public float mobXPMultiplier = 1.0F;
    @ConfigEntry.Category("experience_settings")
    public boolean spawnerMobXP = false;
    @ConfigEntry.Category("experience_settings")
    @Comment("Drop experience from player on death")
    public boolean playerDeathXpDrop = true;

    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    @Comment("Highlight locked blocks in red.")
    public boolean highlightLocked = false;
    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    public boolean inventorySkillLevel = true;
    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    public int inventorySkillLevelPosX = 0;
    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    public int inventorySkillLevelPosY = 0;
    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    @ConfigEntry.Gui.RequiresRestart
    public boolean showLevelList = true;
    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    public boolean showLevel = true;
    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    @Comment("Switch levelz screen instead of closing with inventory key")
    public boolean switchScreen = false;
    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    public boolean showLockedBlockInfo = false;
   // @ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    public int lockedBlockInfoPosX = 0;
    //@ConfigSync.ClientOnly
    @ConfigEntry.Category("gui_settings")
    public int lockedBlockInfoPosY = 0;

    @ConfigEntry.Category("progression_settings")
    @ConfigEntry.Gui.RequiresRestart
    public boolean restrictions = true;
    @ConfigEntry.Category("progression_settings")
    @ConfigEntry.Gui.RequiresRestart
    public boolean defaultRestrictions = false;
    @ConfigEntry.Category("progression_settings")
    @ConfigEntry.Gui.RequiresRestart
    @Comment("Remember to name your datapack json differently than default")
    public boolean defaultSkills = true;

    //@Override
    //public void updateConfig(ConfigData data) {
    // ConfigInit.CONFIG = (LevelZRefabricatedConfig) data;
    //}

}
