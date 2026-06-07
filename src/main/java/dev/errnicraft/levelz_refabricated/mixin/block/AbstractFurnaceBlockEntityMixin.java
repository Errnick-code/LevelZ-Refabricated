package dev.errnicraft.levelz_refabricated.mixin.block;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Unique
    @Nullable
    private ServerPlayer serverPlayerEntity = null;

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("HEAD"))
    private void dropExperienceForRecipesUsedMixin(ServerPlayer player, CallbackInfo info) {
        this.serverPlayerEntity = player;
    }

    @Inject(method = "getRecipesToAwardAndPopExperience", at = @At("RETURN"))
    private void getRecipesUsedAndDropExperienceMixin(
            ServerLevel world, Vec3 pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<List<RecipeHolder<?>>> cir
    ) {
        if (ConfigInit.CONFIG.furnaceXPMultiplier <= 0.0F) {
            return;
        }

        List<RecipeHolder<?>> recipes = cir.getReturnValue();
        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        for (RecipeHolder<?> recipeEntry : recipes) {
            if (!(recipeEntry.value() instanceof AbstractCookingRecipe cookingRecipe)) {
                continue;
            }

            float customXp = cookingRecipe.experience();
            if (customXp <= 0.0F) {
                continue;
            }

            float multiplier = ConfigInit.CONFIG.furnaceXPMultiplier;

            if (ConfigInit.CONFIG.dropXPbasedOnLvl && serverPlayerEntity != null) {
                multiplier *= 1.0F + ConfigInit.CONFIG.basedOnMultiplier
                        * ((LevelManagerAccess) serverPlayerEntity).getLevelManager().getOverallLevel();
            }

            int finalXp = Math.max(1, Math.round(customXp * multiplier));

            LevelExperienceOrbEntity.spawn(world, pos, finalXp);
        }
    }
}