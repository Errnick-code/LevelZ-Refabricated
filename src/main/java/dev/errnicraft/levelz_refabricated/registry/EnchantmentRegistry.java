package dev.errnicraft.levelz_refabricated.registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentRegistry {

    private static final Logger LOGGER = LogManager.getLogger("PlayerLevelSkills");

    public static final Map<Integer, EnchantmentZ> ENCHANTMENTS = new HashMap<>();
    public static final Map<String, Integer> INDEX_ENCHANTMENTS = new HashMap<>();

    public static boolean containsId(Holder<Enchantment> enchantment, int level) {
        return containsId(enchantment.toString(), level);
    }

    public static boolean containsId(Identifier identifier, int level) {
        return containsId(identifier.toString(), level);
    }

    public static boolean containsId(String enchantment, int level) {
        return INDEX_ENCHANTMENTS.containsKey(enchantment + level);
    }

    public static EnchantmentZ getEnchantmentZ(int key) {
        return ENCHANTMENTS.get(key);
    }

    public static int getId(Holder<Enchantment> enchantment, int level) {
        return getId(enchantment.getRegisteredName(), level);
    }

    public static int getId(Identifier identifier, int level) {
        return getId(identifier.toString(), level);
    }

    public static int getId(String enchantment, int level) {
        return getId(enchantment + level);
    }

    private static int getId(String enchantment) {
        if (INDEX_ENCHANTMENTS.containsKey(enchantment)) {
            return INDEX_ENCHANTMENTS.get(enchantment);
        }
        return -1;
    }

    public static void updateEnchantments(HolderLookup.Provider wrapperLookup) {
        ENCHANTMENTS.clear();
        INDEX_ENCHANTMENTS.clear();

        Optional<HolderLookup.RegistryLookup<Enchantment>> wrapper =
                (Optional<HolderLookup.RegistryLookup<Enchantment>>) wrapperLookup.lookup(Registries.ENCHANTMENT);

        for (HolderLookup.RegistryLookup<Enchantment> enchantmentImpl : wrapper.stream().toList()) {
            for (Holder.Reference<Enchantment> enchantment : enchantmentImpl.listElements().toList()) {
                String enchantmentId = enchantment.getRegisteredName();
                int maxLevel = enchantment.value().getMaxLevel();

                LOGGER.info("Loaded enchantment: {} | maxLevel={}", enchantmentId, maxLevel);

                for (int i = 1; i <= maxLevel; i++) {
                    int index = ENCHANTMENTS.size();
                    String key = enchantmentId + i;

                    INDEX_ENCHANTMENTS.put(key, index);
                    ENCHANTMENTS.put(index, new EnchantmentZ(enchantment, i));

                    LOGGER.info(" - registered level {} as key={} index={}", i, key, index);
                }
            }
        }

        LOGGER.info("Finished loading enchantments. Total registered: {}", ENCHANTMENTS.size());
    }
}