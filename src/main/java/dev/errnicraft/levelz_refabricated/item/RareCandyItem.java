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

    /**
     * Resolves the exp amount from a pattern string.
     * Each '%' in the pattern is replaced with a random digit 0-9.
     * Example: "9%%%" → random number like 9347.
     * If pattern is empty or blank, returns the fallback value.
     */
    private static int resolveExpAmount(net.minecraft.util.RandomSource rng, String pattern, int fallback) {
        if (pattern == null || pattern.isBlank()) return fallback;
        StringBuilder sb = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '%') {
                sb.append(rng.nextInt(10));
            } else {
                sb.append(c);
            }
        }
        try {
            int result = Integer.parseInt(sb.toString());
            return Math.max(1, result);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!world.isClientSide() && user instanceof ServerPlayer player) {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();

            // Серверный кд: тратим заряд из бакета именно тут — в момент фактического
            // поедания конфеты (а не на старте анимации, которую можно прервать).
            LevelManager.RareCandyCooldownResult cooldownResult = levelManager.tryConsumeRareCandyUse();

            if (cooldownResult == LevelManager.RareCandyCooldownResult.BLOCKED) {
                // Защитный путь: use() уже не должен пускать сюда при пустом бакете,
                // но если всё же дошло — просто ничего не даём и не тратим предмет.
                long secondsLeft = levelManager.getRareCandyCooldownRemainingSeconds();
                player.displayClientMessage(Component.translatable(
                        "text.levelz.rare_candy.cooldown_active", secondsLeft
                ).withStyle(ChatFormatting.RED), true);
                return stack;
            }

            int currentLevel = levelManager.getOverallLevel();
            int maxLevel = ConfigInit.CONFIG.overallMaxLevel;

            // If exp mode is enabled: give configured exp amount and skip point/level logic
            if (ConfigInit.CONFIG.rareCandyGiveExp) {
                int expToGive = resolveExpAmount(player.level().getRandom(), ConfigInit.CONFIG.rareCandyExpPattern, ConfigInit.CONFIG.rareCandyExpAmount);
                ((ServerPlayerSyncAccess) player).addLevelExperience(expToGive);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                player.getFoodData().eat(2, 1.0f);
                notifyCooldownIfLastUse(player, levelManager, cooldownResult);
                return stack;
            }

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

            // Сообщение про кд показываем ТОЛЬКО когда заряд закончился — во всех остальных
            // случаях (DISABLED/ALLOWED) никаких упоминаний кд не выводим.
            notifyCooldownIfLastUse(player, levelManager, cooldownResult);
        }

        return stack;
    }

    /**
     * Если это было последнее использование бакета — предупреждает игрока над хотбаром,
     * через сколько секунд появится новая порция конфет. В остальных случаях ничего не пишет.
     * Сообщение про достижение лимита приоритетнее сообщений про очки/уровни (показывается
     * последним, поэтому видно именно его).
     */
    private static void notifyCooldownIfLastUse(ServerPlayer player, LevelManager levelManager, LevelManager.RareCandyCooldownResult result) {
        if (result != LevelManager.RareCandyCooldownResult.ALLOWED_LAST_USE) return;
        long secondsLeft = ConfigInit.CONFIG.rareCandyCooldownSeconds;
        player.displayClientMessage(Component.translatable(
                "text.levelz.rare_candy.cooldown_started", secondsLeft
        ).withStyle(ChatFormatting.YELLOW), true);
    }


    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (user instanceof ServerPlayer player) {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            if (!levelManager.canEatRareCandyNow()) {
                // Бакет пуст и кд активен — не начинаем анимацию поедания вообще.
                long secondsLeft = levelManager.getRareCandyCooldownRemainingSeconds();
                player.displayClientMessage(Component.translatable(
                        "text.levelz.rare_candy.cooldown_active", secondsLeft
                ).withStyle(ChatFormatting.RED), true);
                return InteractionResult.PASS;
            }
        }
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
