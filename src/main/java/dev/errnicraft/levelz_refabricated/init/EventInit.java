package dev.errnicraft.levelz_refabricated.init;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.InteractionResult;
import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.PlayerSkill;
import dev.errnicraft.levelz_refabricated.level.Skill;
import dev.errnicraft.levelz_refabricated.mixin.entity.EntityAccessor;
import dev.errnicraft.levelz_refabricated.util.LevelHelper;
import dev.errnicraft.levelz_refabricated.util.PacketHelper;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class EventInit {

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            for (Skill skill : LevelManager.SKILLS.values()) {
                LevelHelper.updateSkill(handler.getPlayer(), skill);
            }

            handler.getPlayer().connection.send(new ClientboundUpdateAttributesPacket(
                    handler.getPlayer().getId(),
                    new ArrayList<>(handler.getPlayer().getAttributes().getSyncableAttributes())
            ));

            handler.getPlayer().setHealth(handler.getPlayer().getHealth());

            PacketHelper.updateLevels(handler.getPlayer());
            PacketHelper.syncConfig(handler.getPlayer());

            PacketHelper.syncEnchantments(handler.getPlayer());
            PacketHelper.updateSkills(handler.getPlayer());
            PacketHelper.updatePlayerSkills(handler.getPlayer(), null);
            PacketHelper.updateRestrictions(handler.getPlayer());
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            PacketHelper.updatePlayerSkills(player, null);
            PacketHelper.updateLevels(player);
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                PacketHelper.updatePlayerSkills(newPlayer, oldPlayer);
                PacketHelper.updateLevels(newPlayer);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            LevelManager newLevelManager = ((LevelManagerAccess) newPlayer).getLevelManager();

            if (ConfigInit.CONFIG.resetCurrentXp) {
                newLevelManager.setLevelProgress(0);
                newLevelManager.setTotalLevelExperience(0);
            }

            if (ConfigInit.CONFIG.levelRetainPercentage < 100) {
                LevelManager oldLevelManager = ((LevelManagerAccess) oldPlayer).getLevelManager();
                float lossMultiplier = 1.0f - (ConfigInit.CONFIG.levelRetainPercentage / 100.0f);

                // Полностью копируем скилы, поинты и уровень
                for (Map.Entry<Integer, PlayerSkill> entry : oldLevelManager.getPlayerSkills().entrySet()) {
                    newLevelManager.setSkillLevel(entry.getKey(), entry.getValue().getLevel());
                }
                newLevelManager.setSkillPoints(oldLevelManager.getSkillPoints());
                newLevelManager.setOverallLevel(oldLevelManager.getOverallLevel());
                newLevelManager.setLevelProgress(oldLevelManager.getLevelProgress());
                newLevelManager.setTotalLevelExperience(oldLevelManager.getTotalLevelExperience());

                if (ConfigInit.CONFIG.legacyDeathPenalty) {
                    // === СТАРАЯ СИСТЕМА: умножаем каждый скил и уровень на коэффициент ===
                    float retainFloat = ConfigInit.CONFIG.levelRetainPercentage / 100.0f;
                    int retainedLevel = (int) (oldLevelManager.getOverallLevel() * retainFloat);
                    int pointsToDistribute = retainedLevel * ConfigInit.CONFIG.pointsPerLevel + ConfigInit.CONFIG.startPoints;

                    if (oldLevelManager.getSkillPoints() > 0) {
                        int retainingSkillPoints = (int) (oldLevelManager.getSkillPoints() * retainFloat);
                        newLevelManager.setSkillPoints(retainingSkillPoints);
                        pointsToDistribute -= retainingSkillPoints;
                    }
                    for (Map.Entry<Integer, PlayerSkill> entry : oldLevelManager.getPlayerSkills().entrySet()) {
                        int retainingLevel = (int) (entry.getValue().getLevel() * retainFloat);
                        newLevelManager.setSkillLevel(entry.getKey(), retainingLevel);
                        pointsToDistribute -= retainingLevel;
                        if (pointsToDistribute < 0) break;
                    }
                    if (pointsToDistribute > 0) {
                        newLevelManager.setSkillPoints(newLevelManager.getSkillPoints() + pointsToDistribute);
                    }
                    newLevelManager.setOverallLevel(retainedLevel);

                } else {
                    // === НОВАЯ СИСТЕМА: снимаем % с прогресса опыта, при уходе в минус -1 уровень ===
                    float newProgress = oldLevelManager.getLevelProgress() - lossMultiplier;
                    int overallLevel = oldLevelManager.getOverallLevel();

                    if (newProgress < 0.0f && overallLevel > 0) {
                        // Теряем уровень — снимаем pointsPerLevel поинтов случайно со скилов
                        overallLevel = overallLevel - 1;
                        newProgress = 1.0f + newProgress; // остаток (newProgress был отрицательным)

                        java.util.Random rng = new java.util.Random();
                        int pointsToRemove = ConfigInit.CONFIG.pointsPerLevel;

                        // Сначала снимаем со свободных поинтов
                        int freePoints = newLevelManager.getSkillPoints();
                        if (freePoints > 0) {
                            int take = Math.min(freePoints, pointsToRemove);
                            newLevelManager.setSkillPoints(freePoints - take);
                            pointsToRemove -= take;
                        }

                        // Остаток снимаем случайно со скилов
                        int safety = pointsToRemove * 20 + 50;
                        while (pointsToRemove > 0 && safety-- > 0) {
                            java.util.List<Integer> available = new java.util.ArrayList<>();
                            for (Map.Entry<Integer, PlayerSkill> entry : newLevelManager.getPlayerSkills().entrySet()) {
                                if (entry.getValue().getLevel() > 0) available.add(entry.getKey());
                            }
                            if (available.isEmpty()) break;
                            int randomId = available.get(rng.nextInt(available.size()));
                            newLevelManager.setSkillLevel(randomId, newLevelManager.getSkillLevel(randomId) - 1);
                            pointsToRemove--;
                        }
                    }

                    newLevelManager.setLevelProgress(Math.max(0.0f, newProgress));
                    newLevelManager.setOverallLevel(Math.max(0, overallLevel));
                }

                PacketHelper.updatePlayerSkills(newPlayer, null);
                PacketHelper.updateLevels(newPlayer);
                Objects.requireNonNull(newPlayer.level().getServer()).getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, newPlayer));
            } else {
                PacketHelper.updatePlayerSkills(newPlayer, oldPlayer);
                PacketHelper.updateLevels(newPlayer);
                for (Skill skill : LevelManager.SKILLS.values()) {
                    LevelHelper.updateSkill(newPlayer, skill);
                }

                newPlayer.connection.send(new ClientboundUpdateAttributesPacket(
                        newPlayer.getId(),
                        new ArrayList<>(newPlayer.getAttributes().getSyncableAttributes())
                ));
                newPlayer.setHealth(newPlayer.getHealth());
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!player.isCreative() && !player.isSpectator()) {
                LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                if (!levelManager.hasRequiredItemLevel(player.getItemInHand(hand).getItem())) {
                    player.displayClientMessage(Component.translatable("restriction.levelz.locked.tooltip").withStyle(ChatFormatting.RED), true);
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, result) -> {
            if (!player.isCreative() && !player.isSpectator()) {
                BlockPos blockPos = result.getBlockPos();
                if (world.mayInteract(player, blockPos)) {
                    LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                    if (!levelManager.hasRequiredBlockLevel(world.getBlockState(blockPos).getBlock())) {
                        player.displayClientMessage(Component.translatable("restriction.levelz.locked.tooltip").withStyle(ChatFormatting.RED), true);
                        return InteractionResult.FAIL;
                    }
                }
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> {
            if (!player.isCreative() && !player.isSpectator()) {
                if (!entity.hasControllingPassenger() || !((EntityAccessor) entity).callCanAddPassenger(player)) {
                    LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                    if (!levelManager.hasRequiredEntityLevel(entity.getType())) {
                        player.displayClientMessage(Component.translatable("restriction.levelz.locked.tooltip").withStyle(ChatFormatting.RED), true);
                        return InteractionResult.FAIL;
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }

}
