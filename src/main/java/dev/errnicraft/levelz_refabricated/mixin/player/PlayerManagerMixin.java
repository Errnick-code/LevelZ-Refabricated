package dev.errnicraft.levelz_refabricated.mixin.player;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.util.PacketHelper;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {

    @Shadow
    private net.minecraft.server.MinecraftServer server;

    @Inject(
            method = "placeNewPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;sendActivePlayerEffects(Lnet/minecraft/server/level/ServerPlayer;)V")
    )
    private void onPlayerConnectMixin(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();

        // FIX: проверяем флаг, а не количество поинтов.
        // Раньше skillPoints <= 0 срабатывало при каждом логине у игрока
        // который потратил все поинты, давая ему стартовые снова и снова.
        if (!levelManager.isStartPointsGiven() && ConfigInit.CONFIG.startPoints > 0) {
            levelManager.setSkillPoints(ConfigInit.CONFIG.startPoints);
            levelManager.setStartPointsGiven(true);
            PacketHelper.updateLevels(player);
        }

        // Broadcast updated level map to all players (including the newly joined one)
        PacketHelper.broadcastPlayerLevels(this.server);
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void onPlayerDisconnectMixin(ServerPlayer player, CallbackInfo ci) {
        // Re-broadcast so remaining players remove the disconnected player's entry
        PacketHelper.broadcastPlayerLevels(this.server);
    }
}
