package dev.errnicraft.levelz_refabricated.item;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.access.ServerPlayerSyncAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.Skill;
import dev.errnicraft.levelz_refabricated.util.PacketHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

public class RareCandyItem extends Item {

    public RareCandyItem(Properties settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!world.isClientSide() && user instanceof ServerPlayer player) {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            int currentLevel = levelManager.getOverallLevel();
            int maxLevel = ConfigInit.CONFIG.overallMaxLevel;

            // 1. Lógica para quem ainda NÃO atingiu o nível máximo (Ganha 1 Nível)
            if (currentLevel < maxLevel) {
                // Sua lógica original de preencher a barra de XP para subir o nível
                ((ServerPlayerSyncAccess) player).addLevelExperience(
                        levelManager.getNextLevelExperience()
                                - ((int) (levelManager.getLevelProgress() * levelManager.getNextLevelExperience()))
                );
            }
            // 2. Lógica para quem JÁ está no nível máximo (Ganha 3 Pontos com Trava)
            else {
                int pointsToGive = 3;
                int currentPoints = levelManager.getSkillPoints();

                // Calcular quantos pontos faltam para maximizar TODAS as habilidades
                int neededPoints = 0;
                for (Skill skill : LevelManager.SKILLS.values()) {
                    int currentSkillLvl = levelManager.getSkillLevel(skill.getId());
                    if (currentSkillLvl < skill.getMaxLevel()) {
                        neededPoints += (skill.getMaxLevel() - currentSkillLvl);
                    }
                }

                // Verifica se o ganho de 3 pontos ultrapassa o que ele pode gastar
                if (currentPoints + pointsToGive > neededPoints) {
                    pointsToGive = Math.max(0, neededPoints - currentPoints);
                }

                if (pointsToGive > 0) {
                    levelManager.setSkillPoints(currentPoints + pointsToGive);
                    PacketHelper.updateLevels(player);

                    // Usando chave de tradução com argumento para o número de pontos
                    player.displayClientMessage(Component.translatable("text.levelz.rare_candy.bonus_points", pointsToGive).withStyle(ChatFormatting.GOLD), true);
                } else {
                    // Usando chave de tradução para quando não precisa de mais pontos
                    player.displayClientMessage(Component.translatable("text.levelz.rare_candy.maxed_skills").withStyle(ChatFormatting.RED), true);
                }
            }

            // Itens de consumo padrão
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            player.getFoodData().eat(2, 1.0f);
        }

        return stack;
    }


    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(world, user, hand);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }
}
