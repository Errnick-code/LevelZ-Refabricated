package dev.errnicraft.levelz_refabricated.init;

import dev.errnicraft.levelz_refabricated.config.LevelZRefabricatedConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

public class ConfigInit {
    public static LevelZRefabricatedConfig CONFIG = new LevelZRefabricatedConfig();

    public static void init() {
        AutoConfig.register(LevelZRefabricatedConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(LevelZRefabricatedConfig.class).getConfig();
    }

}
