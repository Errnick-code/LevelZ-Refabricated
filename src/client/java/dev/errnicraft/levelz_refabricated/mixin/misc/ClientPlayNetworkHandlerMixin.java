package dev.errnicraft.levelz_refabricated.mixin.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;

// Level display in the tab list is now handled via PlayerLevelSyncPacket -> LevelManager.PLAYER_LEVELS.
// No injection needed here anymore.
@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
}
