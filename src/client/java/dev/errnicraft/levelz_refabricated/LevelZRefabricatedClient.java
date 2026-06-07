package dev.errnicraft.levelz_refabricated;


import dev.errnicraft.levelz_refabricated.init.KeyInit;
import dev.errnicraft.levelz_refabricated.init.RenderInit;
import dev.errnicraft.levelz_refabricated.network.LevelClientPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class LevelZRefabricatedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyInit.init();
        LevelClientPacket.init();
        RenderInit.init();
    }
}
