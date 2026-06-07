package dev.errnicraft.levelz_refabricated.init;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.item.RareCandyItem;
import dev.errnicraft.levelz_refabricated.item.StrangePotionItem;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.errnicraft.levelz_refabricated.item.ModFoodComponent;

public class ItemInit {

    public static final Item STRANGE_POTION = registerItem("strange_potion", new StrangePotionItem(new Item.Properties().food(ModFoodComponent.STRANGE_POTION,ModFoodComponent.STRANGE_POTION_EFFECT).useCooldown(2.0f).stacksTo(16).setId(ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "strange_potion")))));

    public static final Item RARE_CANDY = registerItem("rare_candy", new RareCandyItem(new Item.Properties().food(ModFoodComponent.RARE_CANDY,ModFoodComponent.RARE_CANDY_EFFECT).useCooldown(2).stacksTo(16).setId(ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "rare_candy")))));

    public static final CreativeModeTab PLAYER_LEVEL_SKILLS = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "player_level_skills"),
            FabricItemGroup.builder().icon(() -> new ItemStack(RARE_CANDY))
                    .title(Component.translatable("itemgroup.player_level_skills"))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(STRANGE_POTION);
                        entries.accept(RARE_CANDY);
                    }).build());


    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, name), item);
    }

    public static void init() {

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            // Adiciona os itens utilitários normais
            entries.accept(STRANGE_POTION);

        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            // Adiciona os itens utilitários normais
            entries.accept(RARE_CANDY);

        });
    }

//    public static void init() {
//        FabricBrewingRecipeRegistryBuilder.BUILD.register((builder) -> {
//            builder.registerItemRecipe(Items.BLAZE_ROD, Items.COAL, Items.CACTUS);
//        });
//    }
}
