package dev.errnicraft.levelz_refabricated.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * RestrictionInit — больше не регистрирует лоадеры самостоятельно.
 * Вся регистрация перенесена в LoaderInit, как в оригинальном LevelZ.
 * Этот класс оставлен для совместимости на случай если где-то вызывается RestrictionInit.init()
 */
public class RestrictionInit {

    public static final Logger LOGGER = LogManager.getLogger("LevelZ");

    public static void init() {
        // Намеренно пусто: регистрация лоадеров теперь в LoaderInit.init()
        // Это исправляет баг двойной загрузки датапаков и конфликт SkillLoader / LevelDataLoader
        LOGGER.info("RestrictionInit: no-op (loaders registered in LoaderInit).");
    }
}
