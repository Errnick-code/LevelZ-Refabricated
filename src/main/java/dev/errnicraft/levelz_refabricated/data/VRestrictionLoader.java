package dev.errnicraft.levelz_refabricated.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.Skill;
import dev.errnicraft.levelz_refabricated.level.restriction.PlayerRestriction;
import dev.errnicraft.levelz_refabricated.registry.EnchantmentRegistry;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public record VRestrictionLoader(HolderLookup.Provider wrapperLookup) implements SimpleSynchronousResourceReloadListener {

    public static final Identifier ID = LevelZRefabricated.identifierOf("restriction");
    private static final Logger LOGGER = LogManager.getLogger("LevelZrestriction");

    private static final List<Integer> blockList = new ArrayList<>();
    private static final List<Integer> craftingList = new ArrayList<>();
    private static final List<Integer> entityList = new ArrayList<>();
    private static final List<Integer> itemList = new ArrayList<>();
    private static final List<Integer> miningList = new ArrayList<>();
    private static final List<Integer> enchantmentList = new ArrayList<>();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        LevelManager.BLOCK_RESTRICTIONS.clear();
        LevelManager.CRAFTING_RESTRICTIONS.clear();
        LevelManager.ENTITY_RESTRICTIONS.clear();
        LevelManager.ITEM_RESTRICTIONS.clear();
        LevelManager.MINING_RESTRICTIONS.clear();
        LevelManager.ENCHANTMENT_RESTRICTIONS.clear();
        // FIX: статические списки для защиты от дублей тоже нужно сбрасывать,
        // иначе после /reload они остаются заполненными и новые ограничения не добавляются
        blockList.clear();
        craftingList.clear();
        entityList.clear();
        itemList.clear();
        miningList.clear();
        enchantmentList.clear();

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
                LOGGER.error("Error occurred while loading resource {}. {}", id.toString(), e.toString());
            }
        });
    }
}