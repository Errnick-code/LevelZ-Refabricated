package dev.errnicraft.levelz_refabricated.init;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.loot.SetVialXpFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

public class LootInit {

    public static void init() {
        SetVialXpFunction.TYPE = Registry.register(
            BuiltInRegistries.LOOT_FUNCTION_TYPE,
            LevelZRefabricated.identifierOf("set_vial_xp"),
            new LootItemFunctionType<>(SetVialXpFunction.CODEC)
        );
    }
}
