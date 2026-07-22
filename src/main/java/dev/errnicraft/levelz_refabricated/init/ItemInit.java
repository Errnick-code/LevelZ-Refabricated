package dev.errnicraft.levelz_refabricated.init;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.item.ExperienceVialItem;
import dev.errnicraft.levelz_refabricated.item.RareCandyItem;
import dev.errnicraft.levelz_refabricated.item.StrangePotionItem;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
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
import net.minecraft.world.item.Rarity;
import dev.errnicraft.levelz_refabricated.item.ModFoodComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import java.util.List;

public class ItemInit {

    public static final Item STRANGE_POTION = registerItem("strange_potion",
            new StrangePotionItem(new Item.Properties()
                    .food(ModFoodComponent.STRANGE_POTION, ModFoodComponent.STRANGE_POTION_EFFECT)
                    .useCooldown(2.0f).stacksTo(16)
                    .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "strange_potion")))));

    public static final Item RARE_CANDY = registerItem("rare_candy",
            new RareCandyItem(new Item.Properties()
                    .food(ModFoodComponent.RARE_CANDY, ModFoodComponent.RARE_CANDY_EFFECT)
                    .useCooldown(2).stacksTo(16)
                    .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "rare_candy")))));

    private static CompoundTag emptyVialTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(ExperienceVialItem.STORED_XP_KEY, 0);
        return tag;
    }
    private static final CustomData EMPTY_VIAL_DATA = CustomData.of(emptyVialTag());
    private static final CustomModelData EMPTY_VIAL_MODEL = new CustomModelData(List.of(), List.of(false), List.of(), List.of());

    public static final Item EXPERIENCE_VIAL = registerItem("experience_vial",
            new ExperienceVialItem(new Item.Properties()
                    .stacksTo(16)
                    .rarity(Rarity.UNCOMMON)
                    .component(DataComponents.CUSTOM_DATA, EMPTY_VIAL_DATA)
                    .component(DataComponents.CUSTOM_MODEL_DATA, EMPTY_VIAL_MODEL)
                    .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "experience_vial")))));

    public static final CreativeModeTab PLAYER_LEVEL_SKILLS = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "player_level_skills"),
            FabricItemGroup.builder().icon(() -> new ItemStack(RARE_CANDY))
                    .title(Component.translatable("itemgroup.player_level_skills"))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(STRANGE_POTION);
                        entries.accept(RARE_CANDY);

                        // Empty vial
                        entries.accept(new ItemStack(EXPERIENCE_VIAL));

                        // Full vial — XP from config (set at display time)
                        ItemStack fullVial = new ItemStack(EXPERIENCE_VIAL);
                        ExperienceVialItem.setStoredXp(fullVial, ConfigInit.CONFIG.vialMaxCapacity);
                        entries.accept(fullVial);

                        // Random vial — random XP between 1 and max
                        ItemStack randomVial = new ItemStack(EXPERIENCE_VIAL);
                        int randomXp = 1 + (int)(Math.random() * (ConfigInit.CONFIG.vialMaxCapacity - 1));
                        ExperienceVialItem.setStoredXp(randomVial, randomXp);
                        entries.accept(randomVial);
                    }).build());

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, name), item);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(STRANGE_POTION);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(RARE_CANDY);
        });
    }
}
