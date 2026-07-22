package dev.errnicraft.levelz_refabricated.mixin.player;

import com.mojang.authlib.GameProfile;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.access.ServerPlayerSyncAccess;
import dev.errnicraft.levelz_refabricated.init.CriteriaInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.access.PlayerDropAccess;
import dev.errnicraft.levelz_refabricated.util.PacketHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.ScoreAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player implements ServerPlayerSyncAccess {

    @Unique
    private final LevelManager levelManager = ((LevelManagerAccess) this).getLevelManager();
    @Unique
    private int syncedLevelExperience = -99999999;

    public ServerPlayerEntityMixin(Level world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Override
    public void addLevelExperience(int experience) {
        if (!levelManager.isMaxLevel()) {
            ServerPlayer serverPlayerEntity = (ServerPlayer) (Object) this;
            levelManager.setLevelProgress(levelManager.getLevelProgress() + Math.max((float) experience / levelManager.getNextLevelExperience(), 0));
            levelManager.setTotalLevelExperience(Mth.clamp(levelManager.getTotalLevelExperience() + experience, 0, Integer.MAX_VALUE));

            while (levelManager.getLevelProgress() >= 1.0F && !levelManager.isMaxLevel()) {
                // Сохраняем стоимость текущего уровня ДО повышения
                int currentLevelCost = levelManager.getNextLevelExperience();
                // Переводим остаток прогресса в абсолютный XP относительно текущего уровня
                float overflowXp = (levelManager.getLevelProgress() - 1.0F) * currentLevelCost;
                // Повышаем уровень
                levelManager.addExperienceLevels(1);
                // Пересчитываем прогресс относительно стоимости НОВОГО уровня
                levelManager.setLevelProgress(overflowXp / levelManager.getNextLevelExperience());

                PacketHelper.updateLevels(serverPlayerEntity);
                CriteriaInit.LEVEL_UP.trigger(serverPlayerEntity);
                serverPlayerEntity.level().getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, serverPlayerEntity));
                serverPlayerEntity.level().getScoreboard().forAllObjectives(CriteriaInit.LEVELZ, serverPlayerEntity, ScoreAccess::increment);
                if (levelManager.getOverallLevel() > 0) {
                    serverPlayerEntity.level().playSound(null, serverPlayerEntity.getX(), serverPlayerEntity.getY(), serverPlayerEntity.getZ(), SoundEvents.PLAYER_LEVELUP, serverPlayerEntity.getSoundSource(), 1.0F, 1.0F);
                }
            }
        }
        this.syncedLevelExperience = -1;
    }

    @Inject(method = "doTick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerPlayer;totalExperience:I", ordinal = 0, shift = At.Shift.BEFORE))
    private void playerTickMixin(CallbackInfo info) {
        if (levelManager.getTotalLevelExperience() != this.syncedLevelExperience) {
            this.syncedLevelExperience = levelManager.getTotalLevelExperience();
            PacketHelper.updateLevels((ServerPlayer) (Object) this);
        }
        // FIX: тикаем восстановление лимита убийств по чанкам
        ((PlayerDropAccess) this).tickChunkKillDecay();
    }


    @Inject(method = "doTick", at = @At("HEAD"))
    private void setContext(CallbackInfo ci) {
        // Seta o player exclusivo para esta thread de processamento
        LevelManager.CURRENT_MINER.set((ServerPlayer)(Object)this);
    }

    @Inject(method = "doTick", at = @At("TAIL"))
    private void clearContext(CallbackInfo ci) {
        // LIMPA TUDO. Isso impede que o player permissivo "vaze" para o próximo cálculo.
        LevelManager.CURRENT_MINER.remove();
    }



}

