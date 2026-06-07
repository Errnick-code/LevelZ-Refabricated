package dev.errnicraft.levelz_refabricated.mixin.player;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.SkillBonus;
import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class HungerManagerMixin {

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V", ordinal = 1))
    private void updateStaminaMixin(ServerPlayer player, CallbackInfo ci) {
        BonusHelper.healthRegenBonus(player);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V", shift = Shift.AFTER, ordinal = 0))
    private void updateAbsorptionMixin(ServerPlayer player, CallbackInfo ci) {
        BonusHelper.healthAbsorptionBonus(player);
    }



    @ModifyVariable(method = "eat(Lnet/minecraft/world/food/FoodProperties;)V", at = @At("HEAD"), argsOnly = true)
    private FoodProperties player_level_skills$applyFoodBonus(FoodProperties food) {
        // Na 1.21.1, precisamos recuperar o player dono deste HungerManager.
        // Se você não tem o player fácil aqui, use o CURRENT_ATTACKER do seu playerTick!
        ServerPlayer player = LevelManager.CURRENT_ATTACKER.get();

        //if (player == null) return food;

        if (player != null && LevelManager.BONUSES.containsKey("foodIncreasion")) {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            SkillBonus skillBonus = LevelManager.BONUSES.get("foodIncreasion");
            int level = levelManager.getPlayerSkills().get(skillBonus.getId()).getLevel();

            if (level >= skillBonus.getLevel()) {
                float multiplier = level * ConfigInit.CONFIG.foodIncreasionBonus;

                // 2. Lógica da Saturação (Chance de bônus fixo +1.0)
                float currentSaturation = food.saturation();

                // Ex: Nível 20 * 0.025 = 0.5 (50% de chance)
                float chance = level * ConfigInit.CONFIG.foodSaturationChanceBonus;

                if (player.getRandom().nextFloat() <= chance) {
                    // Se ganhar na sorte, soma +1.0 na saturação original
                    currentSaturation += 1.0f;
                    System.out.println("[DEBUG] Bônus de Saturação Ativado!" + currentSaturation);
                }

                // Criamos um novo Record de FoodComponent com os valores multiplicados
                // Na Build 4: food.nutrition() e food.saturation()
                System.out.println("[DEBUG eat] Bônus aplicado para: " + player.getName().getString());
                return new FoodProperties((int) (food.nutrition() * multiplier), currentSaturation, food.canAlwaysEat());
            }
        }
        System.out.println("[DEBUG eat] Player info: !");
        return food;
    }

}