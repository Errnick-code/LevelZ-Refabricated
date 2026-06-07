package dev.errnicraft.levelz_refabricated.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Holder;
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

public class SkillLoader implements SimpleSynchronousResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("LevelZ");

    private static final List<Integer> skillList = new ArrayList<>();

    @Override
    public Identifier getFabricId() {
        return LevelZRefabricated.identifierOf("skill");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {

        // clear skills
        LevelManager.SKILLS.clear();
        // clear bonuses
        LevelManager.BONUSES.clear();
        skillList.clear();

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

                        // FIX: В Fabric/Yarn атрибуты хранятся как "minecraft:generic.max_health",
                        // в mojmaps — как "minecraft:max_health" (без префиксов "generic." и "player.").
                        // Нормализуем имя: убираем эти префиксы, чтобы JSON datapacks оставались совместимы.
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
                            LOGGER.warn("Attribute {} (normalized: {}) is not a usable attribute in skill {}.", attributeJsonObject.get("type").getAsString(), normalizedAttributeType, skillJsonObject.get("id").getAsString());
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
        LOGGER.info("02Skills loaded size = {}", LevelManager.SKILLS.size());
    }
}

