package dev.errnicraft.levelz_refabricated.item;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.init.ItemInit;
import dev.errnicraft.levelz_refabricated.loot.SetVialXpFunction;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class LootInjector {

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            String path = key.identifier().getPath();

            if (path.startsWith("chests/")) {
                if (path.contains("trial_chambers") && (path.contains("_common") || path.contains("_rare") || path.contains("_unique"))) {
                    return;
                }

                float rareCandyChance = 0.10f;
                if (path.contains("ancient_city") || path.contains("end_city") || path.contains("bastion")) {
                    rareCandyChance = 0.35f;
                } else if (path.contains("stronghold") || path.contains("nether_bridge")) {
                    rareCandyChance = 0.20f;
                }
                tableBuilder.withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(rareCandyChance))
                        .add(LootItem.lootTableItem(ItemInit.RARE_CANDY)));

                addVialPool(tableBuilder,
                    ConfigInit.CONFIG.vialLootChance,
                    ConfigInit.CONFIG.vialLootEmptyChanceLoot);
            }

            if (path.equals("gameplay/fishing/treasure") || path.equals("gameplay/fishing/fish")) {
                addVialPool(tableBuilder,
                    ConfigInit.CONFIG.vialFishingChance,
                    ConfigInit.CONFIG.vialFishingEmptyChance);
            }
        });
    }

    private static void addVialPool(
            net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder,
            float filledChance,
            float emptyChance) {

        int filledMin = Math.max(1, ConfigInit.CONFIG.vialLootMinXp);
        int maxXp     = ConfigInit.CONFIG.vialLootMaxXp;
        int cap       = ConfigInit.CONFIG.vialMaxCapacity;

        float filled = Math.min(filledChance / 100f, 1.0f);
        float empty  = Math.min(emptyChance  / 100f, 1.0f);

        LevelZRefabricated.LOGGER.info(
            "[LootInjector] addVialPool: filledChance={} emptyChance={} xp={}-{} cap={}",
            filled, empty, filledMin, maxXp, cap);

        // Пул заполненной колбы
        if (filled > 0 && maxXp > 0) {
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(filled))
                .add(LootItem.lootTableItem(ItemInit.EXPERIENCE_VIAL)
                    .apply(SetVialXpFunction.builder(filledMin, maxXp, cap))));
        }

        // Пул пустой колбы
        if (empty > 0) {
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(empty))
                .add(LootItem.lootTableItem(ItemInit.EXPERIENCE_VIAL)));
        }
    }
}
