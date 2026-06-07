package dev.errnicraft.levelz_refabricated.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.errnicraft.levelz_refabricated.level.restriction.PlayerRestriction;
import dev.errnicraft.levelz_refabricated.registry.EnchantmentRegistry;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.Skill;
import dev.errnicraft.levelz_refabricated.level.SkillAttribute;
import dev.errnicraft.levelz_refabricated.level.SkillBonus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public record LevelDataLoader(HolderLookup.Provider wrapperLookup) implements SimpleSynchronousResourceReloadListener {

    public static final Identifier ID = LevelZRefabricated.identifierOf("rest");
    private static final Logger LOGGER = LogManager.getLogger("LevelZ");


    private static final List<Integer> skillList = new ArrayList<>();
    private static final List<Integer> blockList = new ArrayList<>();
    private static final List<Integer> craftingList = new ArrayList<>();
    private static final List<Integer> entityList = new ArrayList<>();
    private static final List<Integer> itemList = new ArrayList<>();
    private static final List<Integer> potionList = new ArrayList<>();
    private static final List<Integer> miningList = new ArrayList<>();
    private static final List<Integer> enchantmentList = new ArrayList<>();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {


        // clear skills
        LevelManager.SKILLS.clear();
        // clear bonuses
        LevelManager.BONUSES.clear();
        skillList.clear();

        // clear restrictions
        LevelManager.BLOCK_RESTRICTIONS.clear();
        LevelManager.CRAFTING_RESTRICTIONS.clear();
        LevelManager.ENTITY_RESTRICTIONS.clear();
        LevelManager.ITEM_RESTRICTIONS.clear();
        LevelManager.POTION_RESTRICTIONS.clear();
        LevelManager.MINING_RESTRICTIONS.clear();
        LevelManager.ENCHANTMENT_RESTRICTIONS.clear();

        // safety check
        AtomicInteger skillCount = new AtomicInteger();
        List<Integer> attributeIds = new ArrayList<>();

        manager.listResources("skill", id -> id.getPath().endsWith(".json")).forEach((id, resourceRef) -> {
            try {
                if (!ConfigInit.CONFIG.defaultSkills && id.getPath().endsWith("/default.json")) {
                    return;
                }
                InputStream stream = resourceRef.open();
                JsonObject data = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

                for (String mapKey : data.keySet()) {
                    JsonObject skillJsonObject = data.getAsJsonObject(mapKey);

                    int identification = skillJsonObject.get("id").getAsInt();
                    // replace check
                    if (skillJsonObject.has("replace") && skillJsonObject.get("replace").getAsBoolean()) {
                        skillList.add(identification);
                        if (LevelManager.SKILLS.containsKey(identification)) {
                            LevelManager.SKILLS.get(identification).getAttributes().forEach(attribute -> {
                                if (attribute.getId() != -1 && attributeIds.contains(attribute.getId())) {
                                    attributeIds.remove(attribute.getId());
                                }
                            });
                            LevelManager.SKILLS.remove(identification);
                            skillCount.getAndDecrement();
                        }
                    } else if (skillList.contains(identification)) {
                        continue;
                    }

                    // skill creation
                    String key = skillJsonObject.get("key").getAsString();
                    int maxLevel = skillJsonObject.get("level").getAsInt();
                    List<SkillAttribute> attributes = new ArrayList<>();

                    for (JsonElement attributeElement : skillJsonObject.getAsJsonArray("attributes")) {
                        JsonObject attributeJsonObject = attributeElement.getAsJsonObject();

                        // FIX: В Fabric/Yarn атрибуты регистрируются как "minecraft:generic.max_health",
                        // в mojmaps — как "minecraft:max_health" (без "generic."/"player." префиксов).
                        String rawAttributeType = attributeJsonObject.get("type").getAsString();
                        String normalizedAttributeType = rawAttributeType
                                .replace("generic.", "")
                                .replace("player.", "");
                        Optional<Holder.Reference<Attribute>> entityAttribute = BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(normalizedAttributeType));
                        if (entityAttribute.isPresent()) {
                            int attributeId = -1;
                            if (attributeJsonObject.has("id")) {
                                attributeId = attributeJsonObject.get("id").getAsInt();
                            }
                            Holder<Attribute> attibute = entityAttribute.get();
                            float baseValue = -10000.0f;
                            if (attributeJsonObject.has("base")) {
                                baseValue = attributeJsonObject.get("base").getAsFloat();
                            }
                            float levelValue = attributeJsonObject.get("value").getAsFloat();
                            AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(attributeJsonObject.get("operation").getAsString().toUpperCase());
                            attributes.add(new SkillAttribute(attributeId, attibute, baseValue, levelValue, operation));
                            if (attributeId != -1) {
                                attributeIds.add(attributeId);
                            }
                        } else {
                            LOGGER.warn("Attribute {} (normalized: {}) is not a usable attribute in skill {}.", rawAttributeType, normalizedAttributeType, skillJsonObject.get("id").getAsString());
                            continue;
                        }
                    }

                    if (skillJsonObject.has("bonus")) {
                        for (JsonElement attributeElement : skillJsonObject.getAsJsonArray("bonus")) {
                            JsonObject bonusJsonObject = attributeElement.getAsJsonObject();
                            String bonusKey = bonusJsonObject.get("key").getAsString();
                            int bonusLevel = bonusJsonObject.get("level").getAsInt();

                            if (!SkillBonus.BONUS_KEYS.contains(bonusKey)) {
                                LOGGER.warn("Bonus type {} is not a valid bonus type.", bonusKey);
                                continue;
                            }

                            LevelManager.BONUSES.put(bonusKey, new SkillBonus(bonusKey, identification, bonusLevel));
                        }
                    }

                    LevelManager.SKILLS.put(identification, new Skill(identification, key, maxLevel, attributes));

                    skillCount.getAndIncrement();
                }
            } catch (Exception e) {
                LOGGER.error("Error occurred while loading resource {}. {}", id.toString(), e.toString());
            }
        });

        for (int i = 0; i < skillCount.get(); i++) {
            if (!LevelManager.SKILLS.containsKey(i)) {
                throw new MissingResourceException("Missing skill with id " + i + "! Please add a skill with this id.", this.getClass().getName(), "LevelZ");
            }
        }
        for (int i = 0; i < attributeIds.size(); i++) {
            if (!attributeIds.contains(i)) {
                throw new MissingResourceException("Missing attribute with id " + i + "! Please add an attribute with this id.", this.getClass().getName(), "LevelZ");
            }
        }
        Map<Integer, Skill> sortedMap = new TreeMap<>(LevelManager.SKILLS);
        LevelManager.SKILLS.clear();
        LevelManager.SKILLS.putAll(sortedMap);

        LOGGER.info("Loading skills...");
        LOGGER.info("Skills loaded size = {}", LevelManager.SKILLS.size());

        if (!ConfigInit.CONFIG.restrictions) {
            LOGGER.info("Restriction loading disabled by config.");
            return;
        }

        EnchantmentRegistry.updateEnchantments(this.wrapperLookup());

        manager.listResources("restriction", id -> id.getPath().endsWith(".json")).forEach((id, resourceRef) -> {
            try {
                if (!ConfigInit.CONFIG.defaultRestrictions && id.getPath().endsWith("/default.json")) {
                    LOGGER.info("Skipping default restriction file: {}", id);
                    return;
                }

                LOGGER.info("Reading restriction file: {}", id);

                try (InputStream stream = resourceRef.open()) {
                    JsonObject data = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

                    Map<String, Integer> skillKeyIdMap = new HashMap<>();
                    for (Skill skill : LevelManager.SKILLS.values()) {
                        skillKeyIdMap.put(skill.getKey(), skill.getId());
                    }

                    for (String mapKey : data.keySet()) {
                        JsonObject restrictionJsonObject = data.getAsJsonObject(mapKey);
                        Map<Integer, Integer> skillLevelRestrictions = new HashMap<>();
                        boolean replace = restrictionJsonObject.has("replace") && restrictionJsonObject.get("replace").getAsBoolean();

                        LOGGER.info("Processing restriction entry: {} | replace={}", mapKey, replace);

                        if (!restrictionJsonObject.has("skills") || !restrictionJsonObject.get("skills").isJsonObject()) {
                            LOGGER.warn("Restriction {} does not contain a valid skills object.", mapKey);
                            continue;
                        }

                        JsonObject skillRestrictions = restrictionJsonObject.getAsJsonObject("skills");
                        for (String skillKey : skillRestrictions.keySet()) {
                            if (skillKeyIdMap.containsKey(skillKey)) {
                                int skillId = skillKeyIdMap.get(skillKey);
                                int requiredLevel = skillRestrictions.get(skillKey).getAsInt();
                                skillLevelRestrictions.put(skillId, requiredLevel);

                                LOGGER.info(" - skill '{}' resolved to id={} with requiredLevel={}", skillKey, skillId, requiredLevel);
                            } else {
                                LOGGER.warn("Restriction {} contains an unrecognized skill called {}", mapKey, skillKey);
                            }
                        }

                        if (skillLevelRestrictions.isEmpty()) {
                            LOGGER.warn("Restriction {} does not contain any valid skills.", mapKey);
                            continue;
                        }

                        LOGGER.info("Restriction {} accepted with {} skill requirement(s).", mapKey, skillLevelRestrictions.size());

                        // blocks
                        if (restrictionJsonObject.has("blocks")) {
                            for (JsonElement blockElement : restrictionJsonObject.getAsJsonArray("blocks")) {
                                Identifier blockIdentifier = Identifier.parse(blockElement.getAsString());
                                if (BuiltInRegistries.BLOCK.containsKey(blockIdentifier)) {
                                    int blockRawId = BuiltInRegistries.BLOCK.getId(BuiltInRegistries.BLOCK.getValue(blockIdentifier));

                                    if (blockList.contains(blockRawId)) {
                                        continue;
                                    }
                                    if (replace) {
                                        blockList.add(blockRawId);
                                    }
                                    LevelManager.BLOCK_RESTRICTIONS.put(blockRawId, new PlayerRestriction(blockRawId, skillLevelRestrictions));

                                    LOGGER.info(" - block restriction added: {} -> rawId={}", blockIdentifier, blockRawId);
                                } else {
                                    LOGGER.warn("Restriction {} contains an unrecognized block id called {}", mapKey, blockIdentifier);
                                }
                            }
                        }

                        // crafting
                        if (restrictionJsonObject.has("crafting")) {
                            for (JsonElement craftingElement : restrictionJsonObject.getAsJsonArray("crafting")) {
                                Identifier craftingIdentifier = Identifier.parse(craftingElement.getAsString());
                                if (BuiltInRegistries.ITEM.containsKey(craftingIdentifier)) {
                                    int craftingRawId = BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.getValue(craftingIdentifier));

                                    if (craftingList.contains(craftingRawId)) {
                                        continue;
                                    }
                                    if (replace) {
                                        craftingList.add(craftingRawId);
                                    }
                                    LevelManager.CRAFTING_RESTRICTIONS.put(craftingRawId, new PlayerRestriction(craftingRawId, skillLevelRestrictions));

                                    LOGGER.info(" - crafting restriction added: {} -> rawId={}", craftingIdentifier, craftingRawId);
                                } else {
                                    LOGGER.warn("Restriction {} contains an unrecognized crafting id called {}", mapKey, craftingIdentifier);
                                }
                            }
                        }

                        // entities
                        if (restrictionJsonObject.has("entities")) {
                            for (JsonElement entityElement : restrictionJsonObject.getAsJsonArray("entities")) {
                                Identifier entityIdentifier = Identifier.parse(entityElement.getAsString());
                                if (BuiltInRegistries.ENTITY_TYPE.containsKey(entityIdentifier)) {
                                    int entityRawId = BuiltInRegistries.ENTITY_TYPE.getId(BuiltInRegistries.ENTITY_TYPE.getValue(entityIdentifier));

                                    if (entityList.contains(entityRawId)) {
                                        continue;
                                    }
                                    if (replace) {
                                        entityList.add(entityRawId);
                                    }
                                    LevelManager.ENTITY_RESTRICTIONS.put(entityRawId, new PlayerRestriction(entityRawId, skillLevelRestrictions));

                                    LOGGER.info(" - entity restriction added: {} -> rawId={}", entityIdentifier, entityRawId);
                                } else {
                                    LOGGER.warn("Restriction {} contains an unrecognized entity id called {}", mapKey, entityIdentifier);
                                }
                            }
                        }

                        // items
                        if (restrictionJsonObject.has("items")) {
                            for (JsonElement itemElement : restrictionJsonObject.getAsJsonArray("items")) {
                                Identifier itemIdentifier = Identifier.parse(itemElement.getAsString());
                                if (BuiltInRegistries.ITEM.containsKey(itemIdentifier)) {
                                    int itemRawId = BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.getValue(itemIdentifier));

                                    if (itemList.contains(itemRawId)) {
                                        continue;
                                    }
                                    if (replace) {
                                        itemList.add(itemRawId);
                                    }
                                    LevelManager.ITEM_RESTRICTIONS.put(itemRawId, new PlayerRestriction(itemRawId, skillLevelRestrictions));

                                    LOGGER.info(" - item restriction added: {} -> rawId={}", itemIdentifier, itemRawId);
                                } else {
                                    LOGGER.warn("Restriction {} contains an unrecognized item id called {}", mapKey, itemIdentifier);
                                }
                            }
                        }
                        // potions
                        if (restrictionJsonObject.has("potions")) {
                            for (JsonElement itemElement : restrictionJsonObject.getAsJsonArray("potions")) {
                                // Identifier.of agora vai funcionar porque tiramos o ponto extra do JSON
                                Identifier itemIdentifier = Identifier.parse(itemElement.getAsString());

                                if (BuiltInRegistries.POTION.containsKey(itemIdentifier)) {
                                    int itemRawId = BuiltInRegistries.POTION.getId(BuiltInRegistries.POTION.getValue(itemIdentifier));

                                    if (potionList.contains(itemRawId)) {
                                        continue;
                                    }
                                    if (replace) {
                                        potionList.add(itemRawId);
                                    }

                                    // IMPORTANTE: Use o mapa POTION_RESTRICTIONS para não misturar com itens
                                    LevelManager.POTION_RESTRICTIONS.put(itemRawId, new PlayerRestriction(itemRawId, skillLevelRestrictions));

                                    LOGGER.info(" - [SUCCESS] potion restriction added: {} -> rawId={}", itemIdentifier, itemRawId);
                                } else {
                                    // Se o log cair aqui, é porque o nome no JSON ainda está errado para o Registries.POTION
                                    LOGGER.warn("Restriction {} contains an unrecognized potion id called {}", mapKey, itemIdentifier);
                                }
                            }
                        }

                        // mining
                        if (restrictionJsonObject.has("mining")) {
                            for (JsonElement miningElement : restrictionJsonObject.getAsJsonArray("mining")) {
                                Identifier miningIdentifier = Identifier.parse(miningElement.getAsString());
                                if (BuiltInRegistries.BLOCK.containsKey(miningIdentifier)) {
                                    int miningRawId = BuiltInRegistries.BLOCK.getId(BuiltInRegistries.BLOCK.getValue(miningIdentifier));

                                    if (miningList.contains(miningRawId)) {
                                        continue;
                                    }
                                    if (replace) {
                                        miningList.add(miningRawId);
                                    }
                                    LevelManager.MINING_RESTRICTIONS.put(miningRawId, new PlayerRestriction(miningRawId, skillLevelRestrictions));

                                    LOGGER.info(" - mining restriction added: {} -> rawId={}", miningIdentifier, miningRawId);
                                } else {
                                    LOGGER.warn("Restriction {} contains an unrecognized mining id called {}", mapKey, miningIdentifier);
                                }
                            }
                        }

                        // enchantments
                        if (restrictionJsonObject.has("enchantments")) {
                            JsonObject enchantmentObject = restrictionJsonObject.getAsJsonObject("enchantments");
                            for (String enchantment : enchantmentObject.keySet()) {
                                Identifier enchantmentIdentifier = Identifier.parse(enchantment);
                                int level = enchantmentObject.get(enchantment).getAsInt();

                                if (EnchantmentRegistry.containsId(enchantmentIdentifier, level)) {
                                    int enchantmentRawId = EnchantmentRegistry.getId(enchantmentIdentifier, level);

                                    if (enchantmentList.contains(enchantmentRawId)) {
                                        continue;
                                    }
                                    if (replace) {
                                        enchantmentList.add(enchantmentRawId);
                                    }
                                    LevelManager.ENCHANTMENT_RESTRICTIONS.put(enchantmentRawId, new PlayerRestriction(enchantmentRawId, skillLevelRestrictions));

                                    LOGGER.info(" - enchantment restriction added: {} level={} -> rawId={}", enchantmentIdentifier, level, enchantmentRawId);
                                    for (Map.Entry<Integer, Integer> entry : skillLevelRestrictions.entrySet()) {
                                        Skill skill = LevelManager.SKILLS.get(entry.getKey());
                                        String skillName = skill != null ? skill.getKey() : "unknown";
                                        LOGGER.info("   * skill={} (id={}) requiredLevel={}", skillName, entry.getKey(), entry.getValue());
                                    }
                                } else {
                                    LOGGER.warn("Restriction {} contains an unrecognized enchantment id called {}", mapKey, enchantmentIdentifier);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error occurred while loading resource {}. {}", id, e);
            }
        });
    }
}


