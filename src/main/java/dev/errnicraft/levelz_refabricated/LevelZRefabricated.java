package dev.errnicraft.levelz_refabricated;

import dev.errnicraft.levelz_refabricated.init.*;
import dev.errnicraft.levelz_refabricated.item.LootInjector;
import dev.errnicraft.levelz_refabricated.item.TradeInjector;
import dev.errnicraft.levelz_refabricated.network.LevelServerPacket;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LevelZRefabricated implements ModInitializer {

    public static final String MOD_ID = "levelz";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CommandInit.init();
        ConfigInit.init();
        CriteriaInit.init();
        EntityInit.init();
        EventInit.init();
        LoaderInit.init();
        LevelServerPacket.init();
        TagInit.init();
        ItemInit.init();
        RestrictionInit.init();
        LootInit.init();
        TradeInjector.register();
        LootInjector.register();
    }

    public static Identifier identifierOf(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }
}
