package dev.errnicraft.levelz_refabricated.init;

import dev.errnicraft.levelz_refabricated.data.LevelDataLoader;
import dev.errnicraft.levelz_refabricated.util.PacketHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoaderInit {

    public static final Logger LOGGER = LogManager.getLogger("LevelZ");

    public static void init() {
        // Регистрируем единственный лоадер — LevelDataLoader загружает и skills, и restrictions
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(LevelDataLoader.ID, LevelDataLoader::new);

        // После перезагрузки датапаков — только синхронизируем данные игрокам (не вызываем reload повторно!)
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (!success) {
                LOGGER.error("Failed to reload on {}", Thread.currentThread());
                return;
            }
            for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
                PacketHelper.updateSkills(serverPlayer);
                PacketHelper.updatePlayerSkills(serverPlayer, null);
            }
            LOGGER.info("Finished reload on {}", Thread.currentThread());
        });

        LOGGER.info("LevelDataLoader registered.");
    }
}
