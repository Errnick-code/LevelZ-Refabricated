package dev.errnicraft.levelz_refabricated.init;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TagInit {

    public static final TagKey<Item> RESTRICTED_FURNACE_EXPERIENCE_ITEMS = TagKey.create(Registries.ITEM, LevelZRefabricated.identifierOf("restricted_furnace_experience_items"));
    public static final TagKey<Block> RESTRICTED_ORE_EXPERIENCE_BLOCKS = TagKey.create(Registries.BLOCK, LevelZRefabricated.identifierOf("restricted_ore_experience_blocks"));

    public static void init() {
    }
}
