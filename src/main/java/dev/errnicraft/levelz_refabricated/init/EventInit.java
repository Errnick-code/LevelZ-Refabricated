package dev.errnicraft.levelz_refabricated.init;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.InteractionResult;
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
                float levelRetainPercentageFloat = ConfigInit.CONFIG.levelRetainPercentage / 100;
                int retainedLevel = (int) (oldLevelManager.getOverallLevel() * levelRetainPercentageFloat);

                int pointsToDistribute = retainedLevel * ConfigInit.CONFIG.pointsPerLevel + ConfigInit.CONFIG.startPoints;

                if (oldLevelManager.getSkillPoints() > 0) {
                    int retainingSkillPoints = (int) (oldLevelManager.getSkillPoints() * levelRetainPercentageFloat);
                    newLevelManager.setSkillPoints(retainingSkillPoints);
                    pointsToDistribute -= retainingSkillPoints;
                }
                for (Map.Entry<Integer, PlayerSkill> entry : oldLevelManager.getPlayerSkills().entrySet()) {
                    int retainingLevel = (int) (entry.getValue().getLevel() * levelRetainPercentageFloat);
                    newLevelManager.setSkillLevel(entry.getKey(), retainingLevel);
                    pointsToDistribute -= retainingLevel;
                    if (pointsToDistribute < 0) {
                        break;
                    }
                }
                if (pointsToDistribute > 0) {
                    for (Map.Entry<Integer, PlayerSkill> entry : oldLevelManager.getPlayerSkills().entrySet()) {
                        if (entry.getValue().getLevel() < newLevelManager.getSkillLevel(entry.getKey())) {
                            int levelDifference = newLevelManager.getSkillLevel(entry.getKey()) - entry.getValue().getLevel();
                            if (levelDifference < pointsToDistribute) {
                                newLevelManager.setSkillLevel(entry.getKey(), newLevelManager.getSkillLevel(entry.getKey() + levelDifference));
                                pointsToDistribute -= levelDifference;
                            } else {
                                newLevelManager.setSkillLevel(entry.getKey(), newLevelManager.getSkillLevel(entry.getKey() + pointsToDistribute));
                                pointsToDistribute = 0;
                                break;
                            }
                        }
                    }
                    if (pointsToDistribute > 0) {
                        newLevelManager.setSkillPoints(newLevelManager.getSkillPoints() + pointsToDistribute);
                    }
                }

                PacketHelper.updatePlayerSkills(newPlayer, null);

                newLevelManager.setOverallLevel(retainedLevel);
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
